package com.fsck.k9.activity.compose;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.AttachErrorState;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.ComposeCryptoStatusBuilder;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.SendErrorState;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MailTo;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.PgpMessageBuilder;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.Poller;
import com.fsck.k9.view.RecipientSelectView.Recipient;

import org.openintents.openpgp.OpenPgpApiManager;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderError;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import foundation.pEp.jniadapter.Rating;
import timber.log.Timber;


public class RecipientPresenter {
    private static final String STATE_KEY_CC_SHOWN = "state:ccShown";
    private static final String STATE_KEY_BCC_SHOWN = "state:bccShown";
    private static final String STATE_KEY_LAST_FOCUSED_TYPE = "state:lastFocusedType";
    private static final String STATE_KEY_CURRENT_CRYPTO_MODE = "state:currentCryptoMode";
    private static final String STATE_KEY_CRYPTO_ENABLE_PGP_INLINE = "state:cryptoEnablePgpInline";
    private static final String STATE_FORCE_UNENCRYPTED = "forceUnencrypted";
    private static final String STATE_ALWAYS_SECURE = "alwaysSecure";

    private static final int CONTACT_PICKER_TO = 1;
    private static final int CONTACT_PICKER_CC = 2;
    private static final int CONTACT_PICKER_BCC = 3;
    private static final int OPENPGP_USER_INTERACTION = 4;
    public static final int POLLING_INTERVAL = 1200;
    public static final String STATE_RATING = "rating";

    private static final int PGP_DIALOG_DISPLAY_THRESHOLD = 2;


    // transient state, which is either obtained during construction and initialization, or cached
    private final Context context;
    private final RecipientMvpView recipientMvpView;
    private final ComposePgpInlineDecider composePgpInlineDecider;
    private final RecipientsChangedListener listener;
    private Poller poller;
    private ReplyToParser replyToParser;
    private Account account;
    private Boolean hasContactPicker;
    @Nullable
    private ComposeCryptoStatus cachedCryptoStatus;
    private OpenPgpApiManager openPgpApiManager;

    private OpenPgpServiceConnection openPgpServiceConnection;
    private PEpProvider pEp;

    // persistent state, saved during onSaveInstanceState
    private RecipientType lastFocusedType = RecipientType.TO;
    // TODO initialize cryptoMode to other values under some circumstances, e.g. if we reply to an encrypted e-mail
    private CryptoMode currentCryptoMode = CryptoMode.OPPORTUNISTIC;
    private boolean cryptoEnablePgpInline = false;
    private boolean forceUnencrypted = false;
    private boolean isAlwaysSecure = false;
    private List<Address> toAdresses;
    private List<Address> ccAdresses;
    private List<Address> bccAdresses;
    private Rating privacyState = Rating.pEpRatingUnencrypted;
    private boolean dirty;
    private boolean isReplyToEncryptedMessage = false;


    public RecipientPresenter(Context context,  LoaderManager loaderManager,
           OpenPgpApiManager openPgpApiManager, RecipientMvpView recipientMvpView, Account account, ComposePgpInlineDecider composePgpInlineDecider,

            ReplyToParser replyToParser, RecipientsChangedListener recipientsChangedListener) {
        this.recipientMvpView = recipientMvpView;
        this.context = context;
        this.composePgpInlineDecider = composePgpInlineDecider;
        this.replyToParser = replyToParser;
        pEp = ((K9) context.getApplicationContext()).getpEpProvider();
        this.listener = recipientsChangedListener;
        this.openPgpApiManager = openPgpApiManager;

        recipientMvpView.setPresenter(this);
        recipientMvpView.setLoaderManager(loaderManager);
        onSwitchAccount(account);
        updateCryptoStatus();
        setupPEPStatusPolling();
    }

    private void setupPEPStatusPolling() {
        if (poller == null) {
            poller = new Poller(new Handler());
            poller.init(POLLING_INTERVAL, this::loadPEpStatus);
        } else {
            poller.stopPolling();
        }
        poller.startPolling();
    }

