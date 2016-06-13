package com.fsck.k9.pEp;

import android.content.Context;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.DecryptFlags;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;

import java.util.List;

/**
 * Created by dietz on 01.07.15.
 */
public interface PEpProvider {
    /**
     * If is outgoing any copy of the message encrypted (yellow, green, and unsecure for some) it will be putted in this position,
     * if not, all copies will be unencrypted.
     */
    int ENCRYPTED_MESSAGE_POSITION = 0;
    public static final String PEP_OWN_USER_ID = "pEp_own_userId";

    /**
     * checks the privacy level of the adresses supplied. This method creates a pEp message and
     * calls the jni adapter to obtain the info. According to fdik, this check returns fast (all
     * time consuming stuff (network i/o etc.) is done asynchronousely.
     *
     * @param from        from email adress
     * @param toAdresses  to adresses
     * @param ccAdresses  cc adresses
     * @param bccAdresses bcc adresses
     * @return the privacy level of a mail sent to the set of recipients
     */
    Color getPrivacyState(Address from, List<Address> toAdresses, List<Address> ccAdresses, List<Address> bccAdresses);
    Color getPrivacyState(com.fsck.k9.mail.Message message);
    Color getPrivacyState(Message message);


    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real
     * pEp library interaction.
     * <p/>
     * Implications from feeding LocalMessages into decryptMessage are currently not complety understood...
     *
     * @param source the (fully qualified) message to be decrypted.
     * @return the decrypted message
     * <p/>
     * TODO: pEp: how do I get the color? Perhaps Via header value in return value?
     */
    DecryptResult decryptMessage(MimeMessage source);

    /**
     * Encrypts one k9 message. This one hides all the black magic associated with the real
     * pEp library interaction.
     * <p/>
     * Implications from feeding LocalMessages into decryptMessage are currently not complety understood...
     * <p/>
     * FIXME: where do I handle Bcc: corner case?
     * FIXME: where do I handle split for different privacy levels (To1 is green, To2 is yellow?) Is this really necessary?
     *
     * @param source    the (fully qualified) message to be encrypted.
     * @param extraKeys extra key ids to encrypt msg to...
     * @return the encrypted message
     */
    List<MimeMessage> encryptMessage(MimeMessage source, String[] extraKeys);

    /**
     * Helper for pEp setup. Smells funny to have it in an interface, but fits nowhere else.
     * FIXME: How long can I use the context?
     *
     * @param c
     */
    void setup(Context c);

    /**
     * Checks the trust status (Color) for a given identity
     *
     * @param identity
     * @return identity trust status color
     */
    Color identityColor(Identity identity);

    Color identityColor(Address address);

    /**
     * Retrive long trustwords for a given identity
     *
     * @param id
     * @return trustwords string
     */
    String trustwords(Identity id);

    /**
     * Close the engine/session associated to the provider
     */
    void close();

    /**
     * Returns a identity with the attributes related to the given identity filler, like fpr if available.
     *
     * @param id identity to fill
     * @return identity filled
     */
    Identity updateIdentity(Identity id);

    /**
     * Trust on identity
     *
     * @param id identity to trust it
     */
    void trustPersonaKey(Identity id);

    /**
     * Mark key as compromised
     *
     * @param id identity to mark
     */
    void keyCompromised(Identity id);

    /**
     * Reset id trust status, to do handshake again.
     *
     * @param id identity to reset trust
     */
    void resetTrust(Identity id);

    Identity myself(Identity myId);

    void setPassiveModeEnabled(boolean enable);

    void startKeyserverLookup();
    void stoptKeyserverLookup();

    class DecryptResult {
        DecryptResult(MimeMessage msg, Color col, DecryptFlags flags) {
            this.msg = msg;
            this.col = col;
            this.flags = flags;
        }

        final public MimeMessage msg;
        final public Color col;
        final public DecryptFlags flags;
    }
}