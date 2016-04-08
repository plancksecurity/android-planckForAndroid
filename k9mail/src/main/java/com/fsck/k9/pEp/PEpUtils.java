package com.fsck.k9.pEp;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import org.apache.commons.io.IOUtils;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * some helper stuff
 */

// FIXME: this needs cleanup. separate message builder stuff to separate classes and leave only *small* helpers here!

public class PEpUtils {
    private static final String TRUSTWORDS_SEPARATOR = " ";

    public static Vector<Identity> createIdentities(Address[] adrs, Context context) {
        Vector<Identity> rv = new Vector<Identity>(adrs.length);
        for (Address adr : adrs)
            rv.add(createIdentity(adr, context));
        return rv;
    }

    public static Identity createIdentity(Address adr, Context context) {
        Identity id = new Identity();
        id.address = adr.getAddress();
        id.username = adr.getAddress();
//        if (adr.getPersonal() != null) {
//            id.username = adr.getPersonal();
//        } else id.us
        try {
            id.user_id = Contacts.getInstance(context).getContactId(adr.getAddress());
        } catch (Exception e) {
            id.user_id = adr.getAddress();
        }
//        PEpProvider provider = PEpProviderFactory.createProvider();
//        id = provider.updateIdentity(id);
//        provider.close();
//        provider = null;
        // hack to get an unique ID...

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
        String prev = tag + "                                                                                                                                                        ".substring(0, idx);
        if(body==null) {
            Log.e("pepdump", "null body");
            return;
        }
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
        if (is != null) {
            byte[] rv = IOUtils.toByteArray(is);
            is.close();
            return rv;
        }
        return new ByteArrayOutputStream().toByteArray();
    }

    public static String getShortTrustwords(String trustwords) {
        StringBuilder builder = new StringBuilder();
        String[] trustArray = trustwords.split(TRUSTWORDS_SEPARATOR);

        if (trustArray.length > 5) {
            for (int i = 0; i < 5; i++) {
                builder.append(trustArray[i]);
                builder.append(TRUSTWORDS_SEPARATOR);
            }
        }
        return builder.toString();
    }

    public static String getShortTrustWords(PEpProvider pEp, Identity id) {
        return getShortTrustwords(pEp.trustwords(id));
    }


    public static int getColorColor(Color pepColor, Resources resources) {
        if (pepColor.value <= Color.pEpRatingRed.value) {
            return resources.getColor(R.color.pep_red);
        } else if (pepColor.value < Color.pEpRatingYellow.value) {
            return resources.getColor(R.color.pep_gray);
        } else if (pepColor.value < Color.pEpRatingGreen.value) {
            return resources.getColor(R.color.pep_yellow);
        } else {
            return resources.getColor(R.color.pep_green);
        }
    }

    public static void colorActionBar(PePUIArtefactCache pEpUiCache, ActionBar actionBar, Color mPEpColor) {
        if (actionBar != null) {
            ColorDrawable colorDrawable = new ColorDrawable(pEpUiCache.getColor(mPEpColor));
            actionBar.setBackgroundDrawable(colorDrawable);
        }
    }
}

