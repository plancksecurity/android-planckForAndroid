package com.fsck.k9.pEp;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.mailstore.LocalBodyPart;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.SimpleMessageFormat;

import org.pEp.jniadapter.Color;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by dietz on 01.07.15.
 */
public class DummyPepProviderImpl implements PEpProvider {
    DummyPepProviderImpl() {
        // NOP - just to mask external usage, we have a factory
    }

    @Override
    public Color getPrivacyState(Address from, Address[] toAdresses, Address[] ccAdresses, Address[] bccAdresses) {
        if(toAdresses == null || toAdresses.length == 0 || toAdresses[0] == null) return Color.pEpRatingUndefined;
        if(toAdresses[0].getAddress().contains("alice")) return Color.pEpRatingTrusted;
        if(toAdresses[0].getAddress().contains("bob")) return Color.pEpRatingReliable;
        if(toAdresses[0].getAddress().contains("eve")) return Color.pEpRatingUnderAttack;
        return Color.pEpRatingUnreliable;
    }

    @Override
    public MimeMessage encryptMessage(MimeMessage source, String[]extra) {
        // TODO: pEp TextBodyBuilder oder MimeMessageHelper anschauen (s. MessageBuilder.java:324
        String newBody;
        dumpMimeMessage(source);
        MimeMessage newMessage;
        try {
            Address[] to = source.getRecipients(Message.RecipientType.TO);
            Color col = getPrivacyState(null, to, null, null);
           // if (col == Color.pEpRatingTrusted || col == Color.pEpRatingReliable) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                source.getBody().writeTo(os);
                newBody = "*** ENCRYPTED ***\n\r" + os.toString() + "\n\r*** Encryption ends ***";
                os.close();

          //  }

            newMessage = source.clone();
            newMessage.setUid(null);
            MimeMessageHelper.setBody(newMessage, new TextBody(newBody));


        } catch (Exception e) {
            Log.d(K9.LOG_TAG, " error encrypting ", e);
            newMessage = null;
        }
        dumpMimeMessage(newMessage);
        return newMessage != null? newMessage:source;
    }

    public void dumpMimeMessage(MimeMessage mm) {
        String out = "Root:\n";

        try {
            for (String header:mm.getHeaderNames())
                out += header + ":" + mm.getHeader(header) + "\n";
            out += "\n";
            out += "Message-Id:" + mm.getMessageId().hashCode() +"\n";
            out += mangleBody((MimeMultipart)mm.getBody());
            out += "hasAttachments:" + mm.hasAttachments();

        } catch (Exception e) {
            out += "\n\n" + e.getMessage();
        }

        Log.d("MIMEMESSAGE", out);

    }

    private String mangleBody(MimeMultipart body) throws Exception {
        String rv = "Body:\n";
        for(Part p: body.getBodyParts())
                rv += "     " + new String(((BinaryMemoryBody) p).getData()) +"\n";
            //rv+="  " + ((BinaryMemoryBody) p)(((LocalBodyPart) p).getBody())).getData().toString() +"\n";

        return rv;
    }
}