    public List<Address> getToAddresses() {
        return recipientMvpView.getToAddresses();
    }

    public List<Address> getCcAddresses() {
        return recipientMvpView.getCcAddresses();
    }

    public List<Address> getBccAddresses() {
        return recipientMvpView.getBccAddresses();
    }

    private List<Recipient> getAllRecipients() {
        ArrayList<Recipient> result = new ArrayList<>();

        result.addAll(recipientMvpView.getToRecipients());
        result.addAll(recipientMvpView.getCcRecipients());
        result.addAll(recipientMvpView.getBccRecipients());

        return result;
    }

    public boolean checkRecipientsOkForSending() {
        recipientMvpView.recipientToTryPerformCompletion();
        recipientMvpView.recipientCcTryPerformCompletion();
        recipientMvpView.recipientBccTryPerformCompletion();

        if (recipientMvpView.recipientToHasUncompletedText()) {
            recipientMvpView.showToUncompletedError();
            return true;
        }

        if (recipientMvpView.recipientCcHasUncompletedText()) {
            recipientMvpView.showCcUncompletedError();
            return true;
        }

        if (recipientMvpView.recipientBccHasUncompletedText()) {
            recipientMvpView.showBccUncompletedError();
            return true;
        }

        if (getToAddresses().isEmpty() && getCcAddresses().isEmpty() && getBccAddresses().isEmpty()) {
            recipientMvpView.showNoRecipientsError();
            return true;
        }

        return false;
    }

    public void initFromReplyToMessage(Message message, boolean isReplyAll) {
        ReplyToAddresses replyToAddresses = isReplyAll ?
                replyToParser.getRecipientsToReplyAllTo(message, account) :
                replyToParser.getRecipientsToReplyTo(message, account);

        addToAddresses(replyToAddresses.to);
        addCcAddresses(replyToAddresses.cc);

        boolean shouldSendAsPgpInline = composePgpInlineDecider.shouldReplyInline(message);
        if (shouldSendAsPgpInline) {
            cryptoEnablePgpInline = true;
        }
    }

    public void initFromMailto(MailTo mailTo) {
        addToAddresses(mailTo.getTo());
        addCcAddresses(mailTo.getCc());
        addBccAddresses(mailTo.getBcc());
    }

    public void initFromSendOrViewIntent(Intent intent) {
        String[] extraEmail = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
        String[] extraCc = intent.getStringArrayExtra(Intent.EXTRA_CC);
        String[] extraBcc = intent.getStringArrayExtra(Intent.EXTRA_BCC);

        if (extraEmail != null) {
            addToAddresses(addressFromStringArray(extraEmail));
        }

        if (extraCc != null) {
            addCcAddresses(addressFromStringArray(extraCc));
        }

        if (extraBcc != null) {
            addBccAddresses(addressFromStringArray(extraBcc));
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        recipientMvpView.setCcVisibility(savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN));
        recipientMvpView.setBccVisibility(savedInstanceState.getBoolean(STATE_KEY_BCC_SHOWN));
        lastFocusedType = RecipientType.valueOf(savedInstanceState.getString(STATE_KEY_LAST_FOCUSED_TYPE));
        currentCryptoMode = CryptoMode.valueOf(savedInstanceState.getString(STATE_KEY_CURRENT_CRYPTO_MODE));
        cryptoEnablePgpInline = savedInstanceState.getBoolean(STATE_KEY_CRYPTO_ENABLE_PGP_INLINE);
        forceUnencrypted = savedInstanceState.getBoolean(STATE_FORCE_UNENCRYPTED);
        isAlwaysSecure = savedInstanceState.getBoolean(STATE_ALWAYS_SECURE);
        privacyState = (Rating) savedInstanceState.getSerializable(STATE_RATING);
        updateRecipientExpanderVisibility();
        recipientMvpView.setpEpRating(privacyState);
        setupPEPStatusPolling();
    }

