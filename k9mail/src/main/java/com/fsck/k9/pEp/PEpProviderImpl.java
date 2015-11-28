package com.fsck.k9.pEp;

import android.content.Context;
import android.util.Log;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeMessage;
import org.pEp.jniadapter.AndroidHelper;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Engine;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;

import java.util.Collections;
import java.util.Vector;

/**
 * pep provider implementation. Dietz is the culprit.
 */
public class PEpProviderImpl implements PEpProvider {
    private static boolean pEpInitialized = false;

    public synchronized void setup(Context c) {
        if(!pEpInitialized) {
            AndroidHelper.setup(c);
            pEpInitialized = true;
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

    @Override
    public Color getPrivacyState(Address from, Address[] toAdresses, Address[] ccAdresses, Address[] bccAdresses) {
        if(from == null || toAdresses.length == 0)
            return Color.pEpRatingUndefined;

        Message testee = null;
        Engine engine = null;
        try {
            engine = new Engine();
            testee = new Message();

            Identity idFrom = PEpUtils.createIdentity(from);
            idFrom.me = true;
            engine.myself(idFrom);              // not sure wether that call is necessary. But it should do no harm. If necessary, add below too. Now called in right context if only one account.
            testee.setFrom(idFrom);
            testee.setTo(PEpUtils.createIdentities(toAdresses));
            testee.setCc(PEpUtils.createIdentities(ccAdresses));
            testee.setBcc(PEpUtils.createIdentities(bccAdresses));
            testee.setShortmsg("hello, world");     // FIXME: do I need them?
            testee.setLongmsg("Lorem ipsum");
            testee.setDir(Message.Direction.Outgoing);

            Color rv = engine.outgoing_message_color(testee);   // stupid way to be able to patch the value in debugger
            return rv;
        } catch (Throwable e) {
            Log.e("pep", "during color test", e);
        } finally {
            if (engine != null) engine.close();
            if (testee != null) testee.close();
        }

        return Color.pEpRatingB0rken;
    }

    @Override
    public DecryptResult decryptMessage(MimeMessage source) {
        Message  srcMsg = null;
        Engine engine = null;
        Engine.decrypt_message_Return decReturn;
        try {
            engine = new Engine();

            srcMsg = PEpUtils.createMessage(source);
            srcMsg.setDir(Message.Direction.Incoming);

            decReturn = engine.decrypt_message(srcMsg);

            return new DecryptResult(PEpUtils.createMimeMessage(decReturn.dst), decReturn.color);
        } catch (Throwable t) {
            Log.e("pep", "Error from conversion:", t);
        } finally {
            if (engine != null) engine.close();
            if (srcMsg != null) srcMsg.close();
//            if (decReturn != null) decReturn.dst.close();         causes a SIGSEGV atm...
        }
        return null;
    }

    @Override
    public MimeMessage encryptMessage(MimeMessage source, String[] extraKeys) {
        Message  srcMsg = null;
        Message encMsg = null;
        Engine engine = null;
        try {
            engine = new Engine();
            srcMsg = PEpUtils.createMessage(source);
            srcMsg.setDir(Message.Direction.Outgoing);
            encMsg = engine.encrypt_message(srcMsg, convertExtraKeys(extraKeys));
            return PEpUtils.createMimeMessage(encMsg);
        } catch (Throwable t) {
            Log.e("pep", "Error from conversion:", t);
        } finally {
            if (engine != null) engine.close();
            if (srcMsg != null) srcMsg.close();
//            if (encMsg != null) srcMsg.close();       causes a SIGSEGV atm...
        }
        return null;
    }

    private Vector<String> convertExtraKeys(String[] extraKeys) {
        if(extraKeys == null || extraKeys.length == 0) return null;
        Vector<String> rv = new Vector<String>();
        Collections.addAll(rv, extraKeys);
        return rv;
    }
}