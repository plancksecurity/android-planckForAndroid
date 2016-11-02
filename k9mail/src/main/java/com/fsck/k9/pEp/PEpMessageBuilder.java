package com.fsck.k9.pEp;

import android.content.Context;
import android.util.Log;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.*;
import org.pEp.jniadapter.Blob;
import org.pEp.jniadapter.Message;
import org.pEp.jniadapter.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.StreamHandler;

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
        } catch (Throwable t) {
            if(pEpMsg != null) pEpMsg.close();
            Log.e("pEp", "Could not create pep message:", t);
        }
        return null;
    }

    private void addBody(Message pEpMsg) throws MessagingException, IOException, UnsupportedEncodingException {
        // fiddle message txt from MimeMsg...
        // the buildup of mm should be like the follwing:
        // - html body if any, else plain text
        // - plain text body if html above
        // - many attachments (of type binarymemoryblob (hopefully ;-)).
        Body b = mm.getBody();
        Vector<Blob> attachments = new Vector<Blob>();

        if(!(b instanceof MimeMultipart)) { //FIXME: Don't do this assumption (if not Multipart then plain or html text)

                String disposition = MimeUtility.unfoldAndDecode(mm.getDisposition());
                if (("attachment".equalsIgnoreCase(MessageExtractor.getContentDisposition(mm)))) {
                    Log.i("PEpMessageBuilder", "addBody 1 " + disposition);
                    String filename = MimeUtility.getHeaderParameter(disposition, "filename");
                    addAttachment(attachments, mm.getContentType(), filename, PEpUtils.extractBodyContent(b));
                    pEpMsg.setLongmsg("");
                 //   return;
                }

            String charset =  MimeUtility.getHeaderParameter(mm.getContentType(), "charset");
            if (charset == null) {
                // failback when the header doesn't have charset parameter, defaults to UTF-8
                // FIXME: charset, trate non text bod4y types like application/pgp-keys
                charset = "UTF-8";
            }
            String text = new String(PEpUtils.extractBodyContent(b), charset);
            if(mm.isMimeType("text/html")) {
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

    private void handleMultipart(Message pEpMsg, MimeMultipart mmp, Vector<Blob> attachments) throws MessagingException, IOException, UnsupportedEncodingException {
        int nrOfParts = mmp.getBodyParts().size();
        for (int part = 0; part < nrOfParts; part++) {
            MimeBodyPart mbp = (MimeBodyPart) mmp.getBodyPart(part);
            Body mbp_body = mbp.getBody();
            if(mbp_body == null) {
                // eh? this can happen?!
                Log.e("pep", "mbp_body==null!");
                continue;
            }
            try {
                Log.d("pep", "Bodypart #" + part + ":" + mbp.toString() + "mime type:" + mbp.getMimeType() + "  Body:" + mbp_body.toString());
            } catch (Exception e) {
                Log.e("pep", "nope",e);
            }
            if (mbp_body instanceof MimeMultipart) {
                handleMultipart(pEpMsg, (MimeMultipart) mbp_body, attachments);
                continue;
            }
            //FIXME> Deal with non text and non multipart message and non attachments

            boolean plain = mbp.isMimeType("text/plain");
            if (plain || mbp.isMimeType("text/html")) {
                String charset = MimeUtility.getHeaderParameter(mbp.getContentType(), "charset");
                String text;
                if (charset != null) {
                    text = new String(PEpUtils.extractBodyContent(mbp_body), charset);
                } else {
                    text = new String(PEpUtils.extractBodyContent(mbp_body));
                }
                if(plain)
                    pEpMsg.setLongmsg(text);
                else
                    pEpMsg.setLongmsgFormatted(text);
                Log.d("pep", "found Text: " + text);
            } else  {
                String filename = getFileName(mbp);
                String type = mbp.getMimeType();
                byte data[] = PEpUtils.extractBodyContent(mbp_body);
                addAttachment(attachments, type, filename, data);
            }
        }
    }

    private void addAttachment(Vector<Blob> attachments, String mimeType, String filename, byte[] data) {
        Blob blob = new Blob();
        blob.filename = filename;
        blob.mime_type = mimeType;
        blob.data = data;
        attachments.add(blob);
        Log.d("pep", "PePMessageBuilder: BLOB #" + attachments.size() + ":" + mimeType + ":" + filename);


    }


    private void addHeaders(Message m, Context context) {
        try {
            // headers
            m.setFrom(PEpUtils.createIdentity(mm.getFrom()[0], context));
            m.setTo(PEpUtils.createIdentities(Arrays.asList(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO)), context));
            m.setCc(PEpUtils.createIdentities(Arrays.asList(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC)), context));
            m.setBcc(PEpUtils.createIdentities(Arrays.asList(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC)), context));
            m.setId(mm.getMessageId());
            m.setInReplyTo(createMessageReferences(mm.getReferences()));
            m.setSent(mm.getSentDate());
            m.setReplyTo(PEpUtils.createIdentities(Arrays.asList(mm.getReplyTo()), context));
            m.setReferences(createMessageReferences(mm.getReferences()));
            m.setShortmsg(mm.getSubject());

            // TODO: other headers
            ArrayList <Pair<String, String>> optionalFields = new ArrayList<>();
            if (mm.getHeader(MimeHeader.HEADER_PEP_AUTOCONSUME).length > 0 ) {
                optionalFields.add(new Pair<>(MimeHeader.HEADER_PEP_AUTOCONSUME, mm.getHeader(MimeHeader.HEADER_PEP_AUTOCONSUME)[0]));
            }
            m.setOptFields(optionalFields);
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            if (mm.getHeader("Received").length > 0){
                Date received = formatter.parse(mm.getHeader("Received")[0].split(";")[1].trim());
                m.setRecv(received);
            }
        } catch (ParseException ignore) {
        }

        } catch (MessagingException me) {
            Log.e("pep", "creating message headers", me);
        }
    }

    private Vector<String> createMessageReferences(String[] references) {
        Vector<String> rv = new Vector<String>();
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
            if (("attachment".equalsIgnoreCase(MessageExtractor.getContentDisposition(mm)))) {
                Log.i("PEpMessageBuilder", "addBody 1 " + disposition);
                filename = MimeUtility.getHeaderParameter(disposition, "filename");
            }
        }

        return filename != null ? filename : DEFAULT_FILENAME;
    }

}
