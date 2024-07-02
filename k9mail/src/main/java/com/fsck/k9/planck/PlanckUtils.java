package com.fsck.k9.planck;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.WorkerThread;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.SimpleMessageFormat;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import foundation.pEp.jniadapter.CommType;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;
import security.planck.ui.PassphraseProvider;

/**
 * some helper stuff
 */

// FIXME: this needs cleanup. separate message builder stuff to separate classes and leave only *small* helpers here!

public class PlanckUtils {
    private static final String TRUSTWORDS_SEPARATOR = " ";
    private static final int CHUNK_SIZE = 4;

    private static final List<String> planckLanguages = Arrays.asList("en", "de");

    public static List<String> getPlanckLocales() {
        return planckLanguages;
    }

    public static boolean trustWordsAvailableForLang(String trustwordsLanguage) {
        return planckLanguages.contains(trustwordsLanguage);
    }

    public static Vector<Identity> createIdentities(List<Address> addressList, Context context) {
        Vector<Identity> rv = new Vector<>(addressList.size());
        for (Address adr : addressList)
            if (adr.getAddress() != null) {
                rv.add(createIdentity(adr, context));
            }
        return rv;
    }

    // TODO remove Context
    public static Identity createIdentity(Address adr, Context context) {
        Identity id = new Identity();
        if (adr.getAddress() != null) {
            id.address = adr.getAddress().toLowerCase();
            id.username = adr.getAddress().toLowerCase();
        }
        if (adr.getPersonal() != null) {
            id.username = adr.getPersonal();
        }
        if (isMyself(context, adr)) {
            id.user_id = PlanckProvider.PLANCK_OWN_USER_ID;
            id.me = true;
            return id;
        }

        // TODO: 15/03/18 Avoid managing user id (delegate on the engine) until we have our own address book.
//        try {
//            id.user_id = Contacts.getInstance(context).getContactId(adr.getAddress());
//        } catch (Exception ignore) {
//            //we ignore it as we decided to do nothing if the user is not found,
//            // or there is no permission
//        }
////        PEpProvider provider = PEpProviderFactory.createProvider();
////        id = provider.updateIdentity(id);
////        provider.close();
////        provider = null;
//        // hack to get an unique ID...
//
//        // TODO: do I have any kind of unique id for user_id? (no, I don't, see hack from above)
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
        if (ids == null)
            return Collections.emptyList();                // this should be consistent with pep api
        List<Address> rv = new ArrayList<>(ids.size());
        int idx = 0;
        for (Identity i : ids)
            rv.add(createAddress(i));

        return rv;
    }

