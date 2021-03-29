package com.fsck.k9.pEp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;

import foundation.pEp.jniadapter.Blob;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Message;
import foundation.pEp.jniadapter.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

/**
 * Makes a pEp message from a k9 message
 */

class PEpMessageBuilder {
    private static final String DEFAULT_FILENAME = "noname";
    private MimeMessage mm;

    PEpMessageBuilder(MimeMessage m) {
        mm = m;
    }

    Message createMessage(Context context) {
        Message pEpMsg = null;
        try {
            pEpMsg = new Message();

            addHeaders(pEpMsg, context);
            addBody(pEpMsg);
            return pEpMsg;
        } catch (MessagingException | IOException e) {
            pEpMsg.close();
            Log.e("pEp", "Could not create pep message:", e);
        }
        return pEpMsg;
    }

    private void addBody(Message pEpMsg) throws MessagingException, IOException {
        // fiddle message txt from MimeMsg...
        // the buildup of mm should be like the follwing:
        // - html body if any, else plain text
        // - plain text body if html above
        // - many attachments (of type binarymemoryblob (hopefully ;-)).
        Body b = mm.getBody();
        Vector<Blob> attachments = new Vector<>();

        if (!(b instanceof MimeMultipart)) { //FIXME: Don't do this assumption (if not Multipart then plain or html text)

            String disposition = MimeUtility.unfoldAndDecode(mm.getDisposition());
            byte[] bodyContent = PEpUtils.extractBodyContent(b);
            if ((isAnAttachment(mm))) {
                Log.i("PEpMessageBuilder", "addBody 1 " + disposition);
                String filename = MimeUtility.getHeaderParameter(disposition, "filename");
                addAttachment(attachments, mm.getContentType(), filename, bodyContent);
                pEpMsg.setLongmsg("");
            }

            String charset = getMessagePartCharset(mm);
            String text = new String(bodyContent, charset);
            if (mm.isMimeType("text/html")) {
                pEpMsg.setLongmsgFormatted(text);
            } else {
                pEpMsg.setLongmsg(text);
            }
            return;
        }

        MimeMultipart mmp = (MimeMultipart) b;
        handleMultipart(pEpMsg, mmp, attachments);           // recurse into the Joys of Mime...

        pEpMsg.setAttachments(attachments);
    }

    private String getMessagePartCharset(Part part) {
        String charset =  MimeUtility.getHeaderParameter(part.getContentType(), "charset");

        if (charset == null) {
            charset = MimeUtility.getHeaderParameter(mm.getContentType(), "charset");
        }

        if (charset == null || !Charset.isSupported(charset)) {
            // failback when the header doesn't have charset parameter or it is invalid, defaults to UTF-8
            // FIXME: charset, treat non text body types like application/pgp-keys
            charset = Charset.defaultCharset().name();
        }
        return charset;
    }

    private void handleMultipart(Message pEpMsg, MimeMultipart mmp, Vector<Blob> attachments) throws MessagingException, IOException, UnsupportedEncodingException {
        int nrOfParts = mmp.getBodyParts().size();
        for (int part = 0; part < nrOfParts; part++) {
            MimeBodyPart mbp = (MimeBodyPart) mmp.getBodyPart(part);
            Body mbp_body = mbp.getBody();
            if (mbp_body == null) {
                // eh? this can happen?!
                Log.e("pep", "mbp_body==null!");
                continue;
            }
            if (mbp_body instanceof MimeMultipart) {
                handleMultipart(pEpMsg, (MimeMultipart) mbp_body, attachments);
                continue;
            }
            //FIXME> Deal with non text and non multipart message and non attachments

            boolean plain = mbp.isMimeType("text/plain");
            if (!isAnAttachment(mbp) && (plain || mbp.isMimeType("text/html"))) {
                String charset = getMessagePartCharset(mbp);
                String text = new String(PEpUtils.extractBodyContent(mbp_body), charset);

                if (plain) {
                    String longmsg = pEpMsg.getLongmsg();
                    if (longmsg != null) {
                        pEpMsg.setLongmsg(longmsg + text);
                    } else {
                        pEpMsg.setLongmsg(text);
                    }
                } else {
                    String longmsg = pEpMsg.getLongmsgFormatted();
                    if (longmsg != null) {
                        pEpMsg.setLongmsgFormatted(longmsg + text);
                    } else {
                        pEpMsg.setLongmsgFormatted(text);
                    }
                }
            } else  {
                addAttachment(attachments, mbp);
            }
        }
    }

    private void addAttachment(Vector<Blob> attachments, String mimeType, String filename, byte[] data) {
        Blob blob = new Blob();
        blob.filename = filename;
        blob.mime_type = mimeType;
        blob.data = data;
        attachments.add(blob);
    }

    private void addAttachment(Vector<Blob> attachments, MimeBodyPart attachment) throws IOException, MessagingException {
        Blob attachmentBlob = new Blob();
        attachmentBlob.mime_type = attachment.getMimeType();
        attachmentBlob.data = PEpUtils.extractBodyContent(attachment.getBody());
        attachmentBlob.filename = getFilenameUri(attachment);

//        Log.d("pep", "PePMessageBuilder: BLOB #" + attachments.size() + ":" + mimeType + ":" + filename);
        attachments.add(attachmentBlob);

    }

    private String getFilenameUri(MimeBodyPart attachment) throws MessagingException {
        if (getDisposition(attachment).equals(Disposition.INLINE)) {
            return MimeHeader.CID_SCHEME + attachment.getContentId();
        } else {
            return MimeHeader.FILE_SCHEME + getFileName(attachment);
        }
    }