    public void onSaveInstanceState(Bundle outState) {
        poller.stopPolling();
        outState.putBoolean(STATE_KEY_CC_SHOWN, recipientMvpView.isCcVisible());
        outState.putBoolean(STATE_KEY_BCC_SHOWN, recipientMvpView.isBccVisible());
        outState.putString(STATE_KEY_LAST_FOCUSED_TYPE, lastFocusedType.toString());
        outState.putString(STATE_KEY_CURRENT_CRYPTO_MODE, currentCryptoMode.toString());
        outState.putBoolean(STATE_KEY_CRYPTO_ENABLE_PGP_INLINE, cryptoEnablePgpInline);
        outState.putBoolean(STATE_FORCE_UNENCRYPTED, forceUnencrypted);
        outState.putBoolean(STATE_ALWAYS_SECURE, isAlwaysSecure);
        outState.putSerializable(STATE_RATING, privacyState);
    }

    public void initFromDraftMessage(Message message) {
        initRecipientsFromDraftMessage(message);
        initPgpInlineFromDraftMessage(message);
    }

    private void initRecipientsFromDraftMessage(Message message) {
        addToAddresses(message.getRecipients(RecipientType.TO));

        Address[] ccRecipients = message.getRecipients(RecipientType.CC);
        addCcAddresses(ccRecipients);

        Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
        addBccAddresses(bccRecipients);
    }

    private void initPgpInlineFromDraftMessage(Message message) {
        cryptoEnablePgpInline = message.isSet(Flag.X_DRAFT_OPENPGP_INLINE);
    }

    private void addToAddresses(Address... toAddresses) {
        addRecipientsFromAddresses(RecipientType.TO, toAddresses);
    }

    private void addCcAddresses(Address... ccAddresses) {
        if (ccAddresses.length > 0) {
            addRecipientsFromAddresses(RecipientType.CC, ccAddresses);
            recipientMvpView.setCcVisibility(true);
            updateRecipientExpanderVisibility();
        }
    }