    static Address createAddress(Identity id) {
        Address adr = new Address("", id.username);
        if (id.address != null) {
            adr = new Address(id.address, id.username);
        }
        // Address() parses the address, eventually not setting it, therefore just a little sanity...
        // TODO: pEp check what happens if id.address == null beforehand
        if (adr.getAddress() == null && id.address != null)
            throw new RuntimeException(
                    "Could not convert Identiy.address " + id.address + " to Address."
            );
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

    static byte[] extractBodyContent(
            Body body,
            boolean deleteTempFile
    ) throws MessagingException, IOException {
        InputStream is = MimeUtility.decodeBody(body, deleteTempFile);
        if (is != null) {
            byte[] rv = IOUtils.toByteArray(is);
            if (deleteTempFile) {
                // Forcing the file to be erased
                body.writeTo(new ByteArrayOutputStream());
            }

            is.close();
            return rv;
        } else if (body instanceof TextBody) {
            return ((TextBody) body).getRawText().getBytes();
        }
        return new ByteArrayOutputStream().toByteArray();
    }

    public static String ratingToString(Rating rating) {
        if (rating == null) {
            return "undefined";
        }
        switch (rating) {
            case pEpRatingCannotDecrypt:
                return "cannot_decrypt";
            case pEpRatingHaveNoKey:
                return "have_no_key";
            case pEpRatingUnencrypted:
                return "unencrypted";
            case pEpRatingUnreliable:
                return "unreliable";
            case pEpRatingMediaKeyProtected:
                return "media_key_protected";
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
        if (rating == null) {
            return Rating.pEpRatingUndefined;
        }
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
        if (rating.equalsIgnoreCase("unreliable")
                || rating.equalsIgnoreCase("pEpRatingUnreliable")) {
            return Rating.pEpRatingUnreliable;
        }
        if (rating.equalsIgnoreCase("media_key_protected")
                || rating.equalsIgnoreCase("pEpRatingMediaKeyProtected")) {
            return Rating.pEpRatingMediaKeyProtected;
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

    public static Rating extractRating(Message message) {
        String[] pEpRating;
        pEpRating = message.getHeader(MimeHeader.HEADER_PEP_RATING);
        if (pEpRating.length > 0)
            return PlanckUtils.stringToRating(pEpRating[0]);
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

    public static boolean ispEpDisabled(Account account, Rating messageRating) {
        return messageRating == Rating.pEpRatingUndefined
                || !account.isPlanckPrivacyProtected();
    }

    public static boolean isMessageToEncrypt(Account account, Rating messageRating, boolean isForceUnencrypted) {
        return messageRating.value >= Rating.pEpRatingReliable.value
                && account.isPlanckPrivacyProtected()
                && !isForceUnencrypted;
    }

    @WorkerThread
    public static void pEpGenerateAccountKeys(Context context, Account account) {
        K9 app = (K9) context;
        PlanckProvider pEp = app.planckProvider;
        foundation.pEp.jniadapter.Identity myIdentity = PlanckUtils.createIdentity(new Address(account.getEmail(), account.getName()), context);
        PassphraseProvider.setNewAccount(account.getEmail());
        try {
            myIdentity = pEp.myself(myIdentity);
        } finally {
            PassphraseProvider.resetNewAccount();
        }
        updateSyncFlag(account, pEp, myIdentity);

        // As global sync cannot be enabled if there is no enabled account, we disable it if we only
        // have one account an disabled sync on it
        // If we add an account with sync enabled, we enable sync globally if it was not already enabled
        if (!account.isPlanckSyncEnabled()
                && Preferences.getPreferences(context).getAccounts().size() == 1) {
            app.setPlanckSyncEnabled(false);
        } else if (account.isPlanckSyncEnabled() && !K9.isPlanckSyncEnabled()) {
            app.setPlanckSyncEnabled(true);
        } else {
            app.pEpInitSyncEnvironment();
        }
        pEp.close();
    }

    private static void updateSyncFlag(Account account, PlanckProvider pEp, Identity myIdentity) {
        pEp.setIdentityFlag(myIdentity, account.isPlanckSyncEnabled());
    }

    public static void updateSyncFlag(Context context, Account account, PlanckProvider pEp) {
        Identity id = createIdentity(new Address(account.getEmail(), account.getName()), context);
        pEp.setIdentityFlag(id, account.isPlanckSyncEnabled());
    }

    public static ArrayList<Identity> filterRecipients(Account account, ArrayList<Identity> recipients) {
        ArrayList<Identity> identities = new ArrayList<>();

        Collections.sort(recipients, (left, right) -> left.address.compareTo(right.address));

        for (int i = 0; i < recipients.size(); i++) {
            Identity identity = recipients.get(i);
            if (!identity.address.equalsIgnoreCase(account.getEmail())) {
                if (identities.size() == 0) {
                    identities.add(identity);
                } else {
                    Identity previousIdentity = recipients.get(i - 1);
                    if (!previousIdentity.address.equalsIgnoreCase(identity.address)) {
                        identities.add(identity);
                    }
                }
            }
        }
        return identities;
    }

    public static String clobberVector(Vector<String> sv) {   // FIXME: how do revs come out of array? "<...>" or "...."?
        StringBuilder rt = new StringBuilder();
        if (sv != null)
            for (String cur : sv)
                rt.append(cur).append("; ");
        return rt.toString();
    }

    public static String getReplyTo(Address[] replyTo) {
        List<String> addresses = new ArrayList<>(replyTo.length);
        for (Address address : replyTo) {
            addresses.add(address.toString());
        }
        return clobberVector(addresses);
    }

    private static String clobberVector(List<String> sv) {
        StringBuilder rt = new StringBuilder();
        if (sv != null)
            for (String cur : sv)
                rt.append(cur).append("; ");
        return rt.toString();
    }

    public static String addressesToString(Address[] addresses) {
        StringBuilder addressesText = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            if (i < addresses.length - 1) {
                addressesText.append(addresses[i].getAddress()).append(", ");
            } else {
                addressesText.append(addresses[i].getAddress());
            }
        }
        return addressesText.toString();
    }

    public static Boolean isPEpUser(Identity identity) {
        return !identity.comm_type.equals(CommType.PEP_ct_OpenPGP)
                && !identity.comm_type.equals(CommType.PEP_ct_OpenPGP_unconfirmed)
                && !identity.comm_type.equals(CommType.PEP_ct_OpenPGP_weak)
                && !identity.comm_type.equals(CommType.PEP_ct_OpenPGP_weak_unconfirmed);
    }

    public static Pair<CharSequence[], CharSequence[]> getPlanckLanguages(PlanckProvider planckProvider) {
        Map<String, PlanckLanguage> languages = planckProvider.obtainLanguages();
        Set<String> pEpLocales = languages.keySet();
        List<CharSequence> languagesToShow = new ArrayList<>();
        for (String pEpLocale : pEpLocales) {
            PlanckLanguage language = languages.get(String.valueOf(pEpLocale));
            if (language != null) {
                languagesToShow.add(language.getLanguage());
            }
        }
        CharSequence[] localesToReturn = new CharSequence[pEpLocales.size()];
        CharSequence[] languagesToReturn = new CharSequence[languagesToShow.size()];
        return new Pair<>(pEpLocales.toArray(localesToReturn),
                languagesToShow.toArray(languagesToReturn));
    }

    public static boolean isMessageOnOutgoingFolder(Message message, Account account) {
        return message.getFolder().getName().equals(account.getSentFolderName())
                || message.getFolder().getName().equals(account.getDraftsFolderName())
                || message.getFolder().getName().equals(account.getOutboxFolderName());
    }

    public static boolean shouldUseOutgoingRating(Message message, Account account, Rating rating) {
        return isMessageOnOutgoingFolder(message, account) && !isRatingUnsecure(rating);
    }

    public static Message generateKeyImportRequest(Context context, PlanckProvider pEp, Account account,
                                                   boolean ispEp, boolean encrypted) throws MessagingException {
        foundation.pEp.jniadapter.Message result;
        result = new foundation.pEp.jniadapter.Message();
        Address address = new Address(account.getEmail());
        Identity identity = createIdentity(address, context);
        identity = pEp.myself(identity);
        result.setFrom(identity);
        result.setTo(new Vector<>(Collections.singletonList(identity)));
        ArrayList<foundation.pEp.jniadapter.Pair<String, String>> fields = new ArrayList<>();
        fields.add(new foundation.pEp.jniadapter.Pair<>(MimeHeader.HEADER_PEP_AUTOCONSUME_LEGACY, "yes"));

        result.setSent(new Date(System.currentTimeMillis()));
        result.setEncFormat(foundation.pEp.jniadapter.Message.EncFormat.None);

        MimeMessageBuilder builder = new MimeMessageBuilder(result).newInstance();

        builder = (MimeMessageBuilder) builder.setSentDate(new Date())
                .setHideTimeZone(K9.hideTimeZone())
                .setIdentity(account.getIdentity(0))
                .setTo(Collections.singletonList(address))
//                .setIdentity(identity)
                .setMessageFormat(SimpleMessageFormat.TEXT)
                .setForcedUnencrypted(!encrypted)

                .setAttachments(Collections.emptyList());

        if (ispEp) {
            fields.add(new foundation.pEp.jniadapter.Pair<>(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY, identity.fpr));
            builder.setSubject("Please ignore, this message is part of import key protocol");
            result.setShortmsg("Please ignore, this message is part of import key protocol");
            result.setLongmsg("");
        } else {
            builder.setText(context.getString(R.string.pgp_key_import_instructions));
            result.setLongmsg(context.getString(R.string.pgp_key_import_instructions));
            builder.setSubject("p≡p Key Import");
            result.setShortmsg("p≡p Key Import");
        }
        result.setOptFields(fields);

        MimeMessage mimeMessage = builder.parseMessage(result);
        mimeMessage.setFlag(Flag.X_PEP_DISABLED, true);
        return pEp.encryptMessage(mimeMessage, null).get(PlanckProvider.ENCRYPTED_MESSAGE_POSITION);
    }

    public static List<String> getKeyListWithoutDuplicates(String[] keyListHeaders) {
        if (keyListHeaders.length == 0) return Collections.emptyList();
        else {
            Set<String> keys = new HashSet<>(
                    Arrays.asList(keyListHeaders[0].split(PlanckProvider.PLANCK_KEY_LIST_SEPARATOR))
            );
            return new ArrayList<>(keys);
        }
    }

    public static <MSG extends Message> boolean hasKeyImportHeader(MSG srcMsg, MSG decryptedMsg) {
        return srcMsg.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT).length > 0
                || decryptedMsg.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT).length > 0
                || srcMsg.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY).length > 0
                || decryptedMsg.getHeader(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY).length > 0;

    }

    public static <MSG extends Message> String extractKeyFromHeader(MSG srcMsg, MimeMessage decryptedMsg, Rating rating, String header) {
        if (rating.value < Rating.pEpRatingTrusted.value) {
            if (srcMsg.getHeader(header).length > 0) {
                return srcMsg.getHeader(header)[0];
            } else if (decryptedMsg.getHeaderNames().contains(header)) {
                return decryptedMsg.getHeader(header)[0];
            }
        } else if (decryptedMsg.getHeaderNames().contains(header)) {
            return decryptedMsg.getHeader(header)[0];
        }
        return "";
    }

    public static String sanitizeFpr(String fpr) {
        if (fpr != null) return fpr.toUpperCase().replaceAll("\\P{XDigit}", "");
        return "";
    }

    public static <MSG extends Message> boolean isAutoConsumeMessage(MSG message) {
        final Set<String> headerNames = message.getHeaderNames();
        return headerNames.contains(MimeHeader.HEADER_PEP_AUTOCONSUME.toUpperCase(Locale.ROOT))
                || headerNames.contains(MimeHeader.HEADER_PEP_AUTOCONSUME_LEGACY.toUpperCase(Locale.ROOT));
    }

    public static boolean isPepStatusClickable(ArrayList<Identity> recipients ,Rating rating) {
        return recipients.size() > 0 && rating.value >= Rating.pEpRatingReliable.value;
    }

    public static boolean isRatingUnsecure(@NotNull Rating rating) {
        switch (rating) {
            case pEpRatingUnderAttack:
            case pEpRatingB0rken:
            case pEpRatingMistrust:
            case pEpRatingUndefined:
            case pEpRatingCannotDecrypt:
            case pEpRatingHaveNoKey:
            case pEpRatingUnencrypted:
                return true;

            default:
                return false;
        }
    }

    public static boolean isHandshakeRating(@NotNull Rating rating) {
        return rating.value == Rating.pEpRatingReliable.value;
    }

    public static boolean isRatingTrusted(@NotNull Rating rating) {
        switch (rating) {
            case pEpRatingTrusted:
            case pEpRatingTrustedAndAnonymized:
            case pEpRatingFullyAnonymous:
                return true;

            default:
                return false;
        }
    }

    public static boolean isRatingReliable(@NotNull Rating rating) {
        return rating.value == Rating.pEpRatingReliable.value
                || isRatingTrusted(rating);
    }

    public static boolean isRatingDangerous(@NotNull Rating rating) {
        switch (rating) {
            case pEpRatingMistrust:
            case pEpRatingB0rken:
            case pEpRatingUnderAttack:
                return true;

            default:
                return false;
        }
    }
}

