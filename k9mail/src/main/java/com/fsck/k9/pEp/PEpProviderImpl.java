package com.fsck.k9.pEp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.AndroidHelper;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Engine;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;
import org.pEp.jniadapter.pEpException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * pep provider implementation. Dietz is the culprit.
 */
public class PEpProviderImpl implements PEpProvider {
    private static final String TAG = "pEp";
    private static boolean pEpInitialized = false;
    private Context context;
    private Engine engine;

    public PEpProviderImpl(Context context) {
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
    public Color getPrivacyState(com.fsck.k9.mail.Message message) {

        Address from = message.getFrom()[0];                            // FIXME: From is an array?!
        List<Address> to = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO));
        List<Address> cc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC));
        List<Address> bcc = Arrays.asList(message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC));
        return getPrivacyState(from, to, cc, bcc);

    }

    @Override
    public Color getPrivacyState(Message message) {
        try {
            if (engine == null) {
                createEngineSession();
            }
            return engine.outgoing_message_color(message);
        } catch (pEpException e) {
            Log.e(TAG, "during getPrivacyState:", e);
        }
        return Color.pEpRatingB0rken;
    }

    private void createEngineSession() throws pEpException {
        engine = new Engine();
        engine.config_passive_mode(K9.getPEpPassiveMode());
        configKeyServerLockup(K9.getPEpUseKeyserver());
    }

    private void configKeyServerLockup(boolean pEpUseKeyserver) {
        if (pEpUseKeyserver) startKeyserverLookup();
        else stoptKeyserverLookup();
    }

    //Don't instantiate a new engine
    @Override
    public Color getPrivacyState(Address from, List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses) {
        int recipientsSize = toAddresses.size() + ccAddresses.size() + bccAddresses.size();
        if (from == null || recipientsSize == 0)
            return Color.pEpRatingUndefined;

        Message testee = null;
        try {
            if (engine == null) {
                createEngineSession();

            }
            testee = new Message();

            Identity idFrom = PEpUtils.createIdentity(from, context);
            idFrom.me = true;
            idFrom = myself(idFrom);            // not sure wether that call is necessary. But it should do no harm. If necessary, add below too. Now called in right context if only one account.
            testee.setFrom(idFrom);
            testee.setTo(PEpUtils.createIdentities(toAddresses, context));
            testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
            testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
            testee.setShortmsg("hello, world");     // FIXME: do I need them?
            testee.setLongmsg("Lorem ipsum");
            testee.setDir(Message.Direction.Outgoing);

            Color result = engine.outgoing_message_color(testee);   // stupid way to be able to patch the value in debugger
            Log.i(TAG, "getPrivacyState " + idFrom.fpr);
            if (result.value != Color.pEpRatingUnencrypted.value) return result;
            else {
                if (isUnencryptedForSome(toAddresses, ccAddresses, bccAddresses)) {
                    return Color.pEpRatingUnencryptedForSome;
                } else return result;
            }
        } catch (Throwable e) {
            Log.e(TAG, "during color test:", e);
        } finally {
            if (testee != null) testee.close();
        }

        return Color.pEpRatingB0rken;
    }

    private boolean isUnencryptedForSome(List<Address> toAddresses, List<Address> ccAddresses, List<Address> bccAddresses) {
        for (Address toAddress : toAddresses) {
            if (identityColor(toAddress).value > Color.pEpRatingUnencrypted.value) return true;
        }
        for (Address ccAddress : ccAddresses) {
            if (identityColor(ccAddress).value > Color.pEpRatingUnencrypted.value) return true;
        }
        for (Address bccAddress : bccAddresses) {
            if (identityColor(bccAddress).value > Color.pEpRatingUnencrypted.value) return true;
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
            decReturn = engine.decrypt_message(srcMsg);
            Log.d(TAG, "decryptMessage() after decrypt");
            MimeMessage decMsg = new MimeMessageBuilder(decReturn.dst).createMessage();

            decMsg.addHeader(MimeHeader.HEADER_PEPCOLOR, decReturn.color.name());
            if (isUsablePrivateKey(decReturn)) {
                return new DecryptResult(decMsg, decReturn.color, getOwnKeyDetails(srcMsg));
            }
            else return new DecryptResult(decMsg, decReturn.color, null);
        } catch (Throwable t) {
            Log.e(TAG, "while decrypting message:", t);
            throw new RuntimeException("Could not decrypt", t);
        } finally {
            if (srcMsg != null) srcMsg.close();
            if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
            Log.d(TAG, "decryptMessage() exit");
        }
    }
    private boolean isUsablePrivateKey(Engine.decrypt_message_Return result) throws MessagingException {
        // TODO: 13/06/16 Check if is necesary check own id
        return result.color.value >= Color.pEpRatingGreen.value
                && result.flags != null
                && result.flags == DecryptFlags.pEpDecryptFlagOwnPrivateKey;
    }

    @Override
    public List<MimeMessage> encryptMessage(MimeMessage source, String[] extraKeys) {
        Log.d(TAG, "encryptMessage() enter");
        List<MimeMessage> resultMessages = new ArrayList<>();

        try {
            if (engine == null) createEngineSession();
            resultMessages.addAll(getEncryptedCopies(source, extraKeys));
            resultMessages.addAll(getUnencryptedCopies(source, extraKeys));
            return resultMessages;
        } catch (Throwable t) {
            Log.e(TAG, "while encrypting message:", t);
            throw new RuntimeException("Could not encrypt");
        } finally {
            Log.d(TAG, "encryptMessage() exit");
        }
    }

    private List<MimeMessage> getUnencryptedCopies(MimeMessage source, String[] extraKeys) throws MessagingException, pEpException {
        List<MimeMessage> messages = new ArrayList<>();
        messages.add(getUnencryptedBCCCopy(source));
        messages.add(getEncryptedCopy(getUnencryptedCopyWithoutBCC(source), extraKeys));
        return messages;

    }

    private Message getUnencryptedCopyWithoutBCC(MimeMessage source) throws MessagingException {
        Message message = stripEncryptedRecipients(source);
        message.setBcc(null);
        return message;
    }

    private MimeMessage getUnencryptedBCCCopy(MimeMessage source) throws MessagingException {
        Message message = stripEncryptedRecipients(source);
        message.setTo(null);
        message.setCc(null);
        MimeMessage result = new MimeMessageBuilder(message).createMessage();
        message.close();
        return result;
    }

    @NonNull
    private List<MimeMessage> encryptMessages(String[] extraKeys, List<Message> messagesToEncrypt) throws pEpException, MessagingException {
        List<MimeMessage> messages = new ArrayList<>();
        for (Message message : messagesToEncrypt) {
            messages.add(getEncryptedCopy(message, extraKeys));
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

        result.addAll(encryptMessages(extraKeys, messagesToEncrypt));

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

    MimeMessage getEncryptedCopy(Message message, String[] extraKeys) throws pEpException, MessagingException {
        message.setDir(Message.Direction.Outgoing);
        Log.d(TAG, "encryptMessage() before encrypt");
        Identity from = message.getFrom();
        from.user_id = PEP_OWN_USER_ID;
        message.setFrom(from);
        Message currentEnc = engine.encrypt_message(message, convertExtraKeys(extraKeys));
        if (currentEnc == null) currentEnc = message;
        Log.d(TAG, "encryptMessage() after encrypt");
        return new MimeMessageBuilder(currentEnc).createMessage();
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
        return identityColor(identity).value > Color.pEpRatingUnencrypted.value;
    }

    private Vector<String> convertExtraKeys(String[] extraKeys) {
        if (extraKeys == null || extraKeys.length == 0) return null;
        Vector<String> rv = new Vector<String>();
        Collections.addAll(rv, extraKeys);
        return rv;
    }


    @Override
    public Color identityColor(Address address) {
        Identity ident = PEpUtils.createIdentity(address, context);
        return identityColor(ident);
    }

    @Override
    public Color identityColor(Identity ident) {
        createEngineInstanceIfNeeded();
        try {
            return engine.identity_color(ident);
        } catch (pEpException e) {
            Log.e(TAG, "identityColor: ", e);
            return Color.pEpRatingB0rken;
        }
    }

    @Override
    public String trustwords(Identity id) {
        createEngineInstanceIfNeeded();
        id = updateIdentity(id);
        return engine.trustwords(id);
    }

    @Override
    public void close() {
        if (engine != null) engine.close();
    }

    @Override
    public Identity updateIdentity(Identity id) {
        createEngineInstanceIfNeeded();
//        engine.startKeyserverLookup();
        Identity result = engine.updateIdentity(id);
//        engine.stopKeyserverLookup();
        return result;
    }

    @Override
    public void trustPersonaKey(Identity id) {
        createEngineInstanceIfNeeded();
        engine.trustPersonalKey(id);
    }

    @Override
    public void keyCompromised(Identity id) {
        createEngineInstanceIfNeeded();
        engine.keyCompromized(id);
    }

    @Override
    public void resetTrust(Identity id) {
        createEngineInstanceIfNeeded();
        engine.keyResetTrust(id);
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
    public void stoptKeyserverLookup() {
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

    @Override
    public void setPassiveModeEnabled(boolean enable) {
        createEngineInstanceIfNeeded();
        engine.config_passive_mode(enable);
    }

    private void createEngineInstanceIfNeeded() {
        if (engine == null) {
            try {
                createEngineSession();
            } catch (pEpException e) {
                Log.e(TAG, "createEngineInstanceIfNeeded", e);
            }
        }
    }

    private String buildImportDialogText(Context context, Identity id, String fromAddress) {
        StringBuilder stringBuilder = new StringBuilder();
        String formatedFpr = PEpUtils.formatFpr(id.fpr);
        stringBuilder.append(context.getString(R.string.receivedSecretKey))
                .append("\n")
                .append(context.getString(R.string.username)).append(": ")
                .append(id.username).append("\n")
                .append(context.getString(R.string.userAddress)).append(": ")
                .append(id.address).append("\n")
                .append("\n")
                .append(formatedFpr.substring(0, formatedFpr.length()/2))
                .append("\n")
                .append(formatedFpr.substring(formatedFpr.length()/2))
                .append("\n").append("\n")
                .append(context.getString(R.string.recipient_from)).append(": ")
                .append(fromAddress);

        return stringBuilder.toString();
    }
}