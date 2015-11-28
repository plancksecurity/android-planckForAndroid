package com.fsck.k9.pEp;

import android.util.Log;

import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.mailstore.LocalBodyPart;
import com.fsck.k9.message.SimpleMessageFormat;

import org.pEp.jniadapter.Blob;
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
    static Vector<Identity> createIdentities(Address[] adrs) {
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

            addHeaders(m, mm);
            addBody(m, mm);
            return m;
        } catch (Throwable t) {
            if(m != null) m.close();
            Log.e("pEp", "Could not create message:", t);
        }
        return null;
    }

    private static void addBody(Message m, MimeMessage mm) {
        try {
            // fiddle message txt from MimeMsg...
            MimeMultipart mmp = (MimeMultipart) mm.getBody();
            LocalBodyPart lbp = (LocalBodyPart) mmp.getBodyPart(0);
            BinaryMemoryBody bmb = (BinaryMemoryBody) lbp.getBody();
            m.setLongmsg(new String(bmb.getData(), "UTF-8"));

            //TODO: Handle pure text and multipart/alternative

            // and add attachments...
            Vector<Blob> attachments = new Vector<Blob>();
            int nrOfAttachment = mmp.getBodyParts().size();
            for (int i = 1; i < nrOfAttachment; i++) {
                BodyPart p = mmp.getBodyPart(i);

                Log.d("pep", "Bodypart #" + i + ":" + p.toString() + " Body:" + p.getBody().toString());
                if (p.getBody() instanceof BinaryMemoryBody) {
                    BinaryMemoryBody part = (BinaryMemoryBody) p.getBody();

                    // TODO: filename
                    Blob blob = new Blob();
                    blob.filename = "qwerty";
                    blob.mime_type = p.getMimeType();
                    blob.data = part.getData();
                    attachments.add(blob);
                    Log.d("pep", "BLOB #" + i + ":" + blob.mime_type);
                }
            }
            m.setAttachments(attachments);
        } catch (Exception e) {
            Log.e("pep", "creating message body", e);
        }
    }

    private static void addHeaders(Message m, MimeMessage mm) {
        try {
            // headers
            m.setFrom(createIdentity(mm.getFrom()[0]));
            m.setTo(createIdentities(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.TO)));
            m.setCc(createIdentities(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.CC)));
            m.setBcc(createIdentities(mm.getRecipients(com.fsck.k9.mail.Message.RecipientType.BCC)));
            m.setId(mm.getMessageId());
            m.setInReplyTo(createMessageReferences(mm.getReferences()));
            // m.setRecv();
            m.setSent(mm.getSentDate());
            m.setReplyTo(createIdentities(mm.getReplyTo()));
            // m.setRecvBy();
            m.setShortmsg(mm.getSubject());

            // TODO: other headers

        } catch (MessagingException me) {
            Log.e("pep", "creating message headers", me);
        }
    }

    private static Vector<String> createMessageReferences(String[] references) {
        Vector<String> rv = new Vector<String>();
        if(references != null)
            for(String s : references)
                rv.add(s);
        return rv;
    }

    static MimeMessage createMimeMessage(Message m) {
        // FIXME: are these new String()s really necessary? I think, the adapter does that already...
        com.fsck.k9.Identity me = new com.fsck.k9.Identity();
        me.setEmail(new String(m.getFrom().address));
        me.setName(new String(m.getFrom().username));
        try {

            // FIXME: the following sucks. It makes no sense, to shovel stuff from Message to the builder. But builder has to be reworked anyway.
            PEpMessageBuilder pmb = new PEpMessageBuilder()
                    .setSubject(new String(m.getShortmsg()))
                    .setTo(createAddresses(m.getTo()))
                            //    .setCc(createAddresses(m.getCc()))
                            //    .setBcc(createAddresses(m.getBcc()))
                            // .setInReplyTo(mInReplyTo)
                            // .setReferences(mReferences)
                            // .setRequestReadReceipt(mReadReceipt)
                    .setIdentity(me)
                    .setMessageFormat(SimpleMessageFormat.TEXT)             // FIXME: pEp: not only text
                    .setText(new String(m.getLongmsg()));
                    try {
                        pmb.setAttachments(m.getAttachments());
                    } catch (Exception e)  {
                        Log.e("pepdump", "during getAttachments()", e);
                        pmb.setAttachments(new Vector<Blob>());
                    }
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
            MimeMessage rv = pmb.build();

            rv.setHeader("User-Agent", "k9+pEp late alpha");

            rv.setHeader("x-pep-version","1.0");            // FIXME: remove soon

            return rv;
        }
        catch (Exception e) {
            Log.e("pepdump", "Could not create MimeMessage: ", e);
        };
        return null;
    }

    /**
     * dumps a k9 msg to log
     *
     * @param mm mesage to dump
     */
    static public void dumpMimeMessage(MimeMessage mm) {
        Log.e("pepdump", "Root:");
        try {
            for (String header:mm.getHeaderNames())
                Log.e("pepdump", header + ": " + mm.getHeader(header)[0]);

            Log.e("pepdump",  "Message-Id: " + mm.getMessageId().hashCode() );
            Log.e("pepdump", "hasAttachments:" + mm.hasAttachments());

             dumpBody(mm.getBody(), 5);

        } catch (Exception e) {
            Log.e("pepdump", "", e);
        }
    }

    static private void dumpBody(Body body, int idx) throws Exception {
        String prev = "                                                      ".substring(0, idx);
        if (!(body instanceof MimeMultipart)) {
            Log.e("pepdump", prev + "body: "+ body.toString());
            if(body instanceof BinaryMemoryBody) {
                byte[] arr = ((BinaryMemoryBody) body).getData();
                Log.e("pepdump", prev + "Blob content: >" + new String(arr).substring(0, (arr.length > 50) ? 50 : arr.length) + "<");
            }
            if(body instanceof TextBody) {
                TextBody tb = (TextBody) body;
                Log.e("pepdump", prev+ "Textbody content >" + tb.getText()+"<");
            }
            return;
        }
        try {
            MimeMultipart mmp = (MimeMultipart) body;

            Log.e("pepdump", prev + "MimeMultipart:");
            int nr = mmp.getBodyParts().size();
            for (int i = 0; i < nr; i++) {
                BodyPart p = mmp.getBodyPart(i);
                Log.e("pepdump",prev + "Bodypart: " + p.toString());
                dumpBody(p.getBody(), idx + 5);

            }
        } catch (Exception e) {
            Log.e("pepdump", "b0rgd", e);
        }
    }
}
