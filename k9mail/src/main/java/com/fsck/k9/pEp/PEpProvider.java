package com.fsck.k9.pEp;

import android.content.Context;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import org.pEp.jniadapter.DecryptFlags;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;
import org.pEp.jniadapter.Rating;
import org.pEp.jniadapter.Sync;

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

    String PEP_ALWAYS_SECURE_TRUE = "1";

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
    Rating getPrivacyState(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses);
    Rating getPrivacyState(com.fsck.k9.mail.Message message);

    void getPrivacyState(com.fsck.k9.mail.Message message, ResultCallback<Rating> callback);

    void getPrivacyState(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses, ResultCallback<Rating> callback);

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

    void decryptMessage(MimeMessage source, ResultCallback<DecryptResult> callback);

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

    //TODO> When alias available check if it works correctly
    MimeMessage encryptMessageToSelf(MimeMessage source) throws MessagingException;

    /**
     * Checks the trust status (Color) for a given identity
     *
     * @param identity
     * @return identity trust status color
     */
    Rating getRating(Identity identity);

    void getRating(Identity identity, ResultCallback<Rating> callback);
    void getRating(Address address, ResultCallback<Rating> callback);

    Rating getRating(Address address);

    /**
     * Retrieve long trustwords for a given identity
     *
     * @param id
     * @return trustwords string
     */
    String trustwords(Identity id, String language);
    void trustwords(Identity myself, Identity partner, String lang,
                    ResultCallback<HandshakeData> callback);
    String trustwords(Identity myself, Identity partner, String lang, boolean isShort);

    void obtainTrustwords(Identity myself, Identity partner, String lang,
                          Boolean areTrustwordsShort, ResultCallback<HandshakeData> callback);

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

    List<KeyListItem> getBlacklistInfo();

    List<KeyListItem> getMasterKeysInfo();

    void addToBlacklist(String fpr);

    void deleteFromBlacklist(String fpr);

    //com.fsck.k9.mail.Message getMimeMessage(Message message);

    void setSyncSendMessageCallback(Sync.MessageToSendCallback callback);

    void setSyncHandshakeCallback(Sync.notifyHandshakeCallback callback);

    void startSync();

    void acceptHandshake(Identity identity);

    void rejectHandshake(Identity identity);

    void cancelHandshake(Identity identity);

    void loadMessageRatingAfterResetTrust(MimeMessage message, boolean isIncoming, Identity id, ResultCallback<Rating> loadedCallback);

    String getLog();

    void printLog();

    void loadOwnIdentities(ResultCallback<List<Identity>> callback);

    void setIdentityFlag(Identity identity, Integer flags, CompletedCallback completedCallback);

    void unsetIdentityFlag(Identity identity, Integer flags, CompletedCallback completedCallback);

    void setIdentityFlag(Identity identity, Integer flags);

    void unsetIdentityFlag(Identity identity, Integer flags);

    void setFastPollingCallback(Sync.NeedsFastPollCallback needsFastPollCallback);

    Rating incomingMessageRating(MimeMessage message);

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
        public final DecryptFlags flags;

        public DecryptResult(MimeMessage msg, Rating rating, KeyDetail keyDetails, DecryptFlags flags) {
            this.msg = msg;
            this.rating = rating;
            this.keyDetails = keyDetails;
            this.flags = flags;
        }

        final public MimeMessage msg;
        final public Rating rating;
    }

     enum ProtectionScope {
        ACCOUNT,
        MESSAGE
    }

    interface Callback {
        void onError(Throwable throwable);
    }

    interface ResultCallback<Result> extends Callback {
        void onLoaded(Result result);
    }

    interface CompletedCallback extends Callback {
        void onComplete();
    }
}