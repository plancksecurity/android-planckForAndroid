package com.fsck.k9.pEp;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.SimpleMessageFormat;

import org.pEp.jniadapter.Color;

/**
 * Created by dietz on 01.07.15.
 */
public class DummyPepProviderImpl implements PEpProvider {
    DummyPepProviderImpl() {
        // NOP - just to mask external usage, we have a factory
    }

    @Override
    public Color getPrivacyState(Address[] toAdresses, Address[] ccAdresses, Address[] bccAdresses) {
        if(toAdresses == null || toAdresses.length == 0 || toAdresses[0] == null) return Color.pEpRatingUndefined;
        if(toAdresses[0].getAddress().contains("alice")) return Color.pEpRatingTrusted;
        if(toAdresses[0].getAddress().contains("bob")) return Color.pEpRatingReliable;
        if(toAdresses[0].getAddress().contains("eve")) return Color.pEpRatingUnderAttack;
        return Color.pEpRatingUnreliable;
    }

    @Override
    public MimeMessage encryptMessage(MimeMessage source) {
        try {
            Address[] to = source.getRecipients(Message.RecipientType.TO);     // FIXME: does this destroy our msg?
            Color col = getPrivacyState(to, null, null);
            if (col == Color.pEpRatingTrusted || col == Color.pEpRatingReliable) {
                String text = " *** encrypted ***";

                // TODO: TextBodyBuilder oder MimeMessageHelper anschauen (s. MessageBuilder.java:324

                // Capture composed message length before we start attaching quoted parts and signatures.
                int composedMessageLength = text.length();
                int composedMessageOffset = 0;

                TextBody body = new TextBody(text);
                body.setComposedMessageLength(composedMessageLength);
                body.setComposedMessageOffset(composedMessageOffset);

                source.setBody(body);
            }


        } catch (Exception e) {
            Log.d(K9.LOG_TAG, " error encrypting ", e);
        }
        return source;
    }
}
