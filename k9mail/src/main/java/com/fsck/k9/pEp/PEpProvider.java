package com.fsck.k9.pEp;

import android.content.Context;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.Color;

/**
 * Created by dietz on 01.07.15.
 */
public interface PEpProvider {
    /**
     * checks the privacy level of the adresses supplied. This method creates a pEp message and
     * calls the jni adapter to obtain the info. According to fdik, this check returns fast (all
     * time consuming stuff (network i/o etc.) is done asynchronousely.
     *
     * @param from  from email adress
     * @param toAdresses to adresses
     * @param ccAdresses cc adresses
     * @param bccAdresses bcc adresses
     * @return the privacy level of a mail sent to the set of recipients
     */
    public Color getPrivacyState(Address from, Address[] toAdresses, Address[] ccAdresses, Address[] bccAdresses);
    public Color getPrivacyState(com.fsck.k9.mail.Message message);

    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real
     * pEp library interaction.
     *
     * Implications from feeding LocalMessages into decryptMessage are currently not complety understood...
     *
     * @param source the (fully qualified) message to be decrypted.
     * @return the decrypted message
     *
     * TODO: pEp: how do I get the color? Perhaps Via header value in return value?
     */
    public DecryptResult decryptMessage(MimeMessage source);

    /**
     * Encrypts one k9 message. This one hides all the black magic associated with the real
     * pEp library interaction.
     *
     * Implications from feeding LocalMessages into decryptMessage are currently not complety understood...
     *
     * FIXME: where do I handle Bcc: corner case?
     * FIXME: where do I handle split for different privacy levels (To1 is green, To2 is yellow?) Is this really necessary?
     *
     * @param source the (fully qualified) message to be encrypted.
     * @param extraKeys extra key ids to encrypt msg to...
     * @return the encrypted message
     */
    public MimeMessage encryptMessage(MimeMessage source, String[] extraKeys);

    /**
     * Helper for pEp setup. Smells funny to have it in an interface, but fits nowhere else.
     * FIXME: How long can I use the context?
     * @param c
     */
    public void setup(Context c);

    public class DecryptResult {
        DecryptResult(MimeMessage msg, Color col) {
            this.msg = msg;
            this.col = col;
        }
        final public MimeMessage msg;
        final public Color col;
    }
}