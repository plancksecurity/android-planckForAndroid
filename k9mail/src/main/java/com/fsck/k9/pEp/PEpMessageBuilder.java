package com.fsck.k9.pEp;

import android.util.Log;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.BinaryMemoryBody;

import org.pEp.jniadapter.Blob;
import org.pEp.jniadapter.Message;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Makes a pEp message from a k9 message
 */

class PEpMessageBuilder {
    private MimeMessage mm;

    PEpMessageBuilder(MimeMessage m) {
        mm = m;
    }

    Message createMessage() {
        Message pEpMsg = null;
        try {
            pEpMsg = new Message();

            addHeaders(pEpMsg);
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
        if(!(b instanceof MimeMultipart)) {
            String text = new String(PEpUtils.extractBodyContent(b), "UTF-8");   // FIXME: Encoding!
            pEpMsg.setLongmsg(text);
            return;
        }

        MimeMultipart mmp = (MimeMultipart) b;
        Vector<Blob> attachments = new Vector<Blob>();
        handleMultipart(pEpMsg, mmp, attachments);           // recurse into the Joys of Mime...

        pEpMsg.setAttachments(attachments);
    }

    private void handleMultipart(Message pEpMsg, MimeMultipart mmp, Vector<Blob> attachments) throws MessagingException, IOException, UnsupportedEncodingException {
        int nrOfParts = mmp.getBodyParts().size();
        for (int part = 0; part < nrOfParts; part++) {
            MimeBodyPart mbp = (MimeBodyPart) mmp.getBodyPart(part);
            Body mbp_body = mbp.getBody();
            Log.d("pep", "Bodypart #" + part + ":" + mbp.toString() + "mime type:" + mbp.getMimeType() + "  Body:" + mbp.getBody().toString());
            if (mbp_body instanceof MimeMultipart) {
                handleMultipart(pEpMsg, (MimeMultipart) mbp_body, attachments);
                continue;
            }

            boolean plain = mbp.isMimeType("text/plain");
            if (plain || mbp.isMimeType("text/html")) {
                String text = new String(PEpUtils.extractBodyContent(mbp_body), "UTF-8");      // FIXME: encoding!
                if(plain)
                    pEpMsg.setLongmsg(text);
                else
                    pEpMsg.setLongmsgFormatted(text);
                Log.d("pep", "found Text: " + text);
            } else  {
                Blob blob = new Blob();
                blob.filename = MimeUtility.getHeaderParameter(mbp.getContentType(), "name");
                if(blob.filename == null) {
                    blob.filename = "file";
                    Log.e("pep", "Could not determine filename.");
                }
                blob.mime_type = mbp.getMimeType();
                blob.data = PEpUtils.extractBodyContent(mbp_body);
                attachments.add(blob);
                Log.d("pep", "PePMessageBuilder: BLOB #" + part + ":" + blob.mime_type + ":" + blob.filename);
            }
        }
    }

    private void addHeaders(Message m) {
        try {
            // headers
            m.setFrom(PEpUtils.createIdentity(mm.getFrom()[0]));
            m.setTo(PEpUtils.createIdentities(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO)));
            m.setCc(PEpUtils.createIdentities(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC)));
            m.setBcc(PEpUtils.createIdentities(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC)));
            m.setId(mm.getMessageId());
            m.setInReplyTo(createMessageReferences(mm.getReferences()));
            m.setSent(mm.getSentDate());
            m.setReplyTo(PEpUtils.createIdentities(mm.getReplyTo()));
            m.setReferences(createMessageReferences(mm.getReferences()));
            m.setShortmsg(mm.getSubject());

            // TODO: other headers

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

}