    public void addBccAddresses(Address... bccRecipients) {
        if (bccRecipients.length > 0) {
            forceUnencrypted = true;
            addRecipientsFromAddresses(RecipientType.BCC, bccRecipients);
            String bccAddress = account.getAlwaysBcc();

            // If the auto-bcc is the only entry in the BCC list, don't show the Bcc fields.
            boolean alreadyVisible = recipientMvpView.isBccVisible();
            boolean singleBccRecipientFromAccount =
                    bccRecipients.length == 1 && bccRecipients[0].toString().equals(bccAddress);
            recipientMvpView.setBccVisibility(alreadyVisible || !singleBccRecipientFromAccount);
            updateRecipientExpanderVisibility();
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
  /*
            boolean isCryptoConfigured = false;
            menu.findItem(R.id.openpgp_inline_enable).setVisible(isCryptoConfigured && !cryptoEnablePgpInline);
            menu.findItem(R.id.openpgp_inline_disable).setVisible(isCryptoConfigured && cryptoEnablePgpInline);
            boolean showSignOnly = !account.getOpenPgpHideSignOnly();
            boolean isSignOnly = currentCryptoStatus.isSignOnly();
            menu.findItem(R.id.openpgp_sign_only).setVisible(showSignOnly && !isSignOnly);
            menu.findItem(R.id.openpgp_sign_only_disable).setVisible(showSignOnly && isSignOnly);

            boolean pgpInlineModeEnabled = currentCryptoStatus.isPgpInlineModeEnabled();
            boolean showPgpInlineEnable = (isEncrypting || isSignOnly) && !pgpInlineModeEnabled;
            menu.findItem(R.id.openpgp_inline_enable).setVisible(showPgpInlineEnable);
            menu.findItem(R.id.openpgp_inline_disable).setVisible(pgpInlineModeEnabled);
        } else {
            menu.findItem(R.id.openpgp_inline_enable).setVisible(false);
            menu.findItem(R.id.openpgp_inline_disable).setVisible(false);
            menu.findItem(R.id.openpgp_encrypt_enable).setVisible(false);
            menu.findItem(R.id.openpgp_encrypt_disable).setVisible(false);
            menu.findItem(R.id.openpgp_sign_only).setVisible(false);
            menu.findItem(R.id.openpgp_sign_only_disable).setVisible(false);
        }*/

        boolean noContactPickerAvailable = !hasContactPicker();
        if (noContactPickerAvailable) {
            menu.findItem(R.id.add_from_contacts).setVisible(false);
        }


    }

    public void onSwitchAccount(Account account) {
        this.account = account;

        if (account.isAlwaysShowCcBcc()) {
            recipientMvpView.setCcVisibility(true);
            recipientMvpView.setBccVisibility(true);
            updateRecipientExpanderVisibility();
        }

        String openPgpProvider = account.getOpenPgpProvider();
        recipientMvpView.setCryptoProvider(openPgpProvider);
        openPgpApiManager.setOpenPgpProvider(openPgpProvider, openPgpCallback);
    }

    @SuppressWarnings("UnusedParameters")
    public void onSwitchIdentity(Identity identity) {

        // TODO decide what actually to do on identity switch?
        /*
        if (mIdentityChanged) {
            mBccWrapper.setVisibility(View.VISIBLE);
        }
        mBccView.setText("");
        mBccView.addAddress(new Address(mAccount.getAlwaysBcc(), ""));
        */

    }

    private static Address[] addressFromStringArray(String[] addresses) {
        return addressFromStringArray(Arrays.asList(addresses));
    }

    private static Address[] addressFromStringArray(List<String> addresses) {
        ArrayList<Address> result = new ArrayList<>(addresses.size());

        for (String addressStr : addresses) {
            Collections.addAll(result, Address.parseUnencoded(addressStr));
        }

        return result.toArray(new Address[result.size()]);
    }

    void onClickToLabel() {
        recipientMvpView.requestFocusOnToField();
    }

    void onClickCcLabel() {
        recipientMvpView.requestFocusOnCcField();
    }

    void onClickBccLabel() {
        recipientMvpView.requestFocusOnBccField();
    }

    void onClickRecipientExpander() {
        recipientMvpView.setCcVisibility(true);
        recipientMvpView.setBccVisibility(true);
        updateRecipientExpanderVisibility();
    }

    private void hideEmptyExtendedRecipientFields() {
        if (recipientMvpView.getCcAddresses().isEmpty()) {
            recipientMvpView.setCcVisibility(false);
            if (lastFocusedType == RecipientType.CC) {
                lastFocusedType = RecipientType.TO;
            }
        }
        if (recipientMvpView.getBccAddresses().isEmpty()) {
            recipientMvpView.setBccVisibility(false);
            if (lastFocusedType == RecipientType.BCC) {
                lastFocusedType = RecipientType.TO;
            }
        }
        updateRecipientExpanderVisibility();
    }

    private void updateRecipientExpanderVisibility() {
        boolean notBothAreVisible = !(recipientMvpView.isCcVisible() && recipientMvpView.isBccVisible());
        recipientMvpView.setRecipientExpanderVisibility(notBothAreVisible);
    }

    public void updateCryptoStatus() {
        cachedCryptoStatus = null;
        handlepEpState();

        OpenPgpProviderState openPgpProviderState = openPgpApiManager.getOpenPgpProviderState();

        Long accountCryptoKey = account.getOpenPgpKey();
        if (accountCryptoKey == Account.NO_OPENPGP_KEY) {
            accountCryptoKey = null;
        }
    }

    public ComposeCryptoStatus getCurrentCryptoStatus() {
        if (cachedCryptoStatus == null) {
            ComposeCryptoStatusBuilder builder = new ComposeCryptoStatusBuilder()
                    .setOpenPgpProviderState(OpenPgpProviderState.UNCONFIGURED)
                    .setCryptoMode(currentCryptoMode)
                    .setEnablePgpInline(cryptoEnablePgpInline)
                    .setRecipients(getAllRecipients());

            long accountCryptoKey = account.getOpenPgpKey();
            if (accountCryptoKey != Account.NO_OPENPGP_KEY) {
                // TODO split these into individual settings? maybe after key is bound to identity
                builder.setSigningKeyId(accountCryptoKey);
                builder.setSelfEncryptId(accountCryptoKey);
            }

            cachedCryptoStatus = builder.build();
        }

        return cachedCryptoStatus;
    }

    public boolean isForceTextMessageFormat() {
        if (cryptoEnablePgpInline) {
            ComposeCryptoStatus cryptoStatus = getCurrentCryptoStatus();
            return cryptoStatus.isEncryptionEnabled() || cryptoStatus.isSigningEnabled();
        } else {
            return false;
        }
    }

    void onToTokenAdded() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onToTokenRemoved() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onToTokenChanged() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onCcTokenAdded() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onCcTokenRemoved() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onCcTokenChanged() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onBccTokenAdded() {
        forceUnencrypted = true;
        updateCryptoStatus();
        dirty = true;
        listener.onRecipientsChanged();
    }

    void onBccTokenRemoved() {
        forceUnencrypted = false;
        updateCryptoStatus();
        dirty = true;
        listener.onRecipientsChanged();
    }

    void onBccTokenChanged() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    public void onCryptoModeChanged(CryptoMode cryptoMode) {
        currentCryptoMode = cryptoMode;
        updateCryptoStatus();
    }

    public void onCryptoPgpInlineChanged(boolean enablePgpInline) {
        cryptoEnablePgpInline = enablePgpInline;
        updateCryptoStatus();
    }

    private void addRecipientsFromAddresses(final RecipientType recipientType, final Address... addresses) {
        new RecipientLoader(context, account.getOpenPgpProvider(), addresses) {
            @Override
            public void deliverResult(List<Recipient> result) {
                Recipient[] recipientArray = result.toArray(new Recipient[result.size()]);
                recipientMvpView.addRecipients(recipientType, recipientArray);

                stopLoading();
                abandon();
            }
        }.startLoading();
    }

    private void addRecipientFromContactUri(final RecipientType recipientType, final Uri uri) {
        new RecipientLoader(context, account.getOpenPgpProvider(), uri, false) {
            @Override
            public void deliverResult(List<Recipient> result) {
                // TODO handle multiple available mail addresses for a contact?
                if (result.isEmpty()) {
                    recipientMvpView.showErrorContactNoAddress();
                    return;
                }

                Recipient recipient = result.get(0);
                recipientMvpView.addRecipients(recipientType, recipient);

                stopLoading();
                abandon();
            }
        }.startLoading();
    }

    void onToFocused() {
        lastFocusedType = RecipientType.TO;
    }

    void onCcFocused() {
        lastFocusedType = RecipientType.CC;
    }

    void onBccFocused() {
        lastFocusedType = RecipientType.BCC;
    }

    public void onMenuAddFromContacts() {
        int requestCode = recipientTypeToRequestCode(lastFocusedType);
        recipientMvpView.showContactPicker(requestCode);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CONTACT_PICKER_TO:
            case CONTACT_PICKER_CC:
            case CONTACT_PICKER_BCC:
                if (resultCode != Activity.RESULT_OK || data == null) {
                    return;
                }
                RecipientType recipientType = recipientTypeFromRequestCode(requestCode);
                addRecipientFromContactUri(recipientType, data.getData());
                break;
            case OPENPGP_USER_INTERACTION:
                openPgpApiManager.onUserInteractionResult();
                break;
        }
    }

