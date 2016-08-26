package com.fsck.k9.pEp;

import android.content.Context;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;

import java.util.List;

/**
 * Created by dietz on 01.07.15.
 */
public interface PEpProvider {
    /**
     * If is outgoing any copy of the message encrypted (yellow, green, and un secure for some) it will be putted in this position,
     * if not, all copies will be unencrypted.
     */
    int ENCRYPTED_MESSAGE_POSITION = 0;
    String PEP_OWN_USER_ID = "pEp_own_userId";
    int HALF_FINGERPRINT_LENGTH = 24;

    String PEP_PRIVATE_KEY_FPR = "pEpDetailsFpr";
    String PEP_PRIVATE_KEY_ADDRESS = "pEpDetailsAddress";
    String PEP_PRIVATE_KEY_USERNAME = "pEpDetailsUsername";
    String PEP_PRIVATE_KEY_FROM = "pEpDetailsFrom";

    /**
     * checks the privacy level of the addresses supplied. This method creates a pEp message and
     * calls the jni adapter to obtain the info. According to fdik, this check returns fast (all
     * time consuming stuff (network i/o etc.) is done asynchronously.
     *
     * @param from        from email address
     * @param toAddresses  to addresses
     * @param ccAddresses  cc addresses
     * @param bccAddresses bcc addresses
     * @return the privacy level of a mail sent to the set of recipients
     */
    Color getPrivacyState(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses);
    Color getPrivacyState(com.fsck.k9.mail.Message message);
    Color getPrivacyState(Message message);


    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real
     * pEp library interaction.
     * <p/>
     * Implications from feeding LocalMessages into decryptMessage are currently not completely understood...
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
     * Implications from feeding LocalMessages into decryptMessage are currently not completely understood...
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
     * Retrieve long trustwords for a given identity
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
    void stopKeyserverLookup();

    KeyDetail getOwnKeyDetails(Message message);

    void setSubjectUnprotected(boolean enabled);

     class KeyDetail {
        private final Address address;
        private final String detailMessage;
        private final String fpr;

        public KeyDetail(String detailMessage, String fpr, Address address) {
            this.detailMessage = detailMessage;
            this.fpr = fpr;
            this.address = address;
        }

        public String getDetailMessage() {
            return detailMessage;
        }

        public String getFpr() {
            return fpr;
        }

        public Address getAddress() {
            return address;
        }
    }

    class DecryptResult {
        public final KeyDetail keyDetails;

        public DecryptResult(MimeMessage msg, Color col, KeyDetail keyDetails) {
            this.msg = msg;
            this.col = col;
            this.keyDetails = keyDetails;
        }

        final public MimeMessage msg;
        final public Color col;
    }

     enum ProtectionScope {
        ACCOUNT,
        MESSAGE
    }
}