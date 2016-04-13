package com.fsck.k9.pEp;

import android.content.Context;
import android.util.Log;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.AndroidHelper;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Engine;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;
import org.pEp.jniadapter.pEpException;

import java.util.Collections;
import java.util.Vector;

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
            Address[] to = message.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO);
            Address[] cc = message.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC);
            Address[] bcc = message.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC);
            return getPrivacyState(from, to, cc, bcc);
        }

    //Don't instantiate a new engine
    @Override
    public Color getPrivacyState(Address from, Address[] toAdresses, Address[] ccAdresses, Address[] bccAdresses) {
        if (from == null || toAdresses.length == 0)
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
            Log.i(TAG, "getPrivacyState " + idFrom.fpr);

            return rv;
        } catch (Throwable e) {
            Log.e(TAG, "during color test:", e);
        } finally {
            if (testee != null) testee.close();
//            if (engine != null) engine.close();
        }

        return Color.pEpRatingB0rken;
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
            MimeMessage decMsg = new MimeMessageBuilder(decReturn.dst).createMessage(true);

            decMsg.addHeader(MimeHeader.HEADER_PEPCOLOR, decReturn.color.name());
            return new DecryptResult(decMsg, decReturn.color);
        } catch (Throwable t) {
            Log.e(TAG, "while decrypting message:", t);
            throw new RuntimeException("Could not decrypt");
        } finally {
            if (srcMsg != null) srcMsg.close();
            if (decReturn != null && decReturn.dst != srcMsg) decReturn.dst.close();
            Log.d(TAG, "decryptMessage() exit");
        }
    }

    @Override
    public MimeMessage encryptMessage(MimeMessage source, String[] extraKeys) {
        Log.d(TAG, "encryptMessage() enter");
        Message srcMsg = null;
        Message encMsg = null;
        try {
            if (engine == null) engine = new Engine();
            srcMsg = new PEpMessageBuilder(source).createMessage(context);
            srcMsg.setDir(Message.Direction.Outgoing);

            Log.d(TAG, "encryptMessage() before encrypt");
            encMsg = engine.encrypt_message(srcMsg, convertExtraKeys(extraKeys));
            Log.d(TAG, "encryptMessage() after encrypt");

            if (encMsg == null) {
                Log.e(TAG, "engine returned null.");
                encMsg = srcMsg;         // FIXME: this should be done by the engine! I could return source, but this would mask engine and my own errors...
            }
            return new MimeMessageBuilder(encMsg).createMessage(false);
        } catch (Throwable t) {
            Log.e(TAG, "while encrypting message:", t);
            throw new RuntimeException("Could not encrypt");
        } finally {
            if (srcMsg != null) srcMsg.close();
            Log.d(TAG, "encryptMessage() exit");
        }
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
        try {
            if (engine == null) {
                engine = new Engine();
            }
            Color rv = engine.identity_color(ident);
            return rv;
        } catch (pEpException e) {
            Log.e(TAG, "during color test:", e);
        }
        return Color.pEpRatingB0rken;
    }

    @Override
    public Color identityColor(Identity ident) {
        try {
            if (engine == null) {
                engine = new Engine();
            }
            return engine.identity_color(ident);
        } catch (pEpException e) {
            Log.e(TAG, "during color test:", e);
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
        } catch (pEpException e) {
            Log.e(TAG, "during trustwords:", e);
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
        } catch (pEpException e) {
            Log.e(TAG, "during update identity:", e);
        }
        return id;
    }

    @Override
    public void trustPersonaKey(Identity id) {
        try {
            if (engine == null) {
                engine = new Engine();
            }
        } catch (pEpException e) {
            Log.e(TAG, "during trustwords:", e);
        }
        engine.trustPersonalKey(id);
    }

    @Override
    public void keyCompromised(Identity id) {
        if (engine == null) {
            try {
                engine = new Engine();
            } catch (pEpException e) {
                Log.e("pEp", "keyCompromised: ", e);
            }
        }
        engine.keyCompromized(id);
    }

    @Override
    public void resetTrust(Identity id) {
        if (engine == null) {
            try {
                engine = new Engine();
            } catch (pEpException e) {
                e.printStackTrace();
            }
        }
        engine.keyResetTrust(id);
    }

    @Override
    public void myself(Identity myId) {
        if (engine == null) {
            try {
                engine = new Engine();
            } catch (pEpException e) {
                Log.e(TAG, "myself: ", e);
            }
        }
        engine.myself(myId);
    }
}