    private static int recipientTypeToRequestCode(RecipientType type) {
        switch (type) {
            case TO: {
                return CONTACT_PICKER_TO;
            }
            case CC: {
                return CONTACT_PICKER_CC;
            }
            case BCC: {
                return CONTACT_PICKER_BCC;
            }
        }

        throw new AssertionError("Unhandled case: " + type);
    }

    private static RecipientType recipientTypeFromRequestCode(int type) {
        switch (type) {
            case CONTACT_PICKER_TO: {
                return RecipientType.TO;
            }
            case CONTACT_PICKER_CC: {
                return RecipientType.CC;
            }
            case CONTACT_PICKER_BCC: {
                return RecipientType.BCC;
            }
        }

        throw new AssertionError("Unhandled case: " + type);
    }

    public void onNonRecipientFieldFocused() {
        if (!account.isAlwaysShowCcBcc()) {
            hideEmptyExtendedRecipientFields();
        }
    }

    void onClickCryptoStatus() {
        switch (openPgpApiManager.getOpenPgpProviderState()) {
            case UNCONFIGURED:
                Timber.e("click on crypto status while unconfigured - this should not really happen?!");
                return;
            case OK:
                if (cachedCryptoStatus.isSignOnly()) {
                    recipientMvpView.showErrorIsSignOnly();
                } else {
                    recipientMvpView.showCryptoDialog(currentCryptoMode);
                }
                return;

            case UI_REQUIRED:
                // TODO show openpgp settings
                PendingIntent pendingIntent = openPgpApiManager.getUserInteractionPendingIntent();
                recipientMvpView.launchUserInteractionPendingIntent(pendingIntent, OPENPGP_USER_INTERACTION);
                break;

            case UNINITIALIZED:
            case ERROR:
                openPgpApiManager.refreshConnection();
        }
    }

