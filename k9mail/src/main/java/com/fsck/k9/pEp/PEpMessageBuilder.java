package com.fsck.k9.pEp;

import android.util.Log;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.mailstore.LocalBodyPart;

import org.pEp.jniadapter.Blob;
import org.pEp.jniadapter.Message;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Created by dietz on 14.12.15.
 */

class PEpMessageBuilder {
    private MimeMessage mm;

    PEpMessageBuilder(MimeMessage m) {
        mm = m;
    }

    Message createMessage() {
        Message m = null;
        try {
            m = new Message();

            addHeaders(m);
            addBody(m);
            return m;
        } catch (Throwable t) {
            if(m != null) m.close();
            Log.e("pEp", "Could not create pep message:", t);
        }
        return null;
    }

    private void addBody(Message m) throws MessagingException, UnsupportedEncodingException {
        // fiddle message txt from MimeMsg...
        // the buildup of mm should be like the follwing:
        // - html body if any, else plain text
        // - plain text body if html above
        // - many attachments (of type binarymemoryblob (hopefully ;-)).
        Body b = mm.getBody();
        if(b instanceof BinaryMemoryBody) {
            BinaryMemoryBody bmb = (BinaryMemoryBody) b;
            String text = new String(bmb.getData(), "UTF-8");
            m.setLongmsg(text);
            return;
        }

        MimeMultipart mmp = (MimeMultipart) b;
        Vector<Blob> attachments = new Vector<Blob>();
        int nrOfAttachment = mmp.getBodyParts().size();
        for (int i = 0; i < nrOfAttachment; i++) {
            MimeBodyPart mbp = (MimeBodyPart) mmp.getBodyPart(i);
            Log.d("pep", "Bodypart #" + i + ":" + mbp.toString() + "mime type:" + mbp.getMimeType() + "  Body:" + mbp.getBody().toString());
            if (mbp.isMimeType("text/plain")) {
            /*    TextBody tb = (TextBody) mbp.getBody();
                m.setLongmsg(tb.getText()); */
                BinaryMemoryBody bmb = (BinaryMemoryBody) mbp.getBody();
                String text = new String(bmb.getData(), "UTF-8");
                m.setLongmsg(text);
                Log.d("pep", "found Text: " + text);
            } else if (mbp.getBody() instanceof BinaryMemoryBody) {
                BinaryMemoryBody part = (BinaryMemoryBody) mbp.getBody();

                Blob blob = new Blob();
                blob.filename = MimeUtility.getHeaderParameter(mbp.getContentType(), "filename");     // TODO: test wether this works
                 if(blob.filename == null) blob.filename = "empty";
                blob.mime_type = mbp.getMimeType();
                blob.data = part.getData();
                attachments.add(blob);
                Log.d("pep", "BLOB #" + i + ":" + blob.mime_type + ":" + blob.filename);
            } else
                Log.i("pep", "Could not process part #" + i + ": " + mbp.toString());
            // TODO: HTML...
        }

/*            MimeBodyPart mbp = (MimeBodyPart) mmp.getBodyPart(0);
            BinaryMemoryBody bmb = (BinaryMemoryBody) mbp.getBody();
            m.setLongmsg(new String(bmb.getData(), "UTF-8"));

            //TODO: Handle pure text and multipart/alternative

            // and add attachments...
            for (int i = 1; i < nrOfAttachment; i++) {
                BodyPart p = mmp.getBodyPart(i);

                Log.d("pep", "Bodypart #" + i + ":" + p.toString() + " Body:" + p.getBody().toString());
                if (p.getBody() instanceof BinaryMemoryBody) {
                    BinaryMemoryBody part = (BinaryMemoryBody) p.getBody();

                    Blob blob = new Blob();
                    blob.filename = MimeUtility.getHeaderParameter(p.getContentType(), "filename");     // TODO: test wether this works
                    blob.mime_type = p.getMimeType();
                    blob.data = part.getData();
                    attachments.add(blob);
                    Log.d("pep", "BLOB #" + i + ":" + blob.mime_type + ":" + blob.filename);
                }
            } */
        m.setAttachments(attachments);
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
            // m.setRecv();
            m.setSent(mm.getSentDate());
            m.setReplyTo(PEpUtils.createIdentities(mm.getReplyTo()));
            // m.setRecvBy();
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
