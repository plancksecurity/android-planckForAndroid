package com.fsck.k9.pEp;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.pEp.infrastructure.exceptions.AppCannotDecryptException;
import com.fsck.k9.pEp.infrastructure.exceptions.AppDidntEncryptMessageException;
import com.fsck.k9.pEp.infrastructure.exceptions.AuthFailurePassphraseNeeded;
import com.fsck.k9.pEp.infrastructure.exceptions.AuthFailureWrongPassphrase;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import foundation.pEp.jniadapter.DecryptFlags;
import foundation.pEp.jniadapter.Engine;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Message;
import foundation.pEp.jniadapter.Pair;
import foundation.pEp.jniadapter.Rating;
import foundation.pEp.jniadapter.Sync;
import foundation.pEp.jniadapter.SyncHandshakeResult;
import foundation.pEp.jniadapter.pEpCannotCreateKey;
import foundation.pEp.jniadapter.pEpException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.inject.Inject;

import foundation.pEp.jniadapter.pEpPassphraseRequired;
import foundation.pEp.jniadapter.pEpWrongPassphrase;
import security.pEp.ui.PassphraseProvider;
import timber.log.Timber;

/**
 * pep provider implementation. Dietz is the culprit.
 */
public class PEpProviderImpl implements PEpProvider {
    private static final String TAG = "pEpEngine-provider";
    private static final String PEP_SIGNALING_BYPASS_DOMAIN = "@peptunnel.com";
    private final ThreadExecutor threadExecutor;
    private final PostExecutionThread postExecutionThread;
    private final Context context;
    private Engine engine;

    @Inject
    public PEpProviderImpl(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread, Context context) {
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = postExecutionThread;
        this.context = context;
        createEngineInstanceIfNeeded();
    }

