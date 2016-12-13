package com.fsck.k9.pEp.ui;

import org.pEp.jniadapter.Identity;

public class HandshakeData {
    final String fullTrustwords;
    final String shortTrustwords;
    final Identity myself;
    final Identity partner;

    public HandshakeData(String fullTrustwords, String shortTrustwords, Identity myself, Identity partner) {
        this.fullTrustwords = fullTrustwords;
        this.shortTrustwords = shortTrustwords;
        this.myself = myself;
        this.partner = partner;
    }
}
