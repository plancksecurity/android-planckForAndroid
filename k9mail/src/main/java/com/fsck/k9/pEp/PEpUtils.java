package com.fsck.k9.pEp;

import android.util.Log;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.mailstore.LocalBodyPart;
import org.apache.commons.io.IOUtils;
import org.pEp.jniadapter.Blob;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * some helper stuff
 *
 */

// FIXME: this needs cleanup. separate message builder stuff to separate classes and leave only *small* helpers here!

public class PEpUtils {
    static Vector<Identity> createIdentities(Address[] adrs) {
        Vector<Identity> rv = new Vector<Identity>(adrs.length);
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
        if(ids == null) return null;                // this should be consistent with pep api
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
    }

    private static Vector<String> createMessageReferences(String[] references) {
        Vector<String> rv = new Vector<String>();
        if(references != null)
            for(String s : references)
                rv.add(s);
        return rv;
    }


    /**
     * dumps a k9 msg to log
     *
     * @param mm mesage to dump
     */
    static public void dumpMimeMessage(String tag, MimeMessage mm) {
        Log.e("pepdump", "Root:");
        try {
            for (String header:mm.getHeaderNames())
                Log.e("pepdump", tag + " " + header + ": " + mm.getHeader(header)[0]);

            // Log.e("pepdump",  "Message-Id: " + mm.getMessageId().hashCode() );
            Log.e("pepdump", tag + " hasAttachments:" + mm.hasAttachments());

             dumpBody(tag, mm.getBody(), 5);

        } catch (Exception e) {
            Log.e("pepdump", tag + " b0rged", e);
        }
    }

    static private void dumpBody(String tag, Body body, int idx) throws Exception {
        String prev = tag + "                                                      ".substring(0, idx);
        if (!(body instanceof MimeMultipart)) {
            Log.e("pepdump", prev + "body: "+ body.toString());
            if(body instanceof BinaryMemoryBody) {
                byte[] arr = extractBodyContent(body);
                Log.e("pepdump", prev + "Blob content: >" + startOf(new String(arr), 100)  + "<");
            }
            if(body instanceof TextBody) {
                TextBody tb = (TextBody) body;
                Log.e("pepdump", prev+ "Textbody content >" + startOf(tb.getText(),100)+"<");
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
                dumpBody(tag, p.getBody(), idx + 5);

            }
        } catch (Exception e) {
            Log.e("pepdump", tag + " b0rgd", e);
        }
    }

    static public String startOf(String s, int length) {
        String rv = s.substring(0, (s.length() > length) ? length : s.length());
        return rv.replace("\n", "<nl>").replace("\r", "<cr>");
    }

    static byte[] extractBodyContent(Body body) throws MessagingException, IOException {
        InputStream is = MimeUtility.decodeBody(body);
        byte[] rv = IOUtils.toByteArray(is);
        is.close();

        return rv;
    }
}
