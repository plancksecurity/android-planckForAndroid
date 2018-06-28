package com.fsck.k9.pEp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.pEp.infrastructure.exceptions.AppCannotDecryptException;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import org.pEp.jniadapter.DecryptFlags;
import org.pEp.jniadapter.Engine;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;
import org.pEp.jniadapter.Pair;
import org.pEp.jniadapter.Rating;
import org.pEp.jniadapter.Sync;
import org.pEp.jniadapter.pEpException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * pep provider implementation. Dietz is the culprit.
 */
public class PEpProviderImpl implements PEpProvider {
    private static final String TAG = "pEp";
    private final ThreadExecutor threadExecutor;
    private final PostExecutionThread postExecutionThread;
    private Context context;
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
            Log.e(TAG, "during getRating:", e);
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
        engine.config_unencrypted_subject(K9.ispEpSubjectUnprotected());
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
            testee.setFrom(idFrom);
            testee.setTo(PEpUtils.createIdentities(toAddresses, context));
            testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
            testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
            testee.setShortmsg("hello, world");     // FIXME: do I need them?
            testee.setLongmsg("Lorem ipsum");
            testee.setDir(Message.Direction.Outgoing);

            Rating result = engine.outgoing_message_rating(testee);   // stupid way to be able to patch the value in debugger
            Log.i(TAG, "getRating " + result.name());

