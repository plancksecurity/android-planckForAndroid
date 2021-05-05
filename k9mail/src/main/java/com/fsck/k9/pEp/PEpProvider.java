package com.fsck.k9.pEp;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import java.util.List;
import java.util.Map;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Message;
import foundation.pEp.jniadapter.Rating;
import foundation.pEp.jniadapter.Sync;
import foundation.pEp.jniadapter.exceptions.pEpException;
import timber.log.Timber;
import java.util.Vector;

/**
 * Created by dietz on 01.07.15.
 */
public interface PEpProvider extends AutoCloseable {
    /**
     * If is outgoing any copy of the message encrypted (yellow, green, and un secure for some) it will be putted in this position,
     * if not, all copies will be unencrypted.
     */
    int ENCRYPTED_MESSAGE_POSITION = 0;
    String PEP_OWN_USER_ID = "pEp_own_userId";
    int HALF_FINGERPRINT_LENGTH = 24;
    //long TIMEOUT = 4 * 60 * 60 * 1000;
    long TIMEOUT = 10 * 60 * 1000;


    String PEP_PRIVATE_KEY_FPR = "pEpDetailsFpr";
    String PEP_PRIVATE_KEY_ADDRESS = "pEpDetailsAddress";
    String PEP_PRIVATE_KEY_USERNAME = "pEpDetailsUsername";
    String PEP_PRIVATE_KEY_FROM = "pEpDetailsFrom";

    String PEP_ALWAYS_SECURE_TRUE = "yes";
    String PEP_KEY_LIST_SEPARATOR = ",";
    String KEY_MIOSSING_ERORR_MESSAGE = "Key missing";

    void setup();

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
    Rating getRating(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses);
    Rating getRating(com.fsck.k9.mail.Message message);

    void getRating(com.fsck.k9.mail.Message message, ResultCallback<Rating> callback);

    void getRating(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses, ResultCallback<Rating> callback);

    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real
     * pEp library interaction.
     * <p/>
     * Implications from feeding LocalMessages into decryptMessage are currently not completely understood...
     *
     * @param source the (fully qualified) message to be decrypted.
     * @param receivedBy The email address which received the message
     * @return the decrypted message or the original message in case we cannot decrypt
     * <p/>
     * TODO: pEp: how do I get the color? Perhaps Via header value in return value?
     */
    DecryptResult decryptMessage(MimeMessage source, String receivedBy);

    /**
     * Decrypts one k9 MimeMessage. Hides all the black magic associated with the real pEp library interaction.
     * <p/>
     *
     * @param source the (fully qualified) message to be decrypted.
     * @return the decrypted message or error en case we cannot decrypt or engine fails
     * <p/>
     */
    void decryptMessage(MimeMessage source, Account account, ResultCallback<DecryptResult> callback);

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
    MimeMessage encryptMessageToSelf(MimeMessage source, String[] keys) throws MessagingException;

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

    String trustwords(Identity myself, Identity partner, String lang, boolean isShort);

    void trustwords(Identity myself, Identity partner, String lang, boolean isShort,SimpleResultCallback<String> callback);

    void obtainTrustwords(Identity myself, Identity partner, String lang, Boolean areKeysyncTrustwords,
                          ResultCallback<HandshakeData> callback);

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
     * Trust own identity
     *
     * @param id identity to trust it
     */
    void trustOwnKey(Identity id);

    /**
     * Mark key as compromised
     *
     * @param id identity to mark
     */
    void keyMistrusted(Identity id);

    /**
     * Reset id trust status, to do handshake again.
     *
     * @param id identity to reset trust
     */
    void resetTrust(Identity id);

    Identity myself(Identity myId);

    Identity setOwnIdentity(Identity id, String fpr);

    void setPassiveModeEnabled(boolean enable);

    void startKeyserverLookup();
    void stopKeyserverLookup();

    KeyDetail getOwnKeyDetails(Message message);

    void setSubjectProtection(boolean enabled);

    void configPassphrase(String passphrase);

    void configPassphraseForNewKeys(boolean enable, String passphrase);

