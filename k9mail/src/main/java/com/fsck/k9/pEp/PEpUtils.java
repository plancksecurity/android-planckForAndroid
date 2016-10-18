package com.fsck.k9.pEp;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalMessage;

import org.apache.commons.io.IOUtils;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * some helper stuff
 */

// FIXME: this needs cleanup. separate message builder stuff to separate classes and leave only *small* helpers here!

public class PEpUtils {
    private static final String TRUSTWORDS_SEPARATOR = " ";
    private static final int CHUNK_SIZE = 4;

    public static Vector<Identity> createIdentities(List <Address> addressList, Context context) {
        Vector<Identity> rv = new Vector<>(addressList.size());
        for (Address adr : addressList)
            rv.add(createIdentity(adr, context));
        return rv;
    }

    public static Identity createIdentity(Address adr, Context context) {
        Identity id = new Identity();
        id.address = adr.getAddress();
        id.username = adr.getAddress();
        if (adr.getPersonal() != null) {
            id.username = adr.getPersonal();
        }


        if (isMyself(context, adr)) {
            id.user_id = PEpProvider.PEP_OWN_USER_ID;
            return id;
        }
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

    private static boolean isMyself(Context context, Address adr) {
        Preferences prefs = Preferences.getPreferences(context.getApplicationContext());
        Collection<Account> accounts = prefs.getAvailableAccounts();
        for (Account account : accounts) {
            List<com.fsck.k9.Identity> identities = account.getIdentities();
            for (com.fsck.k9.Identity identity : identities) {
                if (identity.getEmail().equals(adr.getAddress())) {
                    return true;
                }
            }
        }
        return false;
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


//    /**
//     * dumps a k9 msg to log
//     *
//     * @param mm mesage to dump
//     */
//    static public void dumpMimeMessage(String tag, MimeMessage mm) {
//        Log.i("pepdump", "Root:");
//        try {
//            for (String header:mm.getHeaderNames())
//                Log.i("pepdump", tag + " " + header + ": " + mm.getHeader(header)[0]);
//
//            // Log.e("pepdump",  "Message-Id: " + mm.getMessageId().hashCode() );
//            Log.i("pepdump", tag + " hasAttachments:" + mm.hasAttachments());
//
//             dumpBody(tag, mm.getBody(), 5);
//
//        } catch (Exception e) {
//            Log.e("pepdump", tag + " b0rged", e);
//        }
//    }

//    static private void dumpBody(String tag, Body body, int idx) throws Exception {
//        String prev = tag + "                                                                                                                                                        ".substring(0, idx);
//        if(body==null) {
//            Log.i("pepdump", "null body");
//            return;
//        }
//        if (!(body instanceof MimeMultipart)) {
//            Log.i("pepdump", prev + "body: "+ body.toString());
//            if(body instanceof BinaryMemoryBody) {
//                byte[] arr = extractBodyContent(body);
//                Log.i("pepdump", prev + "Blob content: >" + startOf(new String(arr), 100)  + "<");
//            }
//            if(body instanceof TextBody) {
//                TextBody tb = (TextBody) body;
//                Log.i("pepdump", prev+ "Textbody content >" + startOf(tb.getRawText(),100)+"<");
//            }
//            return;
//        }
//        try {
//            MimeMultipart mmp = (MimeMultipart) body;
//
//            Log.i("pepdump", prev + "MimeMultipart:");
//            int nr = mmp.getBodyParts().size();
//            for (int i = 0; i < nr; i++) {
//                BodyPart p = mmp.getBodyPart(i);
//                Log.i("pepdump",prev + "Bodypart: " + p.toString());
//                dumpBody(tag, p.getBody(), idx + 5);
//
//            }
//        } catch (Exception e) {
//            Log.e("pepdump", tag + " b0rgd", e);
//        }
//    }

//    static private String startOf(String s, int length) {
//        String rv = s.substring(0, (s.length() > length) ? length : s.length());
//        return rv.replace("\n", "<nl>").replace("\r", "<cr>");
//    }

    static byte[] extractBodyContent(Body body) throws MessagingException, IOException {
        InputStream is = MimeUtility.decodeBody(body);
        if (is != null) {
            byte[] rv = IOUtils.toByteArray(is);
            is.close();
            return rv;
        }
        return new ByteArrayOutputStream().toByteArray();
    }

    private static String getShortTrustwords(String trustwords) {
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
        return getShortTrustwords(pEp.trustwords(id, "es"));
    }


    public static int getRatingColor(Rating rating, Context context) {
        // TODO: 02/09/16 PEP_color color_from_rating(PEP_rating rating) from pEpEngine;

        if (rating == null || rating.equals(Rating.pEpRatingB0rken)
                || rating.value < Rating.pEpRatingReliable.value) {
            return ContextCompat.getColor(context, R.color.pep_no_color);
        }

        if (rating.value < Rating.pEpRatingUndefined.value) {
            return ContextCompat.getColor(context, R.color.pep_red);
        }

        if (rating.value < Rating.pEpRatingTrusted.value) {
            return  ContextCompat.getColor(context, R.color.pep_yellow);
        }

        if (rating.value >= Rating.pEpRatingTrusted.value) {
            return  ContextCompat.getColor(context, R.color.pep_green);
        }
        throw new RuntimeException("Invalid rating");
    }

    public static int getToolbarRatingColor(Rating rating, Context context) {
        // TODO: 02/09/16 PEP_color color_from_rating(PEP_rating rating) from pEpEngine;

        if (rating == null || rating.equals(Rating.pEpRatingB0rken)
                || rating.value < Rating.pEpRatingReliable.value) {
            return ContextCompat.getColor(context, R.color.gray_toolbar);
        }

        if (rating.value < Rating.pEpRatingUndefined.value) {
            return ContextCompat.getColor(context, R.color.red_toolbar);
        }

        if (rating.value < Rating.pEpRatingTrusted.value) {
            return  ContextCompat.getColor(context, R.color.yellow_toolbar);
        }

        if (rating.value >= Rating.pEpRatingTrusted.value) {
            return  ContextCompat.getColor(context, R.color.green_toolbar);
        }
        throw new RuntimeException("Invalid rating");
    }

    public static Rating extractRating(Message message) {
        String[] pEpRating;
        pEpRating = message.getHeader(MimeHeader.HEADER_PEP_RATING);
        if(pEpRating.length > 0)
            return Rating.valueOf(pEpRating[0]);
        else
            return Rating.pEpRatingUndefined;
    }

    public static String formatFpr(String fpr) {
        int requiredSeparators = fpr.length() / CHUNK_SIZE;
        char[] fprChars = new char[fpr.length() + requiredSeparators];
        int sourcePosition = 0;
        for (int destPosition = 0; destPosition < fprChars.length-1; destPosition++) {
            if (sourcePosition % CHUNK_SIZE == 0
                    && destPosition > 0
                    && fprChars[destPosition-1] != TRUSTWORDS_SEPARATOR.charAt(0)) {
                fprChars[destPosition] = TRUSTWORDS_SEPARATOR.charAt(0);
            }
            else {
                fprChars[destPosition] = fpr.charAt(sourcePosition);
                ++sourcePosition;
            }

        }
        fprChars[fprChars.length/2-1] = '\n';
        return String.valueOf(fprChars);
    }

    public static boolean ispEpDisabled(Account account, LocalMessage message, Rating messageRating) {
        return message.isSet(Flag.X_FORCE_UNENCRYPTED)
                || messageRating == Rating.pEpRatingUndefined
                || !account.ispEpPrivacyProtected();
    }

    public static void colorToolbar(PePUIArtefactCache uiCache, Toolbar toolbar, Rating pEpRating) {
        if (toolbar != null) {
            toolbar.setBackgroundColor(uiCache.getToolbarColor(pEpRating));
        }
    }

    public static void colorToolbar(PePUIArtefactCache uiCache, ActionBar supportActionBar, Rating pEpRating) {
        ColorDrawable colorDrawable = new ColorDrawable(uiCache.getToolbarColor(pEpRating));
        supportActionBar.setBackgroundDrawable(colorDrawable);
    }

    public static void colorToolbar(PePUIArtefactCache uiCache, Toolbar toolbar, int colorResource) {
        if (toolbar != null) {
            toolbar.setBackgroundColor(colorResource);
        }
    }
}

