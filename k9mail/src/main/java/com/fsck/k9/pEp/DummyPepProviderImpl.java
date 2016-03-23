package com.fsck.k9.pEp;

import android.content.Context;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

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

    @Override
    public Color getIdentityColor(Address address) {
        return null;
        //nop
    }

    @Override
    public void close() {
        //nop
    }

    @Override
    public Identity updateIdentity(Identity id) {
        return null;
    }

    @Override
    public void trustPersonaKey(Identity id) {
        //nop
    }

    @Override
    public String trustwords(Identity id) {
        return null;
        //np
    }

    @Override
    public Color getIdentityColor(Identity ident) {
        return null;
    }
}