    List<KeyListItem> getBlacklistInfo();

    List<KeyListItem> getMasterKeysInfo();

    void addToBlacklist(String fpr);

    void deleteFromBlacklist(String fpr);

    //com.fsck.k9.mail.Message getMimeMessage(Message message);

    void acceptSync();

    void rejectSync();

    void cancelSync();

    void loadMessageRatingAfterResetTrust(MimeMessage message, boolean isIncoming, Identity id, ResultCallback<Rating> loadedCallback);

    String getLog();

    String getLog(CompletedCallback callback);

    void printLog();

    void loadOwnIdentities(ResultCallback<List<Identity>> callback);

    void setIdentityFlag(Identity identity, Integer flags, CompletedCallback completedCallback);

    void unsetIdentityFlag(Identity identity, Integer flags, CompletedCallback completedCallback);

    void setIdentityFlag(Identity identity, boolean sync);

    void unsetIdentityFlag(Identity identity, Integer flags);

    void setFastPollingCallback(Sync.NeedsFastPollCallback needsFastPollCallback);

    Rating incomingMessageRating(MimeMessage message);

    void incomingMessageRating(MimeMessage message, ResultCallback<Rating> callback);

    void loadOutgoingMessageRatingAfterResetTrust(Identity identity, Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses, ResultCallback<Rating> callback);

    Map<String, PEpLanguage> obtainLanguages();

    com.fsck.k9.mail.Message generatePrivateKeyMessage(MimeMessage message, String fpr);

    Message encryptMessage(Message result) throws pEpException;

    boolean canEncrypt(String address);

    Vector<Identity> importKey(byte[] key);

    void keyResetIdentity(Identity ident, String fpr);

    void keyResetUser(String userId, String fpr);

    void keyResetAllOwnKeys();

    void leaveDeviceGroup();

    void startSync();
    void stopSync();
    boolean isSyncRunning();

    void setSyncSendMessageCallback(Sync.MessageToSendCallback callback);

    void setSyncHandshakeCallback(Sync.NotifyHandshakeCallback callback);

    class KeyDetail {
        private final Address address;
        private final String fpr;

        public KeyDetail(String fpr, Address address) {
            this.fpr = fpr;
            this.address = address;
        }

        public String getFpr() {
            return fpr;
        }

        public Address getAddress() {
            return address;
        }

        public String getUsername() {
            return address.getPersonal();
        }

        public String getStringAddress() {
            return address.getAddress();
        }
    }

    class DecryptResult {
        public int flags = -1;
        final public MimeMessage msg;
        final public Rating rating;
        final public boolean isFormerlyEncryptedReUploadedMessage;

        public DecryptResult(MimeMessage msg, Rating rating, int flags, boolean isEncrypted) {
            this.msg = msg;
            this.rating = rating;
            this.flags = flags;
            this.isFormerlyEncryptedReUploadedMessage = isFormerlyEncryptedReUploadedMessage(isEncrypted);
        }

        /**
         * @deprecated Legacy constructor to be removed with PEpProviderImpl
         */
        @Deprecated
        public DecryptResult(MimeMessage msg, Rating rating, int flags) {
            this.msg = msg;
            this.rating = rating;
            this.flags = flags;
            this.isFormerlyEncryptedReUploadedMessage = false;
        }

        private boolean isFormerlyEncryptedReUploadedMessage(boolean isEncrypted) {
            return isEncrypted && rating.value >= Rating.pEpRatingUnreliable.value;
        }
    }

     enum ProtectionScope {
        ACCOUNT,
        MESSAGE
    }

    enum TrustAction {
        TRUST,
        MISTRUST
    }

    interface Callback {
        void onError(Throwable throwable);
    }

    interface ResultCallback<Result> extends Callback {
        void onLoaded(Result result);
    }

    abstract class SimpleResultCallback<Result> implements ResultCallback<Result> {
        @Override
        public void onError(Throwable throwable) {
            Timber.e(throwable);
        }
    }

    interface CompletedCallback extends Callback {
        void onComplete();
    }
}