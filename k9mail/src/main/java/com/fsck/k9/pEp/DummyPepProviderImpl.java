package com.fsck.k9.pEp;

import com.fsck.k9.mail.Address;

import org.pEp.jniadapter.Color;

/**
 * Created by dietz on 01.07.15.
 */
public class DummyPepProviderImpl implements PEpProvider {
    @Override
    public Color getPrivacyState(Address[] adresses) {
        if(adresses == null || adresses.length == 0 || adresses[0] == null) return Color.pEpRatingUndefined;
        if(adresses[0].getAddress().contains("alice")) return Color.pEpRatingTrusted;
        if(adresses[0].getAddress().contains("bob")) return Color.pEpRatingReliable;
        if(adresses[0].getAddress().contains("eve")) return Color.pEpRatingUnderAttack;
        return Color.pEpRatingUnreliable;
    }
}
