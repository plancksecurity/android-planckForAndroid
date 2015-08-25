package com.fsck.k9.pEp;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeMessage;
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
     * @param from  from email adress
     * @param toAdresses to adresses
     * @param ccAdresses cc adresses
     * @param bccAdresses bcc adresses
     * @return the privacy level of a mail sent to the set of recipients
     */
    public Color getPrivacyState(Address from, Address[] toAdresses, Address[] ccAdresses, Address[] bccAdresses);

    /**
     * Decrypts one k9 message. Hides all the black magic associated with the real
     * pEp library interaction.
     *
     * @param source the (fully qualified) message to be decrypted.
     * @return the decrypted message
     *
     * TODO: pEp: how do I get the color? Perhaps Via header value in return value?
     */
    public MimeMessage decryptMessage(MimeMessage source);

    /**
     * Encrypts one k9 message. This one hides all the black magic associated with the real
     * pEp library interaction.
     *
     * FIXME: where do I handle Cc:/Bcc: split?
     * FIXME: where do I handle split for different privacy levels? Is this really necessary?
     *
     * @param source the (fully qualified) message to be encrypted.
     * @param extraKeys extra key ids to encrypt msg to...
     * @return the encrypted message
     */
    public MimeMessage encryptMessage(MimeMessage source, String[] extraKeys);

    /**
     * checks wether the message given as parameter might be a message that should be piped through pEp
     * @param source
     * @return
     */
    public boolean mightBePEpMessage(MimeMessage source);
}
