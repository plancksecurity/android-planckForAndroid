package com.fsck.k9.pEp;

import android.content.Context;
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
    public Color getPrivacyState(Message message) { return Color.pEpRatingB0rken; }

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
        return null;
    }
    @Override
    public DecryptResult decryptMessage(MimeMessage source) {
        return null;
    }

    public boolean mightBePEpMessage(MimeMessage source) {
        return true;
    }

    @Override
    public void setup(Context c) {
        // nop
    }

}