    /**
     * Does the device actually have a Contacts application suitable for
     * picking a contact. As hard as it is to believe, some vendors ship
     * without it.
     *
     * @return True, if the device supports picking contacts. False, otherwise.
     */
    private boolean hasContactPicker() {
        if (hasContactPicker == null) {
            Contacts contacts = Contacts.getInstance(context);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(contacts.contactPickerIntent(), 0);
            hasContactPicker = !resolveInfoList.isEmpty();
        }

        return hasContactPicker;
    }

    public void showPgpSendError(SendErrorState sendErrorState) {
        switch (sendErrorState) {
            case PROVIDER_ERROR:
                recipientMvpView.showErrorOpenPgpConnection();
                break;
            case SIGN_KEY_NOT_CONFIGURED:
                recipientMvpView.showErrorMissingSignKey();
                break;
            case PRIVATE_BUT_MISSING_KEYS:
                recipientMvpView.showErrorPrivateButMissingKeys();
                break;
            default:
                throw new AssertionError("not all error states handled, this is a bug!");
        }
    }

    void showPgpAttachError(AttachErrorState attachErrorState) {
        switch (attachErrorState) {
            case IS_INLINE:
                recipientMvpView.showErrorInlineAttach();
                break;
            default:
                throw new AssertionError("not all error states handled, this is a bug!");
        }
    }

    public void builderSetProperties(MessageBuilder messageBuilder) {
        if (messageBuilder instanceof PgpMessageBuilder) {
            throw new IllegalArgumentException("PpgMessageBuilder must be called with ComposeCryptoStatus argument!");
        }

        messageBuilder.setTo(getToAddresses());
        messageBuilder.setCc(getCcAddresses());
        messageBuilder.setBcc(getBccAddresses());
    }

    public void builderSetProperties(PgpMessageBuilder pgpBuilder) {
        pgpBuilder.setOpenPgpApi(openPgpApiManager.getOpenPgpApi());
        pgpBuilder.setCryptoStatus(getCurrentCryptoStatus());
    }

    public void onMenuSetPgpInline(boolean enablePgpInline) {
        onCryptoPgpInlineChanged(enablePgpInline);
        if (enablePgpInline) {
            boolean shouldShowPgpInlineDialog = checkAndIncrementPgpInlineDialogCounter();
            if (shouldShowPgpInlineDialog) {
                recipientMvpView.showOpenPgpInlineDialog(true);
            }
        }
    }

    public void onMenuSetSignOnly(boolean enableSignOnly) {
        if (enableSignOnly) {
            onCryptoModeChanged(CryptoMode.SIGN_ONLY);
            boolean shouldShowPgpSignOnlyDialog = checkAndIncrementPgpSignOnlyDialogCounter();
            if (shouldShowPgpSignOnlyDialog) {
                recipientMvpView.showOpenPgpSignOnlyDialog(true);
            }
        } else {
            onCryptoModeChanged(CryptoMode.OPPORTUNISTIC);
        }
    }