    @Override
    public synchronized Rating getRating(com.fsck.k9.mail.Message message) {
        Address from = message.getFrom()[0];                            // FIXME: From is an array?!
        List<Address> to = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO));
        List<Address> cc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC));
        List<Address> bcc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC));
        return getRating(from, to, cc, bcc);
    }

    @Override
    public void getRating(com.fsck.k9.mail.Message message, ResultCallback<Rating> callback) {
        Address from = message.getFrom()[0];                            // FIXME: From is an array?!
        List<Address> to = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO));
        List<Address> cc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC));
        List<Address> bcc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC));
        getRating(from, to, cc, bcc, callback);
    }

    private Rating getRating(Message message) {
        try {
            if (engine == null) {
                createEngineSession();
            }
            return engine.outgoing_message_rating(message);
        } catch (pEpException e) {
            Timber.e(e, "%s %s", TAG, "during getRating:");
        }
        return Rating.pEpRatingUndefined;
    }

    private void createEngineSession() throws pEpException {
        engine = new Engine();
        initEngineConfig(engine);
    }

    private void initEngineConfig(Engine engine) {
        engine.config_passive_mode(K9.getPEpPassiveMode());
        configKeyServerLockup(K9.getPEpUseKeyserver());
        engine.config_unencrypted_subject(!K9.ispEpSubjectProtection());
        engine.setMessageToSendCallback(MessagingController.getInstance(context));
        engine.setNotifyHandshakeCallback(((K9) context.getApplicationContext()).getNotifyHandshakeCallback());
        engine.setPassphraseRequiredCallback(PassphraseProvider.INSTANCE.getPassphraseRequiredCallback(context));
    }

    private Engine getNewEngineSession() throws pEpException {
        Engine engine = new Engine();
        initEngineConfig(engine);
        return engine;
    }

    private void configKeyServerLockup(boolean pEpUseKeyserver) {
        if (pEpUseKeyserver) startKeyserverLookup();
        else stopKeyserverLookup();
    }

    //Don't instantiate a new engine
    @Override
    public synchronized Rating getRating(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses) {
        if (bccAddresses.size()  > 0){
            return Rating.pEpRatingUnencrypted;
        }
        int recipientsSize = toAddresses.size() + ccAddresses.size() + bccAddresses.size();
        if (from == null || recipientsSize == 0)
            return Rating.pEpRatingUndefined;

        Message testee = null;
        try {
            if (engine == null) {
                createEngineSession();

            }
            testee = new Message();

            Identity idFrom = PEpUtils.createIdentity(from, context);
            idFrom.user_id = PEP_OWN_USER_ID;
            idFrom.me = true;
            testee.setFrom(idFrom);
            testee.setTo(PEpUtils.createIdentities(toAddresses, context));
            testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
            testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
            testee.setShortmsg("hello, world");     // FIXME: do I need them?
            testee.setLongmsg("Lorem ipsum");
            testee.setDir(Message.Direction.Outgoing);

            Rating result = engine.outgoing_message_rating(testee);   // stupid way to be able to patch the value in debugger
            Timber.i(TAG, "getRating " + result.name());

            return result;
        } catch (Throwable e) {
            Timber.e(e, "%s %s", TAG, "during color test:");
        } finally {
            if (testee != null) testee.close();
        }

        return Rating.pEpRatingUndefined;
    }

    @Override
    public void getRating(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses, ResultCallback<Rating> callback) {
        getRating(null, from, toAddresses, ccAddresses, bccAddresses, callback);
    }

    private boolean isUnencryptedForSome(List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses) {
        for (Address toAddress : toAddresses) {
            if (getRating(toAddress).value > Rating.pEpRatingUnencrypted.value) return true;
        }
        for (Address ccAddress : ccAddresses) {
            if (getRating(ccAddress).value > Rating.pEpRatingUnencrypted.value) return true;
        }
        for (Address bccAddress : bccAddresses) {
            if (getRating(bccAddress).value > Rating.pEpRatingUnencrypted.value) return true;
        }
        return false;
    }

    @Override
    public synchronized DecryptResult decryptMessage(MimeMessage source) {
        Timber.d(TAG, "decryptMessage() enter");
        Message srcMsg = null;
        Engine.decrypt_message_Return decReturn = null;
        try {
            if (engine == null) createEngineSession();
            srcMsg = new PEpMessageBuilder(source).createMessage(context);
            srcMsg.setDir(Message.Direction.Incoming);

            Timber.d("%s %s", TAG, "pEpdecryptMessage() before decrypt");
            decReturn = engine.decrypt_message(srcMsg, new Vector<>(), 0);
            Timber.d("%s %s", TAG, "pEpdecryptMessage() *after* decrypt");

            Timber.d(TAG, "pEpdecryptMessage() after decrypt Subject" +  decReturn.dst.getShortmsg());
            Message message = decReturn.dst;
            MimeMessage decMsg = getMimeMessage(source, message);

            if (PEpUtils.isAutoConsumeMessage(decMsg)) {
                Timber.e("%s %s", TAG, "Called decrypt on auto-consume message");
                if (K9.DEBUG) {
                    //Not using Timber on purpose.
                    Log.e( TAG,  message.getAttachments().get(0).toString());
                }
            } else {
                Timber.e("%s %s", TAG, "Called decrypt on non auto-consume message");
                Timber.e("%s %s", TAG, "Subject: " + decMsg.getSubject() + "Message-id: " + decMsg.getMessageId());

            }
            boolean neverUnprotected = decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE).length > 0
                    && decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE)[0].equals(PEP_ALWAYS_SECURE_TRUE);
            decMsg.setFlag(Flag.X_PEP_NEVER_UNSECURE, neverUnprotected);

            extractpEpImportHeaderFromReplyTo(decMsg);
            // TODO: 2020-02-20 Seem like this flgs currently are not used on the engine, this needs to be reviewed and probably removed
            DecryptResult flaggedResult = processKeyImportSyncMessages(decReturn, decMsg);
            if (flaggedResult != null) return flaggedResult;

            return new DecryptResult(decMsg, decReturn.rating, -1);
        } catch (Throwable t) {
            Timber.e(t, "%s %s", TAG, "while decrypting message: "  + source.getSubject()
                    + "\n" + source.getFrom()[0]
                    + "\n" + source.getSentDate().toString()
                    + "\n" + source.getMessageId());
            throw new AppCannotDecryptException("Could not decrypt", t);
        } finally {
            if (srcMsg != null) srcMsg.close();
            if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
            Timber.d(TAG, "decryptMessage() exit");
        }
    }

    @org.jetbrains.annotations.Nullable
    private DecryptResult processKeyImportSyncMessages(Engine.decrypt_message_Return decReturn, MimeMessage decryptedMimeMessage) {
        int flags = -1;
        Date lastValidDate = new Date(System.currentTimeMillis() - (TIMEOUT));

        if (decryptedMimeMessage.getHeaderNames().contains(MimeHeader.HEADER_PEP_KEY_IMPORT)
                || decryptedMimeMessage.getHeaderNames().contains(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY)) {

            if (lastValidDate.after(decryptedMimeMessage.getSentDate())) {
                flags = DecryptFlags.pEpDecryptFlagConsumed.value;
                return new DecryptResult(decryptedMimeMessage, decReturn.rating, flags);
            }
        }
        else if (PEpUtils.isAutoConsumeMessage(decryptedMimeMessage)) {
            if (lastValidDate.after(decryptedMimeMessage.getSentDate())) {
                flags = DecryptFlags.pEpDecryptFlagConsumed.value;
            } else {
                flags = DecryptFlags.pEpDecryptFlagIgnored.value;
            }
            return new DecryptResult(decryptedMimeMessage, decReturn.rating, flags);
        }
        return null;
    }

    private void extractpEpImportHeaderFromReplyTo(MimeMessage decMsg) {
        Vector<Address> replyTo = new Vector<>();
        for (Address address : decMsg.getReplyTo()) {
            if (address.getHostname().contains("peptunnel")) {
                decMsg.addHeader(MimeHeader.HEADER_PEP_KEY_IMPORT, address.getPersonal());
                decMsg.addHeader(MimeHeader.HEADER_PEP_AUTOCONSUME, "true");
            } else if (address.getAddress().contains(MimeHeader.HEADER_PEP_AUTOCONSUME.toUpperCase(Locale.ROOT))) {
                decMsg.addHeader(MimeHeader.HEADER_PEP_AUTOCONSUME, "true");
            } else {
                replyTo.add(address);
            }
        }

        decMsg.setReplyTo(replyTo.toArray(new Address[0]));
    }

    @Override
    public void decryptMessage(MimeMessage source, Account account, ResultCallback<DecryptResult> callback) {
        threadExecutor.execute(() -> {
            Timber.d(TAG, "decryptMessage() enter");
            Message srcMsg = null;
            Engine.decrypt_message_Return decReturn = null;
            Engine engine = null;
            try {
                engine = getNewEngineSession();

                srcMsg = new PEpMessageBuilder(source).createMessage(context);
                srcMsg.setDir(Message.Direction.Incoming);

                Timber.d(TAG, "decryptMessage() before decrypt");
                decReturn = engine.decrypt_message(srcMsg, new Vector<>(), 0);
                Timber.d(TAG, "decryptMessage() after decrypt");

                if (decReturn.rating == Rating.pEpRatingCannotDecrypt
                        || decReturn.rating == Rating.pEpRatingHaveNoKey){
                    notifyError(new AppCannotDecryptException(PEpProvider.KEY_MIOSSING_ERORR_MESSAGE), callback);
                    return;
                }

                Message message = decReturn.dst;
                MimeMessage decMsg = getMimeMessage(source, message);

                if (source.getFolder().getName().equals(account.getSentFolderName())
                        || source.getFolder().getName().equals(account.getDraftsFolderName())) {
                    decMsg.setHeader(MimeHeader.HEADER_PEP_RATING, PEpUtils.ratingToString(getRating(source)));
                }

                notifyLoaded(new DecryptResult(decMsg, decReturn.rating, decReturn.flags), callback);

            } catch (Throwable t) {
                Timber.e(t, "%s %s", TAG, "while decrypting message:");
                notifyError(new AppCannotDecryptException("Could not decrypt", t), callback);
            } finally {
                if (srcMsg != null) srcMsg.close();
                if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
                if (engine != null) {
                    engine.close();
                }
                Timber.d(TAG, "decryptMessage() exit");
            }
        });
    }

    @NonNull
    private static MimeMessage getMimeMessage(MimeMessage source, Message message) throws MessagingException {
        MimeMessageBuilder builder = new MimeMessageBuilder(message).newInstance();

        String text = message.getLongmsgFormatted();
        SimpleMessageFormat messageFormat;
        if (!TextUtils.isEmpty(message.getLongmsgFormatted())) {
            messageFormat = SimpleMessageFormat.HTML;
        } else {
            messageFormat = SimpleMessageFormat.TEXT;
            text = message.getLongmsg();
        }

        Date sent = message.getSent();
        if (sent == null) sent = new Date();

        Address[] replyTo = new Address[0];
        if (source != null) {
            replyTo = source.getReplyTo();
        }
        builder.setSubject(message.getShortmsg())
                .setSentDate(sent)
                .setHideTimeZone(K9.hideTimeZone())
                .setTo(PEpUtils.createAddressesList(message.getTo()))
                .setCc(PEpUtils.createAddressesList(message.getCc()))
                .setBcc(PEpUtils.createAddressesList(message.getBcc()))
                .setInReplyTo(PEpUtils.clobberVector(message.getInReplyTo()))
                .setReferences(PEpUtils.clobberVector(message.getReferences()))
                .setIdentity(message.getFrom(), replyTo)
                .setMessageFormat(messageFormat)
                //.setMessageFormat(message.getEncFormat())
                .setText(text)
                .setAttachments(message.getAttachments(), message.getEncFormat());
        //.setSignature(message.get)
        //.setSignatureBeforeQuotedText(mAccount.isSignatureBeforeQuotedText())
        //.setIdentityChanged(message.get)
        //.setSignatureChanged(mSignatureChanged)
        //.setCursorPosition(mMessageContentView.getSelectionStart())
        //TODO rethink message reference
        //.setMessageReference(source.getReferences());
        //.setDraft(isDraft)
        //.setIsPgpInlineEnabled(cryptoStatus.isPgpInlineModeEnabled())
        //.setForcedUnencrypted(recipientPresenter.isForceUnencrypted());


        MimeMessage mimeMessage = builder.parseMessage(message);

        boolean isRequestedFromPEpMessage = source == null;
        if (!isRequestedFromPEpMessage) {
            String[] alwaysSecureHeader = source.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE);
            if (alwaysSecureHeader.length > 0) {
                mimeMessage.addHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE, alwaysSecureHeader[0]);
            }
            mimeMessage.setFlags(source.getFlags(), true);
        }
        return mimeMessage;
    }

    private boolean isUsablePrivateKey(Engine.decrypt_message_Return result) {
        // TODO: 13/06/16 Check if is necesary check own id
        return result.rating.value >= Rating.pEpRatingTrusted.value
                && result.flags == 0x01;
    }

    @Override
    public synchronized List<MimeMessage> encryptMessage(MimeMessage source, String[] extraKeys) {
        // TODO: 06/12/16 add unencrypted for some
        Timber.d(TAG, "encryptMessage() enter");
        List<MimeMessage> resultMessages = new ArrayList<>();
        Message message = new PEpMessageBuilder(source).createMessage(context);
        try {
            if (engine == null) createEngineSession();
            if (source.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY).length > 0) {
                String key = source.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY)[0];
                Vector<Identity> replyTo = message.getReplyTo();
                if (replyTo == null) {
                    replyTo = new Vector<>();
                }
                replyTo.add(PEpUtils.createIdentity(new Address(key + PEP_SIGNALING_BYPASS_DOMAIN, key), context));
                message.setReplyTo(replyTo);
            }
            resultMessages.add(getEncryptedCopy(source, message, extraKeys));
            return resultMessages;
        } catch (pEpPassphraseRequired e) {
            Timber.e(e, "%s %s", TAG, "while encrypting message:");
            throw new AuthFailurePassphraseNeeded();
        } catch (pEpWrongPassphrase e) {
            Timber.e(e, "%s %s", TAG, "while encrypting message:");
            throw new AuthFailureWrongPassphrase();
        } catch (AppDidntEncryptMessageException e) {
            throw e;
        } catch (Throwable t) {
            Timber.e(t, "%s %s", TAG, "while encrypting message:");
            throw new RuntimeException("Could not encrypt", t);
        } finally {
            Timber.d(TAG, "encryptMessage() exit");
        }
    }

    @Override
    public synchronized MimeMessage encryptMessageToSelf(MimeMessage source, String[] keys) {
        if (source == null) {
            return null;
        }
        createEngineInstanceIfNeeded();
        Message message = null;
        try {
            message = new PEpMessageBuilder(source).createMessage(context);
            message.setDir(Message.Direction.Outgoing);
            Timber.d(TAG, "encryptMessage() before encrypt to self");
            Identity from = message.getFrom();
            from.user_id = PEP_OWN_USER_ID;
            from.me = true;
            message.setFrom(from);
            Message currentEnc = engine.encrypt_message_for_self(message.getFrom(), message, convertExtraKeys(keys));
            if (currentEnc == null) currentEnc = message;
            Timber.d(TAG, "encryptMessage() after encrypt to self");
            return getMimeMessage(source, currentEnc);
        } catch (Exception e) {
            Timber.e(e, "%s %s", TAG, "encryptMessageToSelf: ");
            return source;
        } finally {
            if (message != null) {
                message.close();
            }
        }
    }

    private List<MimeMessage> getUnencryptedCopies(MimeMessage source, String[] extraKeys) throws MessagingException, pEpException {
        List<MimeMessage> messages = new ArrayList<>();
        messages.add(getUnencryptedBCCCopy(source));
        messages.add(getEncryptedCopy(source, getUnencryptedCopyWithoutBCC(source), extraKeys));
        return messages;

    }

    private Message getUnencryptedCopyWithoutBCC(MimeMessage source) {
        Message message = stripEncryptedRecipients(source);
        message.setBcc(null);
        return message;
    }

    private MimeMessage getUnencryptedBCCCopy(MimeMessage source) throws MessagingException {
        Message message = stripEncryptedRecipients(source);
        message.setTo(null);
        message.setCc(null);
        MimeMessage result = getMimeMessage(source, message);
        message.close();
        return result;
    }

    @NonNull
    private List<MimeMessage> encryptMessages(MimeMessage source, String[] extraKeys, List<Message> messagesToEncrypt) throws pEpException, MessagingException {
        List<MimeMessage> messages = new ArrayList<>();
        for (Message message : messagesToEncrypt) {
            messages.add(getEncryptedCopy(source, message, extraKeys));
        }
        return messages;
    }

    private List<MimeMessage> getEncryptedCopies(MimeMessage source, String[] extraKeys) throws pEpException, MessagingException {
        List<Message> messagesToEncrypt = new ArrayList<>();
        Message toEncryptMessage = stripUnencryptedRecipients(source);
        messagesToEncrypt.add(toEncryptMessage);

        if (toEncryptMessage.getBcc() != null) {
            handleEncryptedBCC(source, toEncryptMessage, messagesToEncrypt);
        }

        List<MimeMessage> result = new ArrayList<>(encryptMessages(source, extraKeys, messagesToEncrypt));

        for (Message message : messagesToEncrypt) {
            message.close();
        }
        return result;
    }

    private Message stripUnencryptedRecipients(MimeMessage source) {
        return stripRecipients(source, false);
    }

    private Message stripEncryptedRecipients(MimeMessage source) {
        return stripRecipients(source, true);
    }

    private void handleEncryptedBCC(MimeMessage source, Message pEpMessage, List<Message> outgoingMessageList) {
        for (Identity identity : pEpMessage.getBcc()) {
            Message message = new PEpMessageBuilder(source).createMessage(context);
            message.setTo(null);
            message.setCc(null);
            Vector<Identity> oneBCCList = new Vector<>();
            oneBCCList.add(identity);
            message.setBcc(oneBCCList);
            outgoingMessageList.add(message);
        }
        pEpMessage.setBcc(null);
        if (pEpMessage.getTo() == null
                && pEpMessage.getCc() == null
                && pEpMessage.getBcc() == null) {
            outgoingMessageList.remove(ENCRYPTED_MESSAGE_POSITION);
        }
    }

    private MimeMessage getEncryptedCopy(MimeMessage source, Message message, String[] extraKeys) throws pEpException, MessagingException, AppDidntEncryptMessageException {
        message.setDir(Message.Direction.Outgoing);
        Timber.d(TAG, "encryptMessage() before encrypt");
        Identity from = message.getFrom();
        from.user_id = PEP_OWN_USER_ID;
        from.me = true;
        message.setFrom(from);
        Message currentEnc = engine.encrypt_message(message, convertExtraKeys(extraKeys), message.getEncFormat());
        source.setFlag(Flag.X_PEP_WASNT_ENCRYPTED, source.isSet(Flag.X_PEP_SHOWN_ENCRYPTED) && currentEnc == null);
        if (currentEnc == null) {
            if (source.isSet(Flag.X_PEP_SHOWN_ENCRYPTED)) {
                throw new AppDidntEncryptMessageException(source);
            }
            currentEnc = message;
        }
        Timber.d(TAG, "encryptMessage() after encrypt");
        return getMimeMessage(source, currentEnc);
    }

    private Message stripRecipients(MimeMessage src, boolean encrypted) {
        Message message = new PEpMessageBuilder(src).createMessage(context);
        message.setTo(removeRecipients(message.getTo(), encrypted));
        message.setCc(removeRecipients(message.getCc(), encrypted));
        message.setBcc(removeRecipients(message.getBcc(), encrypted));
        return message;
    }


    private Vector<Identity> removeRecipients(Vector<Identity> recipientList, boolean deletingEncrypted) {
        if (recipientList != null) {
            for (Iterator<Identity> iterator = recipientList.iterator(); iterator.hasNext(); ) {
                Identity identity = iterator.next();
                if (deletingEncrypted && isEncrypted(identity)
                        || !deletingEncrypted && !isEncrypted(identity)) {
                    iterator.remove();
                }
            }
        }

        return recipientList;
    }

    private boolean isEncrypted(Identity identity) {
        return getRating(identity).value > Rating.pEpRatingUnencrypted.value;
    }

    private Vector<String> convertExtraKeys(String[] extraKeys) {
        if (extraKeys == null || extraKeys.length == 0) return null;
        Vector<String> rv = new Vector<>();
        Collections.addAll(rv, extraKeys);
        return rv;
    }


    @Override
    public synchronized Rating getRating(Address address) {
        Identity ident = PEpUtils.createIdentity(address, context);
        return getRating(ident);
    }

    @Override
    public synchronized Rating getRating(Identity ident) {
        createEngineInstanceIfNeeded();
        try {
            return engine.identity_rating(ident);
        } catch (pEpException e) {
            Timber.e(e, "%s %s", TAG, "getRating: ");
            return Rating.pEpRatingUndefined;
        }
    }

    @Override
    public void getRating(final Identity identity, final ResultCallback<Rating> callback) {
        threadExecutor.execute(() -> {
            Engine engine1 = null;
            try {
                engine1 = new Engine();
                Rating rating = engine1.identity_rating(identity);
                notifyLoaded(rating, callback);
            } catch (Exception e) {
                notifyError(e, callback);
            } finally {
                if (engine1 != null) {
                    engine1.close();
                }
            }
        });
    }

    @Override
    public void getRating(Address address, ResultCallback<Rating> callback) {
        Identity id = PEpUtils.createIdentity(address, context);
        getRating(id, callback);
    }

    @Override
    public synchronized String trustwords(Identity id, String language) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized String trustwords(Identity myself, Identity partner, String lang, boolean isShort) {
        try {
            return engine.get_trustwords(myself, partner, lang, !isShort);
        } catch (pEpException e) {
            Timber.e(e, "%s %s", TAG, "trustwords: ");
            return null;
        }
    }

    @Override
    public void obtainTrustwords(Identity self, Identity other, String lang,
                                 Boolean areKeysyncTrustwords,
                                 ResultCallback<HandshakeData> callback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                Identity myself;
                Identity another;
                if (!areKeysyncTrustwords) {
                    self.user_id = PEP_OWN_USER_ID;
                    self.me = true;
                    myself = engine.myself(self);
                    another = engine.updateIdentity(other);
                } else {
                    myself = self;
                    another = other;
                }
                String longTrustwords = engine.get_trustwords(myself, another, lang, true);
                String shortTrustwords = engine.get_trustwords(myself, another, lang, false);
                notifyLoaded(new HandshakeData(longTrustwords, shortTrustwords, myself, another), callback);
            } catch (Exception e) {
                notifyError(e, callback);
            } finally {
                if (engine != null) {
                    engine.close();
                }
            }
        });
    }

    @Override
    public synchronized void close() {
        if (engine != null) {
            //engine.stopSync();
            engine.close();
        }
    }

    @Override
    public synchronized Identity updateIdentity(Identity id) {
        createEngineInstanceIfNeeded();
        return engine.updateIdentity(id);
    }

    @Override
    public synchronized void trustPersonaKey(Identity id) {
        createEngineInstanceIfNeeded();
        Timber.i(TAG, "Calling trust personal key");
        engine.trustPersonalKey(id);
    }

    public synchronized void trustOwnKey(Identity id) {
        createEngineInstanceIfNeeded();
        Timber.i(TAG, "Calling trust own key");
        engine.trustOwnKey(id);
    }

    @Override
    public synchronized void keyMistrusted(Identity id) {
        createEngineInstanceIfNeeded();
        engine.keyMistrusted(id);
    }

    @Override
    public synchronized void resetTrust(Identity id) {
        createEngineInstanceIfNeeded();
        engine.keyResetTrust(id);
    }

    @Override
    public void loadMessageRatingAfterResetTrust(MimeMessage mimeMessage, boolean isIncoming, Identity id, ResultCallback resultCallback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                engine.keyResetTrust(id);
                Message pEpMessage = new PEpMessageBuilder(mimeMessage).createMessage(context);
                Rating rating;
                if (isIncoming) {
                    pEpMessage.setDir(Message.Direction.Incoming);
                    rating = engine.re_evaluate_message_rating(pEpMessage);
                } else {
                    pEpMessage.setDir(Message.Direction.Outgoing);
                    rating = engine.outgoing_message_rating(pEpMessage);
                }
                notifyLoaded(rating, resultCallback);
            } catch (pEpException e) {
                notifyError(e, resultCallback);
            } finally {
                if (engine != null) {
                    engine.close();
                }
            }
        });
    }

    @Override
    public synchronized String getLog() {
        return engine.getCrashdumpLog(100);
    }

    @Override
    public synchronized void printLog() {
        String[] logLines = getLog().split("\n");
        for (String logLine : logLines) {
            Timber.i(TAG, logLine);
        }
    }

    @Override
    public synchronized Identity myself(Identity myId) {
        createEngineInstanceIfNeeded();
        myId.user_id = PEP_OWN_USER_ID;
        myId.me = true;
        try {
            return engine.myself(myId);
        }
        catch( pEpCannotCreateKey exception) {
            Timber.e(exception, "%s %s", TAG, "could not create key in PEpProviderImpl.myself");
            return myId;
        }
    }

    @Override
    public synchronized Identity setOwnIdentity(Identity id, String fpr)  {
        createEngineInstanceIfNeeded();
        try {
            String sanitizedFpr = PEpUtils.sanitizeFpr(fpr);
            return engine.setOwnKey(id, sanitizedFpr);
        } catch (Exception e) {
            //TODO: Make pEpException a runtime one, and filter here
            return null;
        }
    }

    @Override
    public synchronized void setPassiveModeEnabled(boolean enable) {
        createEngineInstanceIfNeeded();
        engine.config_passive_mode(enable);
    }

    @Override
    public synchronized void startKeyserverLookup() {
        createEngineInstanceIfNeeded();
        engine.startKeyserverLookup();
    }

    @Override
    public synchronized void stopKeyserverLookup() {
        createEngineInstanceIfNeeded();
        engine.stopKeyserverLookup();
    }

    /**
     * @deprecated private key detection is not supported anymore, alternatives are pEp sync and import from FS
     */
    @Override
    @Deprecated
    public synchronized KeyDetail getOwnKeyDetails(Message message) {
        Identity id;
        try {
            id = engine.own_message_private_key_details(message);
            return  new KeyDetail(id.fpr, new Address(id.address, id.username));
        } catch (Exception e) {
            Timber.e(e, "%s %s", TAG, "getOwnKeyDetails: ");
        }
        return null;
    }

    private synchronized void createEngineInstanceIfNeeded() {
        if (engine == null) {
            try {
                createEngineSession();
            } catch (pEpException e) {
                Timber.e(e, "%s %s", TAG, "createIfNeeded " + Thread.currentThread().getId());
            }
        } else {
            Timber.d(TAG, "createIfNeeded " + Thread.currentThread().getId());
        }
    }

//    private String buildImportDialogText(Context context, Identity id, String fromAddress) {
//        StringBuilder stringBuilder = new StringBuilder();
//        String formattedFpr = PEpUtils.formatFpr(id.fpr);
//        stringBuilder.append(context.getString(R.string.pep_receivedSecretKey))
//                .append("\n")
//                .append(context.getString(R.string.pep_username)).append(": ")
//                .append(id.username).append("\n")
//                .append(context.getString(R.string.pep_userAddress)).append(": ")
//                .append(id.address).append("\n")
//                .append("\n")
//                .append(formattedFpr.substring(0, formattedFpr.length()/2))
//                .append("\n")
//                .append(formattedFpr.substring(formattedFpr.length()/2))
//                .append("\n").append("\n")
//                .append(context.getString(R.string.recipient_from)).append(": ")
//                .append(fromAddress);
//
//        return stringBuilder.toString();
//    }

    @Override
    public synchronized void setSubjectProtection(boolean isProtected) {
        createEngineInstanceIfNeeded();
        engine.config_unencrypted_subject(!isProtected);
    }

    @Override
    public synchronized void configPassphrase(String passphrase) {
        createEngineInstanceIfNeeded();
        engine.config_passphrase(passphrase);
    }

    @Override
    public synchronized List<KeyListItem> getBlacklistInfo() {
        try {
            List<KeyListItem> identites = new ArrayList<>();
            ArrayList<Pair<String, String>> keys = engine.OpenPGP_list_keyinfo("");
            if (keys != null) {
                for (Pair<String, String> key : keys) {
                    identites.add(
                            new KeyListItem(key.first, key.second,
                                    engine.blacklist_is_listed(key.first)));
                }
            }
            return identites;
        } catch (pEpException e) {
            Timber.e(e, "%s %s", TAG, "getBlacklistInfo");
        }

        return null;

    }

    @Override
    public synchronized List<KeyListItem> getMasterKeysInfo() {
        try {
            List<KeyListItem> identites = new ArrayList<>();
            ArrayList<Pair<String, String>> keys = engine.OpenPGP_list_keyinfo("");
            if (keys != null) {
                for (Pair<String, String> key : keys) {
                    identites.add(
                            new KeyListItem(key.first, key.second));
                }
            }
            return identites;
        } catch (pEpException e) {
            Timber.e(e, "%s %s", TAG, "getBlacklistInfo");
        }

        return null;

    }

    @Override
    public synchronized void addToBlacklist(String fpr) {
        engine.blacklist_add(fpr);
    }

    @Override
    public synchronized void deleteFromBlacklist(String fpr) {
        engine.blacklist_delete(fpr);
    }

    public static com.fsck.k9.mail.Message getMimeMessage(Message message) {
        try {
            return getMimeMessage(null, message);
        } catch (MessagingException e) {
            Timber.e(e, "%s %s", TAG, "getMimeMessage: ");
        }
        return null;
    }

    private final boolean sendMessageSet = false;
    private final boolean showHandshakeSet = false;


    @Override
    public synchronized void setSyncSendMessageCallback(Sync.MessageToSendCallback callback) {
        engine.setMessageToSendCallback(callback);
    }

    private boolean areCallbackSet() {
        return sendMessageSet && showHandshakeSet;
    }

    @Override
    public synchronized void setSyncHandshakeCallback(Sync.NotifyHandshakeCallback activity) {
        engine.setNotifyHandshakeCallback(activity);
    }

    @Override
    public synchronized void startSync() {
        try {
            Timber.i("%s %s", TAG, "Trying to start sync thread Engine.startSync()");
            engine.startSync();
        } catch (pEpException exception) {
            Timber.e("%s %s", TAG, "Could not Engine.startSync()", exception);
        }
    }

    //FIXME: Implement sync use lists.
    @Override
    public synchronized void acceptSync() {
        engine.deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeAccepted, new Vector<>());
    }

    @Override
    public synchronized void rejectSync() {
        engine.deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeRejected, new Vector<>());
    }

    @Override
    public synchronized void cancelSync() {
        engine.deliverHandshakeResult(SyncHandshakeResult.SyncHandshakeCancel, new Vector<>());
    }

    @Override
    public void loadOwnIdentities(ResultCallback<List<Identity>> callback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                List<Identity> identitiesVector = engine.own_identities_retrieve();
                notifyLoaded(identitiesVector, callback);
            } catch (pEpException error) {
                notifyError(error, callback);
            } finally {
                if (engine != null) {
                    engine.close();
                }
            }
        });
    }

    @Override
    public void setIdentityFlag(Identity identity, Integer flags, CompletedCallback completedCallback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                engine.set_identity_flags(identity, flags);
                notifyCompleted(completedCallback);
            } catch (pEpException e) {
                notifyError(e, completedCallback);
            } finally {
                if (engine != null) {
                    engine.close();
                }
            }
        });
    }

    @Override
    public void unsetIdentityFlag(Identity identity, Integer flags, CompletedCallback completedCallback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                engine.unset_identity_flags(identity, flags);
                notifyCompleted(completedCallback);
            } catch (pEpException e) {
                notifyError(e, completedCallback);
            } finally {
                if (engine != null) {
                    engine.close();
                }
            }
        });
    }

    @Override
    public void setIdentityFlag(Identity identity, boolean sync) {
        try {
            if (sync) {
                engine.enable_identity_for_sync(identity);
            } else {
                engine.disable_identity_for_sync(identity);
            }
        } catch (pEpException e) {
            Timber.e(e, "%s %s", TAG, "setIdentityFlag: ");
        }
    }

    @Override
    public void unsetIdentityFlag(Identity identity, Integer flags) {
        try {
            engine.unset_identity_flags(identity, flags);
        } catch (pEpException e) {
            Timber.e(e, "%s %s", TAG, "setIdentityFlag: ");
        }
    }

    @Override
    public void setFastPollingCallback(Sync.NeedsFastPollCallback needsFastPollCallback) {
        engine.setNeedsFastPollCallback(needsFastPollCallback);
    }

    private void notifyLoaded(final Object privacyState, final ResultCallback callback) {
        this.postExecutionThread.post(() -> callback.onLoaded(privacyState));
    }

    private void notifyCompleted(CompletedCallback completedCallback) {
        this.postExecutionThread.post(completedCallback::onComplete);
    }

    private void notifyError(final Throwable throwable, final Callback callback) {
        this.postExecutionThread.post(() -> callback.onError(throwable));
    }

    @Override
    public Rating incomingMessageRating(MimeMessage message) {
        Message pEpMessage = new PEpMessageBuilder(message).createMessage(context);
        try {
            return engine.re_evaluate_message_rating(pEpMessage);
        } catch (pEpException e) {
            Timber.e(e);
            return Rating.pEpRatingUndefined;
        }
    }

    @Override
    public void loadOutgoingMessageRatingAfterResetTrust(Identity identity, Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses, ResultCallback<Rating> callback) {
        getRating(identity, from, toAddresses, ccAddresses, bccAddresses, callback);
    }

    private void getRating(@Nullable Identity identity, Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses, ResultCallback<Rating> callback) {
        Timber.i("Contador de PEpProviderImpl+1");
        EspressoTestingIdlingResource.increment();
        threadExecutor.execute(() -> {
            if (bccAddresses.size()  > 0) {
                notifyLoaded(Rating.pEpRatingUnencrypted, callback);
                return;
            }
            Message testee = null;
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                if (identity != null) {
                    engine.keyResetTrust(identity);
                }
                int recipientsSize = toAddresses.size() + ccAddresses.size() + bccAddresses.size();
                if (from == null || recipientsSize == 0)
                    notifyLoaded(Rating.pEpRatingUndefined, callback);

                testee = new Message();

                Identity idFrom = PEpUtils.createIdentity(from, context);
                idFrom.user_id = PEP_OWN_USER_ID;
                idFrom.me = true;
                testee.setFrom(idFrom);
                testee.setTo(PEpUtils.createIdentities(toAddresses, context));
                testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
                testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
                testee.setShortmsg("hello, world");     // FIXME: do I need them?
                testee.setLongmsg("Lorem ipsum");
                testee.setDir(Message.Direction.Outgoing);

                Rating result = engine.outgoing_message_rating(testee);   // stupid way to be able to patch the value in debugger
                Timber.i(TAG, "getRating " + result.name());

                notifyLoaded(result, callback);
            } catch (Throwable e) {
                Timber.e(e, "%s %s", TAG, "during color test:");
                notifyError(e, callback);
            } finally {
                Timber.i("Contador de PEpProviderImpl  -1");
                EspressoTestingIdlingResource.decrement();
                if (testee != null) testee.close();
                if (engine != null) {
                    engine.close();
                }
            }
        });
    }

    @Override
    public Map<String, PEpLanguage> obtainLanguages() {
        try {
            Map<String, PEpLanguage> languages = new HashMap<>();
            String languageList = engine.get_languagelist();
            String[] lanchageCharacters = languageList.split("\n");
            for (String lanchageCharacter : lanchageCharacters) {
                String[] split = lanchageCharacter.split(",");
                PEpLanguage pEpLanguage = new PEpLanguage(getElementAtPosition(split[0]), getElementAtPosition(split[1]), getElementAtPosition(split[2]));
                languages.put(getElementAtPosition(split[0]), pEpLanguage);
            }
            return languages;
        } catch (pEpException e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public com.fsck.k9.mail.Message generatePrivateKeyMessage(MimeMessage message, String fpr) {
        createEngineInstanceIfNeeded();
        try {
            Message containerMsg = new PEpMessageBuilder(message).createMessage(context);
            containerMsg.setDir(Message.Direction.Outgoing);
            return getMimeMessage(engine.encrypt_message_and_add_priv_key(containerMsg, fpr));
        } catch (pEpException e) {
            e.printStackTrace();
            Timber.e(e, "%s %s", TAG, "generatePrivateKeyMessage: ");
        }

        return null;
    }

    @Override
    public Message encryptMessage(Message result) throws pEpException {
        createEngineInstanceIfNeeded();
        return engine.encrypt_message(result, null, result.getEncFormat());
    }

    @Override
    public boolean canEncrypt(String address) {
        createEngineInstanceIfNeeded();

        Message msg = new Message();
        Identity id = myself(PEpUtils.createIdentity(new Address(address), context));

        msg.setFrom(id);

        Vector<Identity> to = new Vector<>();
        to.add(id);
        msg.setTo(to);

        msg.setShortmsg("hello, world");
        msg.setLongmsg("this is a test");

        msg.setDir(Message.Direction.Outgoing);


        try {
            engine.encrypt_message(msg, null, Message.EncFormat.PEP);
        } catch (pEpException e) {
            Timber.e(e);
            return false;
        }

        return true;
    }

    @Override
    public void importKey(byte[] key) {
        createEngineInstanceIfNeeded();
        engine.importKey(key);
    }

    @Override
    public void keyResetIdentity(Identity ident, String fpr) {
        createEngineInstanceIfNeeded();
        ident = updateIdentity(ident);
        try {
            engine.key_reset_identity(ident, fpr);
        } catch (pEpPassphraseRequired | pEpWrongPassphrase e) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetIdentity:");
        }
    }

    @Override
    public void keyResetUser(String userId, String fpr) {
        createEngineInstanceIfNeeded();
        try {
            engine.key_reset_user(userId, fpr);
        } catch (pEpPassphraseRequired | pEpWrongPassphrase e) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetUser:");
        }
    }

    @Override
    public void keyResetAllOwnKeys() {
        createEngineInstanceIfNeeded();
        try {
            engine.key_reset_all_own_keys();
        } catch (pEpPassphraseRequired | pEpWrongPassphrase e) {
            Timber.e(e, "%s %s", TAG, "passphrase issue during keyResetAllOwnKeys:");
        }
    }

    @Override
    public void leaveDeviceGroup() {
        createEngineInstanceIfNeeded();
        engine.leave_device_group();
    }

    @Override
    public void stopSync() {
        Timber.d("%s %s", TAG, "stopSync");

        createEngineInstanceIfNeeded();
        engine.stopSync();
    }

    @Override
    public boolean isSyncRunning() {
        createEngineInstanceIfNeeded();
        return engine.isSyncRunning();
    }


    private String getElementAtPosition(String chain) {
        return chain.substring(1, chain.length() - 1);
    }
}