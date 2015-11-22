package com.fsck.k9.pEp;

import android.util.Log;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.mailstore.LocalBodyPart;
import com.fsck.k9.message.SimpleMessageFormat;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;

import java.util.ArrayList;
import java.util.Vector;

/**
 * some helper stuff
 *
 */

public class PEpUtils {
    static Vector<Identity> createIdentity(Address[] adrs) {
        Vector<Identity> rv = new Vector<Identity>(adrs.length);
        if(adrs == null) return rv;
        for(Address adr : adrs)
            rv.add(createIdentity(adr));
        return rv;
    }

    static Identity createIdentity(Address adr) {
        Identity id = new Identity();
        id.address = adr.getAddress();
        id.username = adr.getAddress();
        id.user_id = adr.getAddress();          // hack to get an unique ID...

        // TODO: do I have any kind of unique id for user_id? (no, I don't, see hack from above)
        return id;
    }

    static Address[] createAddresses(Vector<Identity> ids) {
        Address[] rv = new Address[ids.size()];
        int idx = 0;
        for (Identity i: ids)
            rv[idx++] = createAddress(i);

        return rv;
    }

    static Address createAddress(Identity id) {
        Address adr = new Address(id.address, id.username);
        // Address() parses the address, eventually not setting it, therefore just a little sanity...
        // TODO: pEp check what happens if id.address == null beforehand
        if(adr.getAddress() == null && id.address != null)
            throw new RuntimeException("Could not convert Identiy.address " + id.address + " to Address.");
        return adr;
    }

    static Message createMessage(MimeMessage mm) {
        Message m = null;
        try {
            m = new Message();

            m.setFrom(createIdentity(mm.getFrom()[0]));
            m.setTo(createIdentity(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO)));
            m.setCc(createIdentity(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC)));
            m.setBcc(createIdentity(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC)));

            m.setShortmsg(mm.getSubject());
            // fiddle message txt from MimeMsg...
            MimeMultipart mmp = (MimeMultipart) mm.getBody();
            LocalBodyPart lbp = (LocalBodyPart) mmp.getBodyPart(1);
            BinaryMemoryBody bmb = (BinaryMemoryBody) lbp.getBody();
            m.setLongmsg(new String(bmb.getData(), "UTF-8"));
            // TODO: handle receiving case (should be a 3rd bodypart then...)


            return m;
        } catch (Throwable t) {
            if(m != null) m.close();
            Log.e("pEp", "Could not create message:", t);
        }
        return null;
    }

    static MimeMessage createMimeMessage(Message m) {
        // FIXME: are these new String()s really necessary? I think, the adapter does that already...
        com.fsck.k9.Identity me = new com.fsck.k9.Identity();
        me.setEmail(new String(m.getFrom().address));
        me.setName(new String(m.getFrom().username));
        try {
            MimeMessage rv = new PEpMessageBuilder()
                    .setSubject(new String(m.getShortmsg()))
                    .setTo(createAddresses(m.getTo()))
                            //    .setCc(createAddresses(m.getCc()))
                            //    .setBcc(createAddresses(m.getBcc()))
                            // .setInReplyTo(mInReplyTo)
                            // .setReferences(mReferences)
                            // .setRequestReadReceipt(mReadReceipt)
                    .setIdentity(me)
                    .setMessageFormat(SimpleMessageFormat.TEXT)             // FIXME: pEp: not only text
                    .setText(new String(m.getLongmsg()))
                    .setAttachments(m.getAttachments())
                            // .setSignature(mSignatureView.getCharacters())
                            // .setQuoteStyle(mQuoteStyle)
                            // .setQuotedTextMode(mQuotedTextMode)
                            // .setQuotedText(mQuotedText.getCharacters())
                            // .setQuotedHtmlContent(mQuotedHtmlContent)
                            // .setReplyAfterQuote(mAccount.isReplyAfterQuote())
                            // .setSignatureBeforeQuotedText(mAccount.isSignatureBeforeQuotedText())
                            // .setIdentityChanged(mIdentityChanged)
                            // .setSignatureChanged(mSignatureChanged)
                            // .setCursorPosition(mMessageContentView.getSelectionStart())
                            // .setMessageReference(mMessageReference)
                    .build();

            rv.setHeader("User-Agent", "k9+pEp early alpha");

            return rv;
        }
        catch (Exception e) {
            Log.e("pEp", "Could not create MimeMessage: ", e);
        };
        return null;
    }

    private static ArrayList<Attachment> createAttachments(Message m) {
        return new ArrayList<Attachment>();
    }

    public static boolean sendViaPEp(com.fsck.k9.mail.Message message) {
        Color c = PEpProviderFactory.createProvider().getPrivacyState(message);
        return (c == Color.pEpRatingFullyAnonymous ||
                c == Color.pEpRatingReliable ||
                c == Color.pEpRatingTrusted ||
                c == Color.pEpRatingTrustedAndAnonymized ||
                c == Color.pEpRatingUnreliable );
    }

    /**
     * checks wether the message given as parameter might be a message that should be piped through pEp
     * also checks wether msg has a pubkey attached
     * @param source
     * @return
     */
    public static boolean mightBePEpMessage(MimeMessage source) {
        //TODO pEp: some clever heuristics to identify possible pEp mails
        return true;
    }






    static public void dumpMimeMessage(MimeMessage mm) {
        String out = "\nRoot:\n";

        try {
            for (String header:mm.getHeaderNames())
                out += header + ": " + mm.getHeader(header) + "\n";
            out += "\n";
            out += "Message-Id: " + mm.getMessageId().hashCode() +"\n";
            out += mangleBody((MimeMultipart)mm.getBody());
            out += "hasAttachments:" + mm.hasAttachments();

        } catch (Exception e) {
            out += "\n\n" + e.getMessage();
        }

        Log.d("MIMEMESSAGE", out);

    }

    static private String mangleBody(MimeMultipart body) throws Exception {
        String rv = "Body:\n";
        for(Part p: body.getBodyParts())
            rv += "     " + new String(((BinaryMemoryBody) p).getData()) +"\n";
        //rv+="  " + ((BinaryMemoryBody) p)(((LocalBodyPart) p).getBody())).getData().toString() +"\n";

        return rv;
    }

}