    public void onCryptoPgpSignOnlyDisabled() {
        onCryptoPgpInlineChanged(false);
        onCryptoModeChanged(CryptoMode.OPPORTUNISTIC);
    }

    private boolean checkAndIncrementPgpInlineDialogCounter() {
        int pgpInlineDialogCounter = K9.getPgpInlineDialogCounter();
        if (pgpInlineDialogCounter < PGP_DIALOG_DISPLAY_THRESHOLD) {
            K9.setPgpInlineDialogCounter(pgpInlineDialogCounter + 1);
            return true;
        }
        return false;
    }

    private boolean checkAndIncrementPgpSignOnlyDialogCounter() {
        int pgpSignOnlyDialogCounter = K9.getPgpSignOnlyDialogCounter();
        if (pgpSignOnlyDialogCounter < PGP_DIALOG_DISPLAY_THRESHOLD) {
            K9.setPgpSignOnlyDialogCounter(pgpSignOnlyDialogCounter + 1);
            return true;
        }
        return false;
    }

    void onClickCryptoSpecialModeIndicator() {
        ComposeCryptoStatus currentCryptoStatus = getCurrentCryptoStatus();
        if (currentCryptoStatus.isSignOnly()) {
            recipientMvpView.showOpenPgpSignOnlyDialog(false);
        } else if (currentCryptoStatus.isPgpInlineModeEnabled()) {
            recipientMvpView.showOpenPgpInlineDialog(false);
        } else {
            throw new IllegalStateException("This icon should not be clickable while no special mode is active!");
        }
    }


    public void switchPrivacyProtection(PEpProvider.ProtectionScope scope, boolean... protection) {
        if (bccAdresses == null || bccAdresses.size() == 0) {
            switch (scope) {
                case MESSAGE:
                    if (protection.length > 0)
                        throw new RuntimeException("On message only switch allowed");
                    forceUnencrypted = !forceUnencrypted;
                    break;
                case ACCOUNT:
                    if (protection.length < 1)
                        throw new RuntimeException("On account only explicit boolean allowed");
                    forceUnencrypted = !protection[0];
                    break;
            }
        } else {
            forceUnencrypted = !forceUnencrypted;
        }
        dirty = true;
        handlepEpState();
    }

    public boolean isForceUnencrypted() {
        return forceUnencrypted;
    }

    public boolean isAlwaysSecure() {
        return isAlwaysSecure;
    }

    public void onResume() {
        toAdresses = getToAddresses();
        ccAdresses = getCcAddresses();
        bccAdresses = getBccAddresses();

        dirty = true;

        this.recipientMvpView.notifyAddressesChanged(toAdresses, ccAdresses, bccAdresses);
        setupPEPStatusPolling();
    }

    public void onPause() {
        poller.stopPolling();
    }

    public void onPEpPrivacyStatus() {
        recipientMvpView.onPEpPrivacyStatus();
    }


    public void updatepEpState() {
        /* no-op */
    }


    public void handlepEpState(boolean... withToast) {
        recipientMvpView.handlepEpState(withToast);
    }

    public void setpEpIndicator(MenuItem pEpIndicator) {
        recipientMvpView.setpEpIndicator(pEpIndicator);
    }

    public boolean isForwardedMessageWeakestThanOriginal(Rating originalMessageRating) {
        Rating currentRating = recipientMvpView.getpEpRating();
        return currentRating.value < Rating.pEpRatingReliable.value && currentRating.value < originalMessageRating.value;
    }

    @VisibleForTesting
    void setOpenPgpServiceConnection(OpenPgpServiceConnection openPgpServiceConnection, String cryptoProvider) {
        this.openPgpServiceConnection = openPgpServiceConnection;
        //this.cryptoProvider = cryptoProvider;
    }

    public void setAlwaysSecure(Boolean alwaysSecure) {
        isAlwaysSecure = alwaysSecure;
    }
    public boolean shouldSaveRemotely() {
        // TODO more appropriate logic?
        return cachedCryptoStatus == null || !cachedCryptoStatus.isEncryptionEnabled();
    }

