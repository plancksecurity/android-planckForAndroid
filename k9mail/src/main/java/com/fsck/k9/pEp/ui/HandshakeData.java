package com.fsck.k9.pEp.ui;

import org.pEp.jniadapter.Identity;

public class HandshakeData {
    private final String fullTrustwords;
    private final String shortTrustwords;
    private final Identity myself;
    private final Identity partner;

    public HandshakeData(String fullTrustwords, String shortTrustwords, Identity myself, Identity partner) {
        this.fullTrustwords = fullTrustwords;
        this.shortTrustwords = shortTrustwords;
        this.myself = myself;
        this.partner = partner;
    }

    public String getFullTrustwords() {
        return fullTrustwords;
    }

    public String getShortTrustwords() {
        return shortTrustwords;
    }

    public Identity getMyself() {
        return myself;
    }

    public Identity getPartner() {
        return partner;
    }
}
