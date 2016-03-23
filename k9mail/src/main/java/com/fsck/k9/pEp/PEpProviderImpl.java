package com.fsck.k9.pEp;

import android.content.Context;
import android.util.Log;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.*;

import java.util.Collections;
import java.util.Vector;

/**
 * pep provider implementation. Dietz is the culprit.
 */
public class PEpProviderImpl implements PEpProvider {
    private static boolean pEpInitialized = false;
    private Context context;
    private Engine engine;

    public synchronized void setup(Context c) {
        if(!pEpInitialized) {
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
        try {
            Address from = message.getFrom()[0];                            // FIXME: From is an array?!
            Address[] to = message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO);
            Address[] cc = message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC);
            Address[] bcc = message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC);
            return getPrivacyState(from, to, cc, bcc);
        }
        catch(MessagingException me) {
                Log.e("pEp", "Could not extract addresses: "+ me.getMessage());
        }
        return Color.pEpRatingB0rken;
    }

    //Don't instantiate a new engine
    @Override
    public Color getPrivacyState(Address from, Address[] toAdresses, Address[] ccAdresses, Address[] bccAdresses) {
        if(from == null || toAdresses.length == 0)
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
            testee.setTo(PEpUtils.createIdentities(toAdresses, context));
            testee.setCc(PEpUtils.createIdentities(ccAdresses, context));
            testee.setBcc(PEpUtils.createIdentities(bccAdresses, context));
            testee.setShortmsg("hello, world");     // FIXME: do I need them?
            testee.setLongmsg("Lorem ipsum");
            testee.setDir(Message.Direction.Outgoing);

            Color rv = engine.outgoing_message_color(testee);   // stupid way to be able to patch the value in debugger
            idFrom = engine.updateIdentity(idFrom);
            Log.i("PEpProviderImpl", "getPrivacyState " + idFrom.fpr);

            return rv;
        } catch (Throwable e) {
            Log.e("pep", "during color test:", e);
        } finally {
            if (testee != null) testee.close();
//            if (engine != null) engine.close();
        }

        return Color.pEpRatingB0rken;
    }

    @Override
    public DecryptResult decryptMessage(MimeMessage source) {
        Log.d("pep", "decryptMessage() enter");
        Message  srcMsg = null;
        Engine engine = null;
        Engine.decrypt_message_Return decReturn = null;
        try {
            engine = new Engine();

            srcMsg = new PEpMessageBuilder(source).createMessage(context);
            srcMsg.setDir(Message.Direction.Incoming);

            Log.d("pep", "decryptMessage() before decrypt");
            decReturn = engine.decrypt_message(srcMsg);
            Log.d("pep", "decryptMessage() after decrypt");
            MimeMessage decMsg = new MimeMessageBuilder(decReturn.dst).createMessage();
            decMsg.addHeader(MimeHeader.HEADER_PEPCOLOR, decReturn.color.name());
            return new DecryptResult(decMsg, decReturn.color);
        } catch (Throwable t) {
            Log.e("pep", "while decrypting message:", t);
            throw new RuntimeException("Could not decrypt");
        } finally {
            if (srcMsg != null) srcMsg.close();
            if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
            if (engine != null) engine.close();
            Log.d("pep", "decryptMessage() exit");
        }
    }

    @Override
    public MimeMessage encryptMessage(MimeMessage source, String[] extraKeys) {
        Log.d("pep", "encryptMessage() enter");
        Message  srcMsg = null;
        Message encMsg = null;
        Engine engine = null;
        try {
            engine = new Engine();
            srcMsg = new PEpMessageBuilder(source).createMessage(context);
            srcMsg.setDir(Message.Direction.Outgoing);

            Log.d("pep", "encryptMessage() before encrypt");
            encMsg = engine.encrypt_message(srcMsg, convertExtraKeys(extraKeys));
            Log.d("pep", "encryptMessage() after encrypt");

            if(encMsg == null) {
                Log.e("pep", "engine returned null.");
                encMsg = srcMsg;         // FIXME: this should be done by the engine! I could return source, but this would mask engine and my own errors...
            }
            return new MimeMessageBuilder(encMsg).createMessage();
        } catch (Throwable t) {
            Log.e("pep", "while encrypting message:", t);
            throw new RuntimeException("Could not encrypt");
        } finally {
            if (srcMsg != null) srcMsg.close();
            if (engine != null) engine.close();
            Log.d("pep", "encryptMessage() exit");
        }
    }

    private Vector<String> convertExtraKeys(String[] extraKeys) {
        if(extraKeys == null || extraKeys.length == 0) return null;
        Vector<String> rv = new Vector<String>();
        Collections.addAll(rv, extraKeys);
        return rv;
    }


    @Override
    public Color getIdentityColor(Address address) {
        Identity ident = PEpUtils.createIdentity(address, context);
        try {
            if (engine == null) {
                engine = new Engine();
            }
            Color rv = engine.identity_color(ident);
            return rv;
        } catch (Throwable e) {
            Log.e("pep", "during color test:", e);
        }
        return Color.pEpRatingB0rken;
    }

    @Override
    public Color getIdentityColor(Identity ident) {
        try {
            if (engine == null) {
                engine = new Engine();
            }
            Color rv = engine.identity_color(ident);
            return rv;
        } catch (Throwable e) {
            Log.e("pep", "during color test:", e);
        }
        return Color.pEpRatingB0rken;
    }

    @Override
    public String trustwords(Identity id) {
        try {
            if (engine == null) {
                engine = new Engine();
            }
            id = engine.updateIdentity(id);
            return engine.trustwords(id);
        } catch (Throwable e) {
            Log.e("pep", "during trustwords:", e);
        }
        return "";
    }
    @Override
    public void close() {
        if (engine != null) engine.close();
    }

    @Override
    public Identity updateIdentity(Identity id) {
        try {
            if (engine == null) {
                engine = new Engine();
            }
           return engine.updateIdentity(id);
        } catch (Throwable e) {
            Log.e("pep", "during update identity:", e);
        }
        return id;
    }

    @Override
    public void trustPersonaKey(Identity id) {
        try {
            if (engine == null) {
                engine = new Engine();
            }
            engine.trustPersonalKey(id);
        } catch (Throwable e) {
            Log.e("pep", "during trustwords:", e);
        }
    }
}