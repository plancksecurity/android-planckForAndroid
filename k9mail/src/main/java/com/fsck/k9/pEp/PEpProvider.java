package com.fsck.k9.pEp;

import com.fsck.k9.mail.Address;

import org.pEp.jniadapter.Color;

/**
 * Created by dietz on 01.07.15.
 */
public interface PEpProvider {
    /**
     * checks the privacy level of the adresses supplied. This method creates a pEp message and
     * calls the jni adapter to obtain the info. Accourding to fdik, this check returns fast (all
     * time consuming stuff (network i/o etc.) is done asynchronousely.
     *
     * @param adresses array of addresses to be checked. We do not distinguish between To, Cc and
     *                 Bcc
     * @return the privacy level of a mail sent to the set of recipients
     */

    // FIXME: differentiatze between to, cc and bcc. Do I need from? Yes, I think...
    Color getPrivacyState(Address[] adresses);
}