    public boolean isPepStatusClickable() {
        return recipientMvpView.pEpUiCache.getRecipients().size() > 0 &&
                recipientMvpView.getpEpRating().value >= Rating.pEpRatingReliable.value;
    }

    public interface RecipientsChangedListener {
        void onRecipientsChanged();
    }

    private final OpenPgpApiManagerCallback openPgpCallback = new OpenPgpApiManagerCallback() {
        @Override
        public void onOpenPgpProviderStatusChanged() {
            updatepEpState();
            //asyncUpdateCryptoStatus();
            if (openPgpApiManager.getOpenPgpProviderState() == OpenPgpProviderState.UI_REQUIRED) {
                recipientMvpView.showErrorOpenPgpUserInteractionRequired();
            }
            }

        @Override
        public void onOpenPgpProviderError(OpenPgpProviderError error) {
            switch (error) {
                case ConnectionLost:
                    openPgpApiManager.refreshConnection();
                    break;
                case VersionIncompatible:
                    //recipientMvpView.showErrorOpenPgpIncompatible();
                    break;
                case ConnectionFailed:
                default:
                    recipientMvpView.showErrorOpenPgpConnection();
                    break;
            }
        }
    };

    public enum CryptoMode {
        DISABLE,
        SIGN_ONLY,
        OPPORTUNISTIC,
        PRIVATE,
    }

    private void loadPEpStatus() {
        Address fromAddress = recipientMvpView.getFromAddress();
        List<Address> newToAdresses = recipientMvpView.getToAddresses();
        List<Address> newCcAdresses = recipientMvpView.getCcAddresses();
        List<Address> newBccAdresses = recipientMvpView.getBccAddresses();
        toAdresses = initializeAdresses(toAdresses);
        ccAdresses = initializeAdresses(ccAdresses);
        bccAdresses = initializeAdresses(bccAdresses);
        if (privacyState.value != Rating.pEpRatingUndefined.value && newToAdresses.isEmpty() && newCcAdresses.isEmpty() && newBccAdresses.isEmpty()) {
            showDefaultStatus();
            recipientMvpView.unlockSendButton();
            return;
        }
        if (fromAddress != null
                && (dirty
                || addressesChanged(toAdresses, newToAdresses)
                || addressesChanged(ccAdresses, newCcAdresses)
                || addressesChanged(bccAdresses, newBccAdresses))) {
            dirty = false;
            toAdresses = newToAdresses;
            ccAdresses = newCcAdresses;
            bccAdresses = newBccAdresses;
            recipientMvpView.lockSendButton();
            pEp = ((K9) context.getApplicationContext()).getpEpProvider();
            pEp.getRating(fromAddress, toAdresses, ccAdresses, bccAdresses, new PEpProvider.ResultCallback<Rating>() {
                @Override
                public void onLoaded(Rating rating) {
                    if (newToAdresses.isEmpty() && newCcAdresses.isEmpty() && newBccAdresses.isEmpty()) {
                        showDefaultStatus();
                    } else {
                        privacyState = rating;
                        showRatingFeedback(rating);
                    }
                    recipientMvpView.unlockSendButton();
                }

                @Override
                public void onError(Throwable throwable) {
                    showDefaultStatus();
                    recipientMvpView.unlockSendButton();
                }
            });
        }
        recipientMvpView.unlockSendButton();
    }

    private void showDefaultStatus() {
        privacyState = Rating.pEpRatingUndefined;
        showRatingFeedback(privacyState);
    }

    private void showRatingFeedback(Rating rating) {
        recipientMvpView.setpEpRating(rating);
        handlepEpState();
    }

    private List<Address> initializeAdresses(List<Address> addresses) {
        if(addresses == null) {
            addresses = Collections.emptyList();
        }
        return addresses;
    }

    private boolean addressesChanged(List<Address> oldAdresses, List<Address> newAdresses) {
        return !oldAdresses.equals(newAdresses);
    }
}
