package com.fsck.k9.pEp;

import android.util.Log;

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
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;

import java.util.Vector;

/**
 * some helper stuff
 *
 */

// FIXME: this needs cleanup. separate message builder stuff to separate classes and leave only *small* helpers here!

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

            // Log.e("pepdump",  "Message-Id: " + mm.getMessageId().hashCode() );
            Log.e("pepdump", "hasAttachments:" + mm.hasAttachments());

             dumpBody(mm.getBody(), 5);

        } catch (Exception e) {
            Log.e("pepdump", "b0rged", e);
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
