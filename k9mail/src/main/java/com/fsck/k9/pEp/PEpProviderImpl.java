package com.fsck.k9.pEp;

import android.content.Context;
import android.util.Log;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.AndroidHelper;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Engine;
import org.pEp.jniadapter.pEpException;

import java.util.*;

/**
 * pep provider implementation. Dietz is the culprit.
 */
public class PEpProviderImpl implements PEpProvider {
    private static final String TAG = "pEp";
    private static boolean pEpInitialized = false;
    private Context context;
    private Engine engine;

    public synchronized void setup(Context c) {
        if (!pEpInitialized) {
            AndroidHelper.setup(c);
            pEpInitialized = true;
        }

        context = c;
        try {
            engine = new Engine();
        } catch (pEpException e) {
            Log.e("pEpProvider", "setup: ", e);
        }

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
                engine = new Engine();
            }
            return engine.outgoing_message_color(message);
        } catch (pEpException e) {
            Log.e(TAG, "during getPrivacyState:", e);
        }
        return Color.pEpRatingB0rken;
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
                engine = new Engine();

            }
            testee = new Message();

            Identity idFrom = PEpUtils.createIdentity(from, context);
            idFrom.me = true;
            engine.myself(idFrom);              // not sure wether that call is necessary. But it should do no harm. If necessary, add below too. Now called in right context if only one account.
            testee.setFrom(idFrom);
            testee.setTo(PEpUtils.createIdentities(toAddresses, context));
            testee.setCc(PEpUtils.createIdentities(ccAddresses, context));
            testee.setBcc(PEpUtils.createIdentities(bccAddresses, context));
            testee.setShortmsg("hello, world");     // FIXME: do I need them?
            testee.setLongmsg("Lorem ipsum");
            testee.setDir(Message.Direction.Outgoing);

            Color result = engine.outgoing_message_color(testee);   // stupid way to be able to patch the value in debugger
            idFrom = engine.updateIdentity(idFrom);
            Log.i(TAG, "getPrivacyState " + idFrom.fpr);
            if (result.value != Color.pEpRatingUnencrypted.value) return result;
            else {
                if (isUnencryptedForSome(toAddresses, ccAddresses, bccAddresses)) {
                    return Color.pEpRatingUnencryptedForSome;
                }
                else return result;
            }
        } catch (Throwable e) {
            Log.e(TAG, "during color test:", e);
        } finally {
            if (testee != null) testee.close();
//            if (engine != null) engine.close();
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

    private boolean isUnencryptedForSome(Message message) {
        if (message.getTo() != null) {
            for (Identity toIdentity : message.getTo()) {
                if (identityColor(toIdentity).value > Color.pEpRatingUnencrypted.value) return true;
            }
        }

        if (message.getCc() != null) {
            for (Identity ccIdentity : message.getCc()) {
                if (identityColor(ccIdentity).value > Color.pEpRatingUnencrypted.value) return true;
            }
        }
        if (message.getBcc() != null) {
            for (Identity bccIdentity : message.getBcc()) {
                if (identityColor(bccIdentity).value > Color.pEpRatingUnencrypted.value) return true;
            }
        }
        return false;
    }

    @Override
    public DecryptResult decryptMessage(MimeMessage source) {
        Log.d(TAG, "decryptMessage() enter");
        Message srcMsg = null;
        Engine.decrypt_message_Return decReturn = null;
        try {
            if (engine == null) engine = new Engine();

            srcMsg = new PEpMessageBuilder(source).createMessage(context);
            srcMsg.setDir(Message.Direction.Incoming);

            Log.d(TAG, "decryptMessage() before decrypt");
            decReturn = engine.decrypt_message(srcMsg);
            Log.d(TAG, "decryptMessage() after decrypt");
            MimeMessage decMsg = new MimeMessageBuilder(this, decReturn.dst).createMessage(true);

            decMsg.addHeader(MimeHeader.HEADER_PEPCOLOR, decReturn.color.name());
            return new DecryptResult(decMsg, decReturn.color);
        } catch (Throwable t) {
            Log.e(TAG, "while decrypting message:", t);
            throw new RuntimeException("Could not decrypt", t);
        } finally {
            if (srcMsg != null) srcMsg.close();
            if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
            Log.d(TAG, "decryptMessage() exit");
        }
    }

    @Override
    public List<MimeMessage> encryptMessage(MimeMessage source, String[] extraKeys) {
        Log.d(TAG, "encryptMessage() enter");
        Message srcMsg = null;
        Message encMsg = null;
        List <Message> messagesTo_pEp = new ArrayList<>();

        try {
            if (engine == null) engine = new Engine();
            srcMsg = new PEpMessageBuilder(source).createMessage(context);
            if (isUnencryptedForSome(srcMsg) || srcMsg.getBcc() != null && srcMsg.getBcc().size() > 1) {
                Message toEncryptMessage = prepareMessageToSend(source, true);
                messagesTo_pEp.add(toEncryptMessage);

                if (toEncryptMessage.getBcc() != null) {
                    for (Identity identity : toEncryptMessage.getBcc()) {
                        Message message = new PEpMessageBuilder(source).createMessage(context);
                        message.setTo(null);
                        message.setCc(null);
                        Vector <Identity> oneBCCList = new Vector<>();
                        oneBCCList.add(identity);
                        message.setBcc(oneBCCList);
                        messagesTo_pEp.add(message);
                    }
                    toEncryptMessage.setBcc(null);
                    if(toEncryptMessage.getTo() == null
                            && toEncryptMessage.getCc() == null
                            && toEncryptMessage.getBcc() == null) {
                        messagesTo_pEp.remove(ENCRYPTED_MESSAGE_POSITION);
                    }
                }

                Message unencryptedMessage = prepareMessageToSend(source, false);
                messagesTo_pEp.add(unencryptedMessage);

            } else {
                messagesTo_pEp.add(srcMsg);
            }

            List<MimeMessage> messages = new ArrayList<>();
            for (Message message : messagesTo_pEp) {
                messages.add(getEncryptedCopy(message, extraKeys));
            }

            return messages;
        } catch (Throwable t) {
            Log.e(TAG, "while encrypting message:", t);
            throw new RuntimeException("Could not encrypt");
        } finally {
            if (srcMsg != null) srcMsg.close();
            for (Message message : messagesTo_pEp) {
                if (message != null) message.close();
            }
            Log.d(TAG, "encryptMessage() exit");
        }
    }

    MimeMessage getEncryptedCopy(Message message, String[] extraKeys) throws pEpException, MessagingException {
        message.setDir(Message.Direction.Outgoing);
        Log.d(TAG, "encryptMessage() before encrypt");
        Message currentEnc = engine.encrypt_message(message, convertExtraKeys(extraKeys));
        if (currentEnc == null) currentEnc = message;
        Log.d(TAG, "encryptMessage() after encrypt");
        return new MimeMessageBuilder(this, currentEnc).createMessage(false);
    }

    private Message prepareMessageToSend(MimeMessage src, boolean encrypted) {
        Message message = new PEpMessageBuilder(src).createMessage(context);
        message.setTo(removeRecipients(message.getTo(), !encrypted));
        message.setCc(removeRecipients(message.getCc(), !encrypted));
        message.setBcc(removeRecipients(message.getBcc(), !encrypted));
        return message;
    }



    private Vector<Identity>  removeRecipients(Vector<Identity> recipientList, boolean deletingEncrypted) {
        if (recipientList != null) {
            for (Iterator<Identity> iterator = recipientList.iterator(); iterator.hasNext(); ) {
                Identity identity = iterator.next();
                if(deletingEncrypted && isEncrypted(identity)
                        || !deletingEncrypted && !isEncrypted(identity)){
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
        engine.updateIdentity(id);
        return engine.trustwords(id);
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
        engine.keyCompromized(id);
    }

    @Override
    public void resetTrust(Identity id) {
        createEngineInstanceIfNeeded();
        engine.keyResetTrust(id);
    }

    @Override
    public void myself(Identity myId) {
        createEngineInstanceIfNeeded();
        engine.myself(myId);
    }

    private void createEngineInstanceIfNeeded() {
        if (engine == null) {
            try {
                engine = new Engine();
            } catch (pEpException e) {
                Log.e(TAG, "createEngineInstanceIfNeeded", e);
            }
        }
    }
}