    /*********************************************************************************************
    * From RFC 2183 - Content-Disposition definition
    *
    * Status of this Memo
    *
    *    This document specifies an Internet standards track protocol for the
    *    Internet community, and requests discussion and suggestions for
    *    improvements.  Please refer to the current edition of the "Internet
    *    Official Protocol Standards" (STD 1) for the standardization state
    *    and status of this protocol.  Distribution of this memo is unlimited.
    *
    * Abstract
    *
    *    This memo provides a mechanism whereby messages conforming to the
    *    MIME specifications [RFC 2045, RFC 2046, RFC 2047, RFC 2048, RFC
    *    2049] can convey presentational information.  It specifies the
    *    "Content-Disposition" header field, which is optional and valid for
    *    any MIME entity ("message" or "body part").  Two values for this
    *    header field are described in this memo; one for the ordinary linear
    *    presentation of the body part, and another to facilitate the use of
    *    mail to transfer files.  It is expected that more values will be
    *    defined in the future, and procedures are defined for extending this
    *     set of values.
    *
    * disposition := "Content-Disposition" ":"
    *                disposition-type
    *                *(";" disposition-parm)
    *
    * disposition-type := "inline"
    *                   / "attachment"
    *                   / extension-token
    *                   ; values are not case-sensitive
    *
    * FROM RFC 2045 - Mime definition: extension-token definition
    *
    * extension-token := ietf-token / x-token
    *
    * ietf-token := <An extension token defined by a
    *                standards-track RFC and registered
    *                with IANA.>
    *
    * x-token := <The two characters "X-" or "x-" followed, with
    *             no intervening white space, by any token>
    *
    * ********************************************************************************************
    *    Disposition and getDisposition(MimeBodyPart) to follow the behaviour described above    *
    * ********************************************************************************************/
    private enum Disposition {
        UNKNOWN,
        ATTACHMENT,
        INLINE,
    }

    private Disposition getDisposition(final MimeBodyPart attachment) {
        final boolean isDispositionDefined = attachment.getDisposition() != null;
        final String disposition = attachment.getDisposition();
        final String dispositionType = isDispositionDefined ? disposition.split(";")[0] : "";
        final boolean contentIdIsEmpty = TextUtils.isEmpty(attachment.getContentId());

        if (contentIdIsEmpty || ("attachment".equalsIgnoreCase(dispositionType))) {
            return Disposition.ATTACHMENT;
        } else if (!contentIdIsEmpty
                && (!isDispositionDefined || "inline".equalsIgnoreCase(dispositionType))) {
            return Disposition.INLINE;
        } else {
            return Disposition.UNKNOWN;
        }
    }

    private void addHeaders(Message m, Context context) {
        // headers
        if (mm.getFrom()[0].getAddress() != null) {
            m.setFrom(PEpUtils.createIdentity(mm.getFrom()[0], context));
        }
        m.setTo(PEpUtils.createIdentities(Arrays.asList(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO)), context));
        m.setCc(PEpUtils.createIdentities(Arrays.asList(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC)), context));
        m.setBcc(PEpUtils.createIdentities(Arrays.asList(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC)), context));
        m.setId(mm.getMessageId());
        m.setInReplyTo(createMessageReferences(mm.getHeader("In-Reply-To")));
        m.setSent(mm.getSentDate());
        Vector<Identity> identities = PEpUtils.createIdentities(Arrays.asList(mm.getReplyTo()), context);
        m.setReplyTo(identities);
        m.setReferences(createMessageReferences(mm.getReferences()));
        m.setShortmsg(mm.getSubject());


        ArrayList<Pair<String, String>> optionalFields = new ArrayList<>();

        for (String headerName : mm.getHeaderNames()) {
            if (!MimeHeader.MANDATORY_HEADER_NAMES.contains(headerName.toUpperCase())) {
                addOptionalField(optionalFields, headerName);
            }
        }

        m.setOptFields(optionalFields);
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            if (mm.getHeader("Received").length > 0) {
                Date received = formatter.parse(mm.getHeader("Received")[0].split(";")[1].trim());
                m.setRecv(received);
            }
        } catch (ParseException ignore) {
        }

        m.setEncFormat(Message.EncFormat.None);
    }

    private void addOptionalField(ArrayList<Pair<String, String>> optionalFields, String headerKey) {
        for (String headerValue : mm.getHeader(headerKey)) {
            optionalFields.add(new Pair<>(headerKey, headerValue));
        }
    }

    private Vector<String> createMessageReferences(String[] references) {
        Vector<String> rv = new Vector<>();
        if(references != null)
            for(String s : references)
                rv.add(s);
        return rv;
    }

    private String getFileName(Part part) throws MessagingException {
        String filename = MimeUtility.getHeaderParameter(part.getContentType(), "name");
        if (part.getMimeType().equalsIgnoreCase("message/rfc822")) return "ForwardedMessage.eml";
        if (filename == null) {
            String disposition = MimeUtility.unfoldAndDecode(part.getDisposition());
            if (isAnAttachment(part)) {
                Log.i("PEpMessageBuilder", "addBody 1 " + disposition);
                filename = MimeUtility.getHeaderParameter(disposition, "filename");
            }
        }

        return filename != null ? filename : DEFAULT_FILENAME;
    }

    private boolean isAnAttachment(Part part) {
        return "attachment".equalsIgnoreCase(MessageExtractor.getContentDisposition(part));
    }

}
