package com.fsck.k9.pEp;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
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

import org.apache.commons.io.IOUtils;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.inject.Inject;

/**
 * some helper stuff
 */

// FIXME: this needs cleanup. separate message builder stuff to separate classes and leave only *small* helpers here!

public class PEpUtils {
    private static final String TRUSTWORDS_SEPARATOR = " ";
    private static final int CHUNK_SIZE = 4;

    private static final CharSequence[] pEpLanguages = {"ca", "de", "es", "fr", "tr", "en"};

    public static CharSequence[] getPEpLanguages() {
        return pEpLanguages;
    }

    @Inject public PEpUtils() {
    }

    public static Vector<Identity> createIdentities(List<Address> addressList, Context context) {
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
                if (identity.getEmail().equalsIgnoreCase(adr.getAddress())) {
                    return true;
                }
            }
        }
        return false;

    }

    private static boolean isMyself(Context context, Identity adr) {
        Preferences prefs = Preferences.getPreferences(context.getApplicationContext());
        Collection<Account> accounts = prefs.getAvailableAccounts();
        for (Account account : accounts) {
            List<com.fsck.k9.Identity> identities = account.getIdentities();
            for (com.fsck.k9.Identity identity : identities) {
                if (identity.getEmail().equalsIgnoreCase(adr.address)) {
                    return true;
                }
            }
        }
        return false;
    }

    static Address[] createAddresses(Vector<Identity> ids) {
        if (ids == null) return null;                // this should be consistent with pep api
        Address[] rv = new Address[ids.size()];
        int idx = 0;
        for (Identity i : ids)
            rv[idx++] = createAddress(i);

        return rv;
    }

    static List<Address> createAddressesList(Vector<Identity> ids) {
        if (ids == null) return Collections.emptyList();                // this should be consistent with pep api
        List<Address> rv = new ArrayList<>(ids.size());
        int idx = 0;
        for (Identity i : ids)
            rv.add(createAddress(i));

        return rv;
    }

    static Address createAddress(Identity id) {
        Address adr = new Address(id.address, id.username);
        // Address() parses the address, eventually not setting it, therefore just a little sanity...
        // TODO: pEp check what happens if id.address == null beforehand
        if (adr.getAddress() == null && id.address != null)
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

    public static String getShortTrustWords(PEpProvider pEp, Identity id, String... languages) {
        if (languages.length == 0) {
            String k9Language = K9.getK9Language();
            return obtainTrustwords(pEp, id, k9Language, true);
        } else {
            String language = languages[0];
            return obtainTrustwords(pEp, id, language, true);
        }
    }

    public static String getTrustWords(PEpProvider pEp, Identity id, String... languages) {
        if (languages.length == 0) {
            String k9Language = K9.getK9Language();
            return obtainTrustwords(pEp, id, k9Language, false);
        } else {
            String language = languages[0];
            return obtainTrustwords(pEp, id, language, false);
        }
    }

    @NonNull
    private static String obtainTrustwords(PEpProvider pEp, Identity id, String language, Boolean shouldBeShorten) {
        if (language == null || language.equals("")) {
            language = Locale.getDefault().getLanguage();
        }
        if (isLanguageInPEPLanguages(language)) {
            if (shouldBeShorten) {
                return getShortTrustwords(pEp.trustwords(id, language));
            } else {
                return pEp.trustwords(id, language);
            }
        } else {
            if (shouldBeShorten) {
                return getShortTrustwords(pEp.trustwords(id, "en"));
            } else {
                return pEp.trustwords(id, "en");
            }
        }
    }


    @NonNull
    public static String obtainTrustwordsLang(String language) {
        if (language == null || language.equals("")) {
            language = Locale.getDefault().getLanguage();
        }
        if (isLanguageInPEPLanguages(language)) {
            return language;
        } else {
            return  "en";
        }
    }
    private static boolean isLanguageInPEPLanguages(String language) {
        for (CharSequence pEpLanguage : pEpLanguages) {
            if (pEpLanguage.equals(language)) {
                return true;
            }
        }
        return false;
    }


    public static String ratingToString(Rating rating) {
        switch (rating) {
            case pEpRatingCannotDecrypt:
                return "cannot_decrypt";
            case pEpRatingHaveNoKey:
                return "have_no_key";
            case pEpRatingUnencrypted:
                return "unencrypted";
            case pEpRatingUnencryptedForSome:
                return "unencrypted_for_some";
            case pEpRatingUnreliable:
                return "unreliable";
            case pEpRatingReliable:
                return "reliable";
            case pEpRatingTrusted:
                return "trusted";
            case pEpRatingTrustedAndAnonymized:
                return "trusted_and_anonymized";
            case pEpRatingFullyAnonymous:
                return "fully_anonymous";
            case pEpRatingMistrust:
                return "mistrust";
            case pEpRatingB0rken:
                return "b0rken";
            case pEpRatingUnderAttack:
                return "under_attack";
            default:
                return "undefined";
        }
    }

    public static Rating stringToRating(String rating) {
        if (rating.equalsIgnoreCase("cannot_decrypt")
                || rating.equalsIgnoreCase("pEpRatingCannotDecrypt")) {
            return Rating.pEpRatingCannotDecrypt;
        }
        if (rating.equalsIgnoreCase("have_no_key")
                || rating.equalsIgnoreCase("pEpRatingHaveNoKey")) {
            return Rating.pEpRatingHaveNoKey;
        }
        if (rating.equalsIgnoreCase("unencrypted")
                || rating.equalsIgnoreCase("pEpRatingUnencrypted")) {
            return Rating.pEpRatingUnencrypted;
        }
        if (rating.equalsIgnoreCase("unencrypted_for_some")
                || rating.equalsIgnoreCase("pEpRatingUnencryptedForSome")) {
            return Rating.pEpRatingUnencryptedForSome;
        }
        if (rating.equalsIgnoreCase("unreliable")
                || rating.equalsIgnoreCase("pEpRatingUnreliable")) {
            return Rating.pEpRatingUnreliable;
        }
        if (rating.equalsIgnoreCase("reliable")
                || rating.equalsIgnoreCase("pEpRatingReliable")) {
            return Rating.pEpRatingReliable;
        }
        if (rating.equalsIgnoreCase("trusted")
                || rating.equalsIgnoreCase("pEpRatingTrusted")) {
            return Rating.pEpRatingTrusted;
        }
        if (rating.equalsIgnoreCase("trusted_and_anonymized")
                || rating.equalsIgnoreCase("pEpRatingTrustedAndAnonymized")) {
            return Rating.pEpRatingTrustedAndAnonymized;
        }
        if (rating.equalsIgnoreCase("fully_anonymous")
                || rating.equalsIgnoreCase("pEpRatingFullyAnonymous")) {
            return Rating.pEpRatingFullyAnonymous;
        }
        if (rating.equalsIgnoreCase("mistrust")
                || rating.equalsIgnoreCase("pEpRatingMistrust")) {
            return Rating.pEpRatingMistrust;
        }
        if (rating.equalsIgnoreCase("b0rken")
                || rating.equalsIgnoreCase("pEpRatingB0rken")) {
            return Rating.pEpRatingB0rken;
        }
        if (rating.equalsIgnoreCase("under_attack")
                || rating.equalsIgnoreCase("pEpRatingUnderAttack")) {
            return Rating.pEpRatingUnderAttack;
        }
        return Rating.pEpRatingUndefined;

    }

    public static int getRatingColor(Rating rating, Context context) {
        // TODO: 02/09/16 PEP_color color_from_rating(PEP_rating rating) from pEpEngine;

        if (rating == null || rating.equals(Rating.pEpRatingB0rken)
                || rating.equals(Rating.pEpRatingHaveNoKey)) {
            return ContextCompat.getColor(context, R.color.pep_no_color);
        }

        if (rating.value < Rating.pEpRatingUndefined.value) {
            return ContextCompat.getColor(context, R.color.pep_red);
        }

        if (rating.value < Rating.pEpRatingReliable.value) {
            return ContextCompat.getColor(context, R.color.pep_no_color);
        }

        if (rating.value < Rating.pEpRatingTrusted.value) {
            return ContextCompat.getColor(context, R.color.pep_yellow);
        }

        if (rating.value >= Rating.pEpRatingTrusted.value) {
            return ContextCompat.getColor(context, R.color.pep_green);
        }

        /*
        if (rating == PEP_rating_b0rken || rating == PEP_rating_have_no_key)
1892	        return PEP_color_no_color;
1893
1894	    if (rating < PEP_rating_undefined)
1895	        return PEP_color_red;
1896
1897	    if (rating < PEP_rating_reliable)
1898	        return PEP_color_no_color;
1899
1900	    if (rating < PEP_rating_trusted)
1901	        return PEP_color_yellow;
1902
1903	    if (rating >= PEP_rating_trusted)
1904	        return PEP_color_green;
         */

        throw new RuntimeException("Invalid rating");
    }

    public static void colorActionBar(PePUIArtefactCache pEpUiCache, ActionBar actionBar, Rating rating) {
        if (actionBar != null) {
            ColorDrawable colorDrawable = new ColorDrawable(pEpUiCache.getColor(rating));
            actionBar.setBackgroundDrawable(colorDrawable);
        }
    }

    public static Rating extractRating(Message message) {
        String[] pEpRating;
        pEpRating = message.getHeader(MimeHeader.HEADER_PEP_RATING);
        if (pEpRating.length > 0)
            return PEpUtils.stringToRating(pEpRating[0]);
        else
            return Rating.pEpRatingUndefined;
    }

    public static String formatFpr(String fpr) {
        int requiredSeparators = fpr.length() / CHUNK_SIZE;
        char[] fprChars = new char[fpr.length() + requiredSeparators];
        int sourcePosition = 0;
        for (int destPosition = 0; destPosition < fprChars.length - 1; destPosition++) {
            if (sourcePosition % CHUNK_SIZE == 0
                    && destPosition > 0
                    && fprChars[destPosition - 1] != TRUSTWORDS_SEPARATOR.charAt(0)) {
                fprChars[destPosition] = TRUSTWORDS_SEPARATOR.charAt(0);
            } else {
                fprChars[destPosition] = fpr.charAt(sourcePosition);
                ++sourcePosition;
            }

        }
        fprChars[fprChars.length / 2 - 1] = '\n';
        return String.valueOf(fprChars);
    }

    public static boolean ispEpDisabled(Account account, Message message, Rating messageRating) {
        return message.isSet(Flag.X_PEP_DISABLED)
                || messageRating == Rating.pEpRatingUndefined
                || !account.ispEpPrivacyProtected();
    }

    public static void pEpGenerateAccountKeys(Context context, Account account) {
        PEpProvider pEp = PEpProviderFactory.createAndSetupProvider(context);
        org.pEp.jniadapter.Identity myIdentity = PEpUtils.createIdentity(new Address(account.getEmail(), account.getName()), context);
        pEp.myself(myIdentity);
        pEp.close();
    }

    public static ArrayList<Identity> filterRecipients(Context context, ArrayList<Identity> recipients) {
        ArrayList<Identity> identities = new ArrayList<>();

        Collections.sort(recipients, new Comparator<Identity>() {
            @Override
            public int compare(Identity left, Identity right) {
                return left.address.compareTo(right.address);
            }
        });

        for (int i = 0; i < recipients.size(); i++) {
            Identity identity = recipients.get(i);
            if (!isMyself(context, identity)) {
                if (identities.size() == 0) {
                    identities.add(identity);
                } else {
                    Identity previousIdentity = recipients.get(i - 1);
                    if (!previousIdentity.address.equals(identity.address)) {
                        identities.add(identity);
                    }
                }
            }
        }
        return identities;
    }

    public static String clobberVector(Vector<String> sv) {   // FIXME: how do revs come out of array? "<...>" or "...."?
        String rt = "";
        if (sv != null)
            for (String cur : sv)
                rt += cur + "; ";
        return rt;
    }

    public static String getReplyTo(Address[] replyTo) {
        List<String> addresses = new ArrayList<>(replyTo.length);
        for (Address address : replyTo) {
            addresses.add(address.toString());
        }
        return clobberVector(addresses);
    }

    private static String clobberVector(List<String> sv) {
        String rt = "";
        if (sv != null)
            for (String cur : sv)
                rt += cur + "; ";
        return rt;
    }

    public static Drawable getDrawableForRating(Context context, Rating rating) {
        if (rating.value != Rating.pEpRatingMistrust.value
                && rating.value < Rating.pEpRatingReliable.value) {
            return context.getResources().getDrawable(R.drawable.pep_status_gray);
        }else if (rating.value == Rating.pEpRatingMistrust.value) {
            return context.getResources().getDrawable(R.drawable.pep_status_red);
        } else if (rating.value >= Rating.pEpRatingTrusted.value){
            return context.getResources().getDrawable(R.drawable.pep_status_green);
        } else if (rating.value == Rating.pEpRatingReliable.value){
            return context.getResources().getDrawable(R.drawable.pep_status_yellow);
        }
        return context.getResources().getDrawable(R.drawable.pep_status_gray);
    }

    public static Drawable getDrawableForRatingRecipient(Context context, Rating rating) {
        if (rating.value != Rating.pEpRatingMistrust.value
                && rating.value < Rating.pEpRatingReliable.value) {
            return context.getResources().getDrawable(R.drawable.pep_status_gray_white);
        }else if (rating.value == Rating.pEpRatingMistrust.value) {
            return context.getResources().getDrawable(R.drawable.pep_status_red_white);
        } else if (rating.value >= Rating.pEpRatingTrusted.value){
            return context.getResources().getDrawable(R.drawable.pep_status_green_white);
        } else if (rating.value == Rating.pEpRatingReliable.value){
            return context.getResources().getDrawable(R.drawable.pep_status_yellow_white);
        }
        return context.getResources().getDrawable(R.drawable.pep_status_gray_white);
    }
}

