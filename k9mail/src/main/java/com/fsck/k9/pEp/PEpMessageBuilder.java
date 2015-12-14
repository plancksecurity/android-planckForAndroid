package com.fsck.k9.pEp;

import android.util.Log;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.mailstore.LocalBodyPart;
import org.pEp.jniadapter.Blob;
import org.pEp.jniadapter.Message;

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

            addHeaders(m, mm);
            addBody(m, mm);
            return m;
        } catch (Throwable t) {
            if(m != null) m.close();
            Log.e("pEp", "Could not create message:", t);
        }
        return null;
    }

    private void addBody(Message m, MimeMessage mm) {
        try {
            // fiddle message txt from MimeMsg...
            MimeMultipart mmp = (MimeMultipart) mm.getBody();
            LocalBodyPart lbp = (LocalBodyPart) mmp.getBodyPart(0);
            BinaryMemoryBody bmb = (BinaryMemoryBody) lbp.getBody();
            m.setLongmsg(new String(bmb.getData(), "UTF-8"));

            //TODO: Handle pure text and multipart/alternative

            // and add attachments...
            Vector<Blob> attachments = new Vector<Blob>();
            int nrOfAttachment = mmp.getBodyParts().size();
            for (int i = 1; i < nrOfAttachment; i++) {
                BodyPart p = mmp.getBodyPart(i);

                Log.d("pep", "Bodypart #" + i + ":" + p.toString() + " Body:" + p.getBody().toString());
                if (p.getBody() instanceof BinaryMemoryBody) {
                    BinaryMemoryBody part = (BinaryMemoryBody) p.getBody();

                    // TODO: filename
                    Blob blob = new Blob();
                    blob.filename = "qwerty";
                    blob.mime_type = p.getMimeType();
                    blob.data = part.getData();
                    attachments.add(blob);
                    Log.d("pep", "BLOB #" + i + ":" + blob.mime_type);
                }
            }
            m.setAttachments(attachments);
        } catch (Exception e) {
            Log.e("pep", "creating message body", e);
        }
    }

    private void addHeaders(Message m, MimeMessage mm) {
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
    }

    private Vector<String> createMessageReferences(String[] references) {
        Vector<String> rv = new Vector<String>();
        if(references != null)
            for(String s : references)
                rv.add(s);
        return rv;
    }

}
