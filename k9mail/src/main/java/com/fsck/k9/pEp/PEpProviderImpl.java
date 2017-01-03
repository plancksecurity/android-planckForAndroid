package com.fsck.k9.pEp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.pEp.infrastructure.exceptions.AppCannotDecryptException;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import org.pEp.jniadapter.AndroidHelper;
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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.inject.Inject;

/**
 * pep provider implementation. Dietz is the culprit.
 */
public class PEpProviderImpl implements PEpProvider {
    private static final String TAG = "pEp";
    private static boolean pEpInitialized = false;
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

    public synchronized void setup(Context c) {
        if (!pEpInitialized) {
            AndroidHelper.setup(c);
            pEpInitialized = true;
        }

        context = c;
        createEngineInstanceIfNeeded();

    }

    @Override
    public Rating getPrivacyState(com.fsck.k9.mail.Message message) {
        Address from = message.getFrom()[0];                            // FIXME: From is an array?!
        List<Address> to = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO));
        List<Address> cc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC));
        List<Address> bcc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC));
        return getPrivacyState(from, to, cc, bcc);
    }

    @Override
    public void getPrivacyState(com.fsck.k9.mail.Message message, ResultCallback<Rating> callback) {
        threadExecutor.execute(() -> {
            Address from = message.getFrom()[0];                            // FIXME: From is an array?!
            List<Address> to = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO));
            List<Address> cc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC));
            List<Address> bcc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC));
            notifyLoaded(getPrivacyState(from, to, cc, bcc), callback);
        });
    }

    private Rating getPrivacyState(Message message) {
        try {
            if (engine == null) {
                createEngineSession();
            }
            return engine.outgoing_message_rating(message);
        } catch (pEpException e) {
            Log.e(TAG, "during getPrivacyState:", e);
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
    public synchronized Rating getPrivacyState(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses) {
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
            idFrom.me = true;
            idFrom.user_id = PEP_OWN_USER_ID;
            testee.setFrom(idFrom);
            testee.setTo(PEpUtils.createIdentities(toAddresses, context));
            testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
            testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
            testee.setShortmsg("hello, world");     // FIXME: do I need them?
            testee.setLongmsg("Lorem ipsum");
            testee.setDir(Message.Direction.Outgoing);

            Rating result = engine.outgoing_message_rating(testee);   // stupid way to be able to patch the value in debugger
            Log.i(TAG, "getPrivacyState " + result.name());

            return result;
        } catch (Throwable e) {
            Log.e(TAG, "during color test:", e);
        } finally {
            if (testee != null) testee.close();
        }

        return Rating.pEpRatingUndefined;
    }

    @Override
    public synchronized void getPrivacyState(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses, ResultCallback<Rating> callback) {
        threadExecutor.execute(() -> {
            if (bccAddresses.size()  > 0) {
                notifyLoaded(Rating.pEpRatingUnencrypted, callback);
                return;
            }
            Message testee = null;
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                int recipientsSize = toAddresses.size() + ccAddresses.size() + bccAddresses.size();
                if (from == null || recipientsSize == 0)
                    notifyLoaded(Rating.pEpRatingUndefined, callback);

                testee = new Message();

                Identity idFrom = PEpUtils.createIdentity(from, context);
                idFrom.me = true;
                idFrom.user_id = PEP_OWN_USER_ID;
                testee.setFrom(idFrom);
                testee.setTo(PEpUtils.createIdentities(toAddresses, context));
                testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
                testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
                testee.setShortmsg("hello, world");     // FIXME: do I need them?
                testee.setLongmsg("Lorem ipsum");
                testee.setDir(Message.Direction.Outgoing);

                Rating result = engine.outgoing_message_rating(testee);   // stupid way to be able to patch the value in debugger
                Log.i(TAG, "getPrivacyState " + result.name());

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

    private boolean isUnencryptedForSome(List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses) {
        for (Address toAddress : toAddresses) {
            if (identityRating(toAddress).value > Rating.pEpRatingUnencrypted.value) return true;
        }
        for (Address ccAddress : ccAddresses) {
            if (identityRating(ccAddress).value > Rating.pEpRatingUnencrypted.value) return true;
        }
        for (Address bccAddress : bccAddresses) {
            if (identityRating(bccAddress).value > Rating.pEpRatingUnencrypted.value) return true;
        }
        return false;
    }

    @Override
    public DecryptResult decryptMessage(MimeMessage source) {
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
            decReturn = engine.decrypt_message(srcMsg);
            Log.d(TAG, "decryptMessage() after decrypt");
            Message message = decReturn.dst;
            MimeMessage decMsg = getMimeMessage(source, message);

            if (isUsablePrivateKey(decReturn)) {
                return new DecryptResult(decMsg, decReturn.rating, getOwnKeyDetails(srcMsg), null);
            }
            else return new DecryptResult(decMsg, decReturn.rating, null, decReturn.flags);
//        } catch (pEpMessageConsume | pEpMessageIgnore pe) {
//            // TODO: 15/11/16 deal with it as flag not exception
//            //  throw pe;
//            return null;
        }catch (Throwable t) {
            Log.e(TAG, "while decrypting message: "  + source.getSubject()
                    + "\n" + source.getFrom()[0], t);
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
                decReturn = engine.decrypt_message(srcMsg);
                Log.d(TAG, "decryptMessage() after decrypt");

                Message message = decReturn.dst;
                MimeMessage decMsg = getMimeMessage(source, message);

                if (isUsablePrivateKey(decReturn)) {
                    notifyLoaded(new DecryptResult(decMsg, decReturn.rating, getOwnKeyDetails(srcMsg), null), callback);
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
                Log.d(TAG, "decryptMessage() exit");
            }
        });
    }

    @NonNull
    private MimeMessage getMimeMessage(MimeMessage source, Message message) throws MessagingException {
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

        return builder.parseMessage(message);
    }

    private boolean isUsablePrivateKey(Engine.decrypt_message_Return result) {
        // TODO: 13/06/16 Check if is necesary check own id
        return result.rating.value >= Rating.pEpRatingTrusted.value
                && result.flags != null
                && result.flags == DecryptFlags.pEpDecryptFlagOwnPrivateKey;
    }

    @Override
    public List<MimeMessage> encryptMessage(MimeMessage source, String[] extraKeys) {
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
    public void encryptMessage(MimeMessage source, String[] extraKeys, ResultCallback<List<MimeMessage>> callback) {
        // TODO: 06/12/16 add unencrypted for some
        Log.d(TAG, "encryptMessage() enter");
        List<MimeMessage> resultMessages = new ArrayList<>();
        Message message = new PEpMessageBuilder(source).createMessage(context);
        try {
            if (engine == null) createEngineSession();
            resultMessages.add(getEncryptedCopy(source, message, extraKeys));
            notifyLoaded(resultMessages, callback);
        } catch (Throwable t) {
            Log.e(TAG, "while encrypting message:", t);
            notifyError(new RuntimeException("Could not encrypt", t), callback);
        } finally {
            Log.d(TAG, "encryptMessage() exit");
        }
    }

    @Override
    public MimeMessage encryptMessageToSelf(MimeMessage source) throws MessagingException{
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
            Message currentEnc = engine.encrypt_message_for_self(message.getFrom(), message);
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

    @Override
    public void encryptMessageToSelf(MimeMessage source, ResultCallback<MimeMessage> callback){
        if (source == null) {
            notifyLoaded(source, callback);
        }
        Message message = null;
        try {
            message = new PEpMessageBuilder(source).createMessage(context);
            message.setDir(Message.Direction.Outgoing);
            Log.d(TAG, "encryptMessage() before encrypt to self");
            Identity from = message.getFrom();
            from.user_id = PEP_OWN_USER_ID;
            message.setFrom(from);
            Message currentEnc = engine.encrypt_message_for_self(message.getFrom(), message);
            if (currentEnc == null) currentEnc = message;
            Log.d(TAG, "encryptMessage() after encrypt to self");
            notifyLoaded(getMimeMessage(source, message), callback);
        } catch (Exception exception) {
            Log.e(TAG, "encryptMessageToSelf: ", exception);
            notifyLoaded(source, callback);
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
        Message currentEnc = engine.encrypt_message(message, convertExtraKeys(extraKeys));
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
        return identityRating(identity).value > Rating.pEpRatingUnencrypted.value;
    }

    private Vector<String> convertExtraKeys(String[] extraKeys) {
        if (extraKeys == null || extraKeys.length == 0) return null;
        Vector<String> rv = new Vector<>();
        Collections.addAll(rv, extraKeys);
        return rv;
    }


    @Override
    public Rating identityRating(Address address) {
        Identity ident = PEpUtils.createIdentity(address, context);
        return identityRating(ident);
    }

    @Override
    public void identityRating(Address address, ResultCallback<Rating> callback) {
        threadExecutor.execute(() -> {
            Identity ident = PEpUtils.createIdentity(address, context);
            notifyLoaded(identityRating(ident), callback);
        });
    }

    @Override
    public Rating identityRating(Identity ident) {
        createEngineInstanceIfNeeded();
        try {
            Rating result =  engine.identity_rating(ident);
            return result;
        } catch (pEpException e) {
            Log.e(TAG, "identityRating: ", e);
            return Rating.pEpRatingUndefined;
        }
    }

    @Override
    public void identityRating(final Identity identity, final ResultCallback<Rating> callback) {
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
    public String trustwords(Identity id, String language) {
        id.lang = language;
        createEngineInstanceIfNeeded();
        return engine.trustwords(id);
    }

    @Override
    public void trustwords(Identity self, Identity other, String lang, ResultCallback<HandshakeData> callback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                Identity myself = self;
                Identity partner = other;
                engine = getNewEngineSession();
                myself.lang = PEpUtils.obtainTrustwordsLang(lang);
                myself.user_id = PEP_OWN_USER_ID;
                myself = engine.myself(myself);
                partner.lang = PEpUtils.obtainTrustwordsLang(lang);
                partner = engine.updateIdentity(partner);
                String myTrust = engine.trustwords(myself);
                String theirTrust = engine.trustwords(partner);
                String myTrustShort = PEpUtils.getShortTrustwords(myTrust);
                String theirTrustShort = PEpUtils.getShortTrustwords(theirTrust);

                String trust = "";
                String shortTrust = "";
                if (myself.fpr.compareTo(partner.fpr) > 0) {
                    trust = theirTrust + myTrust;
                    shortTrust = theirTrustShort + myTrustShort;
                } else {
                    trust = myTrust + theirTrust;
                    shortTrust = myTrustShort + theirTrustShort;
                }
                notifyLoaded(new HandshakeData(trust, shortTrust, myself, partner), callback);
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
    public void close() {
        if (engine != null) engine.close();
    }

    @Override
    public Identity updateIdentity(Identity id) {
        createEngineInstanceIfNeeded();
        return engine.updateIdentity(id);
    }

    @Override
    public void trustPersonaKey(Identity id) {
        createEngineInstanceIfNeeded();
        engine.trustPersonalKey(id);
    }

    @Override
    public void keyCompromised(Identity id) {
        createEngineInstanceIfNeeded();
        engine.keyMistrusted(id);
    }

    @Override
    public void resetTrust(Identity id) {
        createEngineInstanceIfNeeded();
        engine.keyResetTrust(id);
    }

    @Override
    public void resetTrust(Identity id, CompletedCallback completedCallback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                engine.keyResetTrust(id);
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
    public String getLog() {
        return engine.getCrashdumpLog(100);
    }

    @Override
    public void printLog() {
        String logLines[] = getLog().split("\n");
        for (String logLine : logLines) {
            Log.i("PEPJNI", logLine);
        }
    }

    @Override
    public Identity myself(Identity myId) {
        createEngineInstanceIfNeeded();
        myId.user_id = PEP_OWN_USER_ID;
        return engine.myself(myId);
    }

    @Override
    public void setPassiveModeEnabled(boolean enable) {
        createEngineInstanceIfNeeded();
        engine.config_passive_mode(enable);
    }

    @Override
    public void startKeyserverLookup() {
        createEngineInstanceIfNeeded();
        engine.startKeyserverLookup();
    }

    @Override
    public void stopKeyserverLookup() {
        createEngineInstanceIfNeeded();
        engine.stopKeyserverLookup();
    }

    @Override
    public KeyDetail getOwnKeyDetails(Message message) {
//        createEngineInstanceIfNeeded();
        Identity id;
        try {
            id = engine.own_message_private_key_details(message);
            return  new KeyDetail(buildImportDialogText(context, id, message.getFrom().address), id.fpr, new Address(id.address, id.username));
        } catch (Exception e) {
            Log.e(TAG, "getOwnKeyDetails: ", e);
        }
        return null;
    }

    private void createEngineInstanceIfNeeded() {
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

    private String buildImportDialogText(Context context, Identity id, String fromAddress) {
        StringBuilder stringBuilder = new StringBuilder();
        String formattedFpr = PEpUtils.formatFpr(id.fpr);
        stringBuilder.append(context.getString(R.string.pep_receivedSecretKey))
                .append("\n")
                .append(context.getString(R.string.pep_username)).append(": ")
                .append(id.username).append("\n")
                .append(context.getString(R.string.pep_userAddress)).append(": ")
                .append(id.address).append("\n")
                .append("\n")
                .append(formattedFpr.substring(0, formattedFpr.length()/2))
                .append("\n")
                .append(formattedFpr.substring(formattedFpr.length()/2))
                .append("\n").append("\n")
                .append(context.getString(R.string.recipient_from)).append(": ")
                .append(fromAddress);

        return stringBuilder.toString();
    }

    @Override
    public void setSubjectUnprotected (boolean isUnprotected) {
        createEngineInstanceIfNeeded();
        engine.config_unencrypted_subject(isUnprotected);
    }

    @Override
    public List<KeyListItem> getAvailableKey() {
        try {
            List<KeyListItem> identites = new ArrayList<>();
            ArrayList<Pair<String, String>> keys = engine.OpenPGP_list_keyinfo("");
            for (Pair<String, String> key : keys) {
                identites.add(new KeyListItem(key.first, key.second, engine.blacklist_is_listed(key.first)));
            }
            return identites;
        } catch (pEpException e) {
            Log.e(TAG, "getAvailableKey", e);
        }

        return null;

    }

    @Override
    public void addToBlacklist(String fpr) {
        engine.blacklist_add(fpr);
    }

    @Override
    public void deleteFromBlacklist(String fpr) {
        engine.blacklist_delete(fpr);
    }

    @Override
    public com.fsck.k9.mail.Message getMimeMessage(Message message) {
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
    public void setSyncSendMessageCallback(Sync.MessageToSendCallback callback) {
        engine.setMessageToSendCallback(callback);
        sendMessageSet = true;
        if (areCallbackSet() && !keysyncStarted) {
            engine.startSync();
            keysyncStarted = true;
            Log.e(TAG, "setstartSync: ");
        }
        Log.i(TAG, "setSyncSendMessageCallback: SEND");

    }

    private boolean areCallbackSet() {
        return sendMessageSet && showHandshakeSet;
    }

    @Override
    public void setSyncHandshakeCallback(Sync.notifyHandshakeCallback activity) {
        engine.setnotifyHandshakeCallback(activity);
        showHandshakeSet = true;
        if (areCallbackSet() && !keysyncStarted) {
            engine.startSync();
            keysyncStarted = true;
            Log.e(TAG, "setstartSync: ");
        }
        Log.i(TAG, "setSyncHandshakeCallback: SEND");
    }

    @Override
    public void startSync() {
        engine.startSync();
    }

    @Override
    public void acceptHandshake(Identity identity) {
        engine.accept_sync_handshake(identity);
    }

    @Override
    public void rejectHandshake(Identity identity) {
        engine.reject_sync_handshake(identity);
    }

    @Override
    public void cancelHandshake(Identity identity) {
        engine.cancel_sync_handshake(identity);
    }

    @Override
    public void loadOwnIdentities(ResultCallback<List<Identity>> callback) {
        threadExecutor.execute(() -> {
            Engine engine = null;
            try {
                engine = getNewEngineSession();
                List<Identity> identities = engine.own_identities_retrieve();
                notifyLoaded(identities, callback);
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

    private void notifyLoaded(final Object privacyState, final ResultCallback callback) {
        this.postExecutionThread.post(() -> callback.onLoaded(privacyState));
    }

    private void notifyCompleted(CompletedCallback completedCallback) {
        this.postExecutionThread.post(completedCallback::onComplete);
    }

    private void notifyError(final Throwable throwable, final Callback callback) {
        this.postExecutionThread.post(() -> callback.onError(throwable));
    }
}