            return result;
        } catch (Throwable e) {
            Log.e(TAG, "during color test:", e);
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
        Log.d(TAG, "decryptMessage() enter");
        Message srcMsg = null;
        Engine.decrypt_message_Return decReturn = null;
        try {
            if (engine == null) createEngineSession();

            srcMsg = new PEpMessageBuilder(source).createMessage(context);
            srcMsg.setDir(Message.Direction.Incoming);

            Log.d(TAG, "decryptMessage() before decrypt");
            if ( srcMsg.getOptFields() != null) {
                for (Pair<String, String> stringStringPair : srcMsg.getOptFields()) {
                    Log.d(TAG, "decryptMessage() after decrypt " + stringStringPair.first + ": " + stringStringPair.second);
                }
            }
            decReturn = engine.decrypt_message(srcMsg, 0);
            Log.d(TAG, "decryptMessage() after decrypt");
            Message message = decReturn.dst;
            MimeMessage decMsg = getMimeMessage(source, message);
            boolean neverUnprotected = decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE).length > 0
                    && decMsg.getHeader(MimeHeader.HEADER_PEP_ALWAYS_SECURE)[0].equals(PEP_ALWAYS_SECURE_TRUE);
            decMsg.setFlag(Flag.X_PEP_NEVER_UNSECURE, neverUnprotected);
            if (isUsablePrivateKey(decReturn)) {
                return new DecryptResult(decMsg, decReturn.rating, new KeyDetail("", null), decReturn.flags);
            }
           else return new DecryptResult(decMsg, decReturn.rating, null, -1);
//        } catch (pEpMessageConsume | pEpMessageIgnore pe) {
//            // TODO: 15/11/16 deal with it as flag not exception
//            //  throw pe;
//            return null;
        }catch (Throwable t) {
            Log.e(TAG, "while decrypting message: "  + source.getSubject()
                    + "\n" + source.getFrom()[0]
                    + "\n" + source.getSentDate().toString()
                    + "\n" + source.getMessageId(),
                    t);
            throw new AppCannotDecryptException("Could not decrypt", t);
        } finally {
            if (srcMsg != null) srcMsg.close();
            if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
            Log.d(TAG, "decryptMessage() exit");
        }
    }

    @Override
    public void decryptMessage(MimeMessage source, ResultCallback<DecryptResult> callback) {
        threadExecutor.execute(() -> {
            Log.d(TAG, "decryptMessage() enter");
            Message srcMsg = null;
            Engine.decrypt_message_Return decReturn = null;
            Engine engine = null;
            try {
                engine = getNewEngineSession();

                srcMsg = new PEpMessageBuilder(source).createMessage(context);
                srcMsg.setDir(Message.Direction.Incoming);

                Log.d(TAG, "decryptMessage() before decrypt");
                if ( srcMsg.getOptFields() != null) {
                    for (Pair<String, String> stringStringPair : srcMsg.getOptFields()) {
                        Log.d(TAG, "decryptMessage() after decrypt " + stringStringPair.first + ": " + stringStringPair.second);
                    }
                }
                decReturn = engine.decrypt_message(srcMsg, 0);
                Log.d(TAG, "decryptMessage() after decrypt");

                Message message = decReturn.dst;
                MimeMessage decMsg = getMimeMessage(source, message);

                if (isUsablePrivateKey(decReturn)) {
                    notifyLoaded(new DecryptResult(decMsg, decReturn.rating, getOwnKeyDetails(srcMsg), -1), callback);
                }
                else notifyLoaded(new DecryptResult(decMsg, decReturn.rating, null, decReturn.flags), callback);
//        } catch (pEpMessageConsume | pEpMessageIgnore pe) {
//            // TODO: 15/11/16 deal with it as flag not exception

//            //  throw pe;
//            return null;
            }catch (Throwable t) {
                Log.e(TAG, "while decrypting message:", t);
                notifyError(new AppCannotDecryptException("Could not decrypt", t), callback);
            } finally {
                if (srcMsg != null) srcMsg.close();
                if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
                if (engine != null) {
                    engine.close();
                }
                Log.d(TAG, "decryptMessage() exit");
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

        Boolean isRequestedFromPEpMessage = source == null;
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
                && result.flags != -1
                && result.flags == 0x01;
    }

    @Override
    public synchronized List<MimeMessage> encryptMessage(MimeMessage source, String[] extraKeys) {
        // TODO: 06/12/16 add unencrypted for some
        Log.d(TAG, "encryptMessage() enter");
        List<MimeMessage> resultMessages = new ArrayList<>();
        Message message = new PEpMessageBuilder(source).createMessage(context);
        try {
            if (engine == null) createEngineSession();
            resultMessages.add(getEncryptedCopy(source, message, extraKeys));
            return resultMessages;
        } catch (Throwable t) {
            Log.e(TAG, "while encrypting message:", t);
            throw new RuntimeException("Could not encrypt", t);
        } finally {
            Log.d(TAG, "encryptMessage() exit");
        }
    }

    @Override
    public synchronized MimeMessage encryptMessageToSelf(MimeMessage source, String[] keys) throws MessagingException{
        if (source == null) {
            return source;
        }
        createEngineInstanceIfNeeded();
        Message message = null;
        try {
            message = new PEpMessageBuilder(source).createMessage(context);
            message.setDir(Message.Direction.Outgoing);
            Log.d(TAG, "encryptMessage() before encrypt to self");
            Identity from = message.getFrom();
            from.user_id = PEP_OWN_USER_ID;
            message.setFrom(from);
            Message currentEnc = engine.encrypt_message_for_self(message.getFrom(), message, convertExtraKeys(keys));
            if (currentEnc == null) currentEnc = message;
            Log.d(TAG, "encryptMessage() after encrypt to self");
            return getMimeMessage(source, currentEnc);
        } catch (Exception exception) {
            Log.e(TAG, "encryptMessageToSelf: ", exception);
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
        List<MimeMessage> result = new ArrayList<>();
        List<Message> messagesToEncrypt = new ArrayList<>();
        Message toEncryptMessage = stripUnencryptedRecipients(source);
        messagesToEncrypt.add(toEncryptMessage);

        if (toEncryptMessage.getBcc() != null) {
            handleEncryptedBCC(source, toEncryptMessage, messagesToEncrypt);
        }

        result.addAll(encryptMessages(source, extraKeys, messagesToEncrypt));

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

    private MimeMessage getEncryptedCopy(MimeMessage source, Message message, String[] extraKeys) throws pEpException, MessagingException {
        message.setDir(Message.Direction.Outgoing);
        Log.d(TAG, "encryptMessage() before encrypt");
        Identity from = message.getFrom();
        from.user_id = PEP_OWN_USER_ID;
        message.setFrom(from);
        Message currentEnc = engine.encrypt_message(message, convertExtraKeys(extraKeys), message.getEncFormat());
        if (currentEnc == null) currentEnc = message;
        Log.d(TAG, "encryptMessage() after encrypt");
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
            Rating result =  engine.identity_rating(ident);
            return result;
        } catch (pEpException e) {
            Log.e(TAG, "getRating: ", e);
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
            Log.e(TAG, "trustwords: ");
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
        if (engine != null) engine.close();
    }

    @Override
    public synchronized Identity updateIdentity(Identity id) {
        createEngineInstanceIfNeeded();
        return engine.updateIdentity(id);
    }

    @Override
    public synchronized void trustPersonaKey(Identity id) {
        createEngineInstanceIfNeeded();
        engine.trustPersonalKey(id);
    }

    @Override
    public synchronized void keyCompromised(Identity id) {
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
        String logLines[] = getLog().split("\n");
        for (String logLine : logLines) {
            Log.i("PEPJNI", logLine);
        }
    }

    @Override
    public synchronized Identity myself(Identity myId) {
        createEngineInstanceIfNeeded();
        myId.user_id = PEP_OWN_USER_ID;
        return engine.myself(myId);
    }

    @Override
    public synchronized void setOwnIdentity(Identity id, String fpr) {
        createEngineInstanceIfNeeded();
        engine.setOwnKey(id, fpr);
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

    @Override
    public synchronized KeyDetail getOwnKeyDetails(Message message) {
        Identity id;
        try {
            id = engine.own_message_private_key_details(message);
            return  new KeyDetail(id.fpr, new Address(id.address, id.username));
        } catch (Exception e) {
            Log.e(TAG, "getOwnKeyDetails: ", e);
        }
        return null;
    }

    private synchronized void createEngineInstanceIfNeeded() {
        if (engine == null) {
            try {
                createEngineSession();
            } catch (pEpException e) {
                Log.e(TAG, "createIfNeeded " + Thread.currentThread().getId());
            }
        } else {
            Log.d(TAG, "createIfNeeded " + Thread.currentThread().getId());
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
    public synchronized void setSubjectUnprotected (boolean isUnprotected) {
        createEngineInstanceIfNeeded();
        engine.config_unencrypted_subject(isUnprotected);
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
            Log.e(TAG, "getBlacklistInfo", e);
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
            Log.e(TAG, "getBlacklistInfo", e);
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
            Log.e(TAG, "getMimeMessage: ", e);
        }
        return null;
    }

    boolean sendMessageSet = false;
    boolean showHandshakeSet = false;
    boolean keysyncStarted = false;


    @Override
    public synchronized void setSyncSendMessageCallback(Sync.MessageToSendCallback callback) {
        engine.setMessageToSendCallback(callback);
    }

    private boolean areCallbackSet() {
        return sendMessageSet && showHandshakeSet;
    }

    @Override
    public synchronized void setSyncHandshakeCallback(Sync.notifyHandshakeCallback activity) {
        engine.setnotifyHandshakeCallback(activity);
    }

    @Override
    public synchronized void startSync() {
        engine.startSync();
    }

    @Override
    public synchronized void acceptHandshake(Identity identity) {
        engine.accept_sync_handshake(identity);
    }

    @Override
    public synchronized void rejectHandshake(Identity identity) {
        engine.reject_sync_handshake(identity);
    }

    @Override
    public synchronized void cancelHandshake(Identity identity) {
        engine.cancel_sync_handshake(identity);
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
    public void setIdentityFlag(Identity identity, Integer flags) {
        try {
            engine.set_identity_flags(identity, flags);
        } catch (pEpException e) {
            Log.e(TAG, "setIdentityFlag: ", e);
        }
    }

    @Override
    public void unsetIdentityFlag(Identity identity, Integer flags) {
        try {
            engine.unset_identity_flags(identity, flags);
        } catch (pEpException e) {
            Log.e(TAG, "setIdentityFlag: ", e);
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
                testee.setFrom(idFrom);
                testee.setTo(PEpUtils.createIdentities(toAddresses, context));
                testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
                testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
                testee.setShortmsg("hello, world");     // FIXME: do I need them?
                testee.setLongmsg("Lorem ipsum");
                testee.setDir(Message.Direction.Outgoing);

                Rating result = engine.outgoing_message_rating(testee);   // stupid way to be able to patch the value in debugger
                Log.i(TAG, "getRating " + result.name());

                notifyLoaded(result, callback);
            } catch (Throwable e) {
                Log.e(TAG, "during color test:", e);
                notifyError(e, callback);
            } finally {
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
            Log.e(TAG, "generatePrivateKeyMessage: ", e);
        }

        return null;
    }

    @Override
    public Message encryptMessage(Message result) {

        try {
            return engine.encrypt_message(result, null, result.getEncFormat());
        } catch (pEpException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getElementAtPosition(String chain) {
        return chain.substring(1, chain.length() - 1);
    }
}