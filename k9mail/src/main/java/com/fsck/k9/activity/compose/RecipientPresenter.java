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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
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
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.infrastructure.Poller;

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
import security.planck.echo.EchoMessageReceivedListener;
import timber.log.Timber;


public class RecipientPresenter implements EchoMessageReceivedListener {
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
    private static final int ZERO_RECIPIENTS = 0;
    private static final int ONE_ADDRESS = 1;


    // transient state, which is either obtained during construction and initialization, or cached
    private final Context context;
    private final RecipientMvpView recipientMvpView;
    private RecipientSelectPresenter toPresenter;
    private RecipientSelectPresenter ccPresenter;
    private RecipientSelectPresenter bccPresenter;
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
    private PlanckProvider planck;

    // persistent state, saved during onSaveInstanceState
    private RecipientType lastFocusedType = RecipientType.TO;
    // TODO initialize cryptoMode to other values under some circumstances, e.g. if we reply to an encrypted e-mail
    private CryptoMode currentCryptoMode = CryptoMode.OPPORTUNISTIC;
    private boolean cryptoEnablePgpInline = false;
    private boolean forceUnencrypted = false;
    private boolean isAlwaysSecure = false;
    private Rating privacyState = Rating.pEpRatingUnencrypted;
    private boolean isReplyToEncryptedMessage = false;
    private long lastRequestTime;
    PlanckUIArtefactCache planckUiCache;


    public RecipientPresenter(Context context,  LoaderManager loaderManager,
            OpenPgpApiManager openPgpApiManager, RecipientMvpView recipientMvpView, Account account,
            ComposePgpInlineDecider composePgpInlineDecider,
            PlanckProvider planckProvider,
            ReplyToParser replyToParser, RecipientsChangedListener recipientsChangedListener) {
        this.recipientMvpView = recipientMvpView;
        this.context = context;
        this.composePgpInlineDecider = composePgpInlineDecider;
        this.replyToParser = replyToParser;
        this.planck = planckProvider;
        this.listener = recipientsChangedListener;
        this.openPgpApiManager = openPgpApiManager;

        recipientMvpView.setPresenter(this);
        planckUiCache = PlanckUIArtefactCache.getInstance(context.getApplicationContext());
        recipientMvpView.setLoaderManager(loaderManager);
        onSwitchAccount(account);
        updateCryptoStatus();
    }

    public void setPresenter(RecipientSelectPresenter presenter, RecipientType type) {
        switch (type) {
            case TO:
                this.toPresenter = presenter;
                break;
            case CC:
                this.ccPresenter = presenter;
                break;
            case BCC:
                this.bccPresenter = presenter;
                break;
        }
    }

    private void setupPlanckStatusPolling() {
        if (poller == null) {
            poller = new Poller(new Handler());
            poller.init(POLLING_INTERVAL, this::loadPEpStatus);
        } else {
            poller.stopPolling();
        }
        poller.startPolling();
    }

    public List<Address> getToAddresses() {
        return toPresenter.getAddresses();
    }

    public List<Address> getCcAddresses() {
        return ccPresenter.getAddresses();
    }

    public List<Address> getBccAddresses() {
        return bccPresenter.getAddresses();
    }

    public void clearUnsecureRecipients() {
        toPresenter.clearUnsecureAddresses();
        ccPresenter.clearUnsecureAddresses();
        bccPresenter.clearUnsecureAddresses();
    }

    public void startHandshakeWithSingleRecipient(MessageReference relatedMessageReference) {
        refreshRecipients();
        if (canHandshakeSingleAddress(
                getToAddresses(),
                getCcAddresses(),
                getBccAddresses()
        )) {
            recipientMvpView.setMessageReference(relatedMessageReference);
            recipientMvpView.onPlanckPrivacyStatus();
        }
    }

    public void refreshRecipients() {
        ArrayList<foundation.pEp.jniadapter.Identity> recipients = new ArrayList<>();
        recipients.addAll(PlanckUtils.createIdentities(getToAddresses(), context.getApplicationContext()));
        recipients.addAll(PlanckUtils.createIdentities(getCcAddresses(), context.getApplicationContext()));
        recipients.addAll(PlanckUtils.createIdentities(getBccAddresses(), context.getApplicationContext()));
        planckUiCache.setRecipients(account, recipients);
    }

    private List<Recipient> getAllRecipients() {
        ArrayList<Recipient> result = new ArrayList<>();

        result.addAll(toPresenter.getRecipients());
        result.addAll(ccPresenter.getRecipients());
        result.addAll(bccPresenter.getRecipients());

        return result;
    }

    public boolean checkRecipientsOkForSending() {
        toPresenter.tryPerformCompletion();
        ccPresenter.tryPerformCompletion();
        bccPresenter.tryPerformCompletion();

        if (toPresenter.reportedUncompletedRecipients()
        || ccPresenter.reportedUncompletedRecipients()
        || bccPresenter.reportedUncompletedRecipients()) return true;

        if (addressesAreEmpty(getToAddresses(), getCcAddresses(), getBccAddresses())) {
            toPresenter.showNoRecipientsError();
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
        recipientMvpView.setPlanckRating(privacyState);
    }

    public void onSaveInstanceState(Bundle outState) {
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
        setAlwaysSecure(message.isSet(Flag.X_PEP_NEVER_UNSECURE));
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
        if (getCcAddresses().isEmpty()) {
            recipientMvpView.setCcVisibility(false);
            if (lastFocusedType == RecipientType.CC) {
                lastFocusedType = RecipientType.TO;
            }
        }
        if (getBccAddresses().isEmpty()) {
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
        loadPEpStatus();

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

    void onRecipientsChanged() {
        updateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void showError(Throwable throwable) {
        recipientMvpView.showError(throwable);
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
                addRecipients(recipientType, recipientArray);

                stopLoading();
                abandon();
            }
        }.startLoading();
    }

    public void addRecipients(RecipientType recipientType, Recipient... recipients) {
        switch (recipientType) {
            case TO: {
                toPresenter.addRecipients(recipients);
                break;
            }
            case CC: {
                ccPresenter.addRecipients(recipients);
                break;
            }
            case BCC: {
                bccPresenter.addRecipients(recipients);
                break;
            }
        }
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
                addRecipients(recipientType, recipient);

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

    public void handleVerifyPartnerIdentityResult() {
        loadPEpStatus();
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


    public void switchPrivacyProtection(PlanckProvider.ProtectionScope scope, boolean... protection) {
        List<Address> bccAdresses = getBccAddresses();
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
        updateCryptoStatus();
    }

    public boolean isForceUnencrypted() {
        return forceUnencrypted;
    }

    public boolean isAlwaysSecure() {
        return isAlwaysSecure;
    }

    public void onResume() {
        notifyRecipientsChanged();
        updateCryptoStatus();
    }

    public void handlepEpState() {
        recipientMvpView.handlepEpState(getAllRecipients().isEmpty());
    }

    public boolean isForwardedMessageWeakestThanOriginal(Rating originalMessageRating) {
        Rating currentRating = recipientMvpView.getPlanckRating();
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

    @Override
    public void echoMessageReceived(@NonNull String from, @NonNull String to) {
        updateCryptoStatus();
        if (account.isPlanckPrivacyProtected() && K9.isPlanckForwardWarningEnabled()) {
            if (to.equalsIgnoreCase(recipientMvpView.getFromAddress().getAddress())) {
                toPresenter.updateRecipientsFromEcho(from);
                ccPresenter.updateRecipientsFromEcho(from);
            }
        }
    }

    public interface RecipientsChangedListener {
        void onRecipientsChanged();
    }

    private final OpenPgpApiManagerCallback openPgpCallback = new OpenPgpApiManagerCallback() {
        @Override
        public void onOpenPgpProviderStatusChanged() {
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
        List<Address> newToAdresses = getToAddresses();
        List<Address> newCcAdresses = getCcAddresses();
        List<Address> newBccAdresses = getBccAddresses();
        if (addressesAreEmpty(newToAdresses, newCcAdresses, newBccAdresses)) {
            showDefaultStatus();
            recipientMvpView.hideUnsecureDeliveryWarning();
            recipientMvpView.hideSingleRecipientHandshakeBanner();
            recipientMvpView.messageRatingLoaded();
            return;
        }
        recipientMvpView.messageRatingIsBeingLoaded();
        long requestTime = System.currentTimeMillis();
        lastRequestTime = requestTime;
        planck.getRating(fromAddress, newToAdresses, newCcAdresses, newBccAdresses, new PlanckProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                if (isRequestOutdated(requestTime)) {
                    return;
                }
                if (addressesAreEmpty(newToAdresses, newCcAdresses, newBccAdresses)) {
                    showDefaultStatus();
                    recipientMvpView.hideUnsecureDeliveryWarning();
                    recipientMvpView.hideSingleRecipientHandshakeBanner();
                } else {
                    privacyState = rating;
                    handleSingleAddressHandshakeFeedback(newToAdresses, newCcAdresses, newBccAdresses);
                    showRatingFeedback(rating);
                }
                recipientMvpView.messageRatingLoaded();
            }

            @Override
            public void onError(Throwable throwable) {
                recipientMvpView.showError(throwable);
                if (isRequestOutdated(requestTime)) {
                    return;
                }
                showDefaultStatus();
                recipientMvpView.messageRatingLoaded();
            }
        });
    }

    private boolean isRequestOutdated(long requestTime) {
        return lastRequestTime > requestTime;
    }

    private boolean addressesAreEmpty(List<Address> newToAdresses, List<Address> newCcAdresses, List<Address> newBccAdresses) {
        return newToAdresses.isEmpty() && newCcAdresses.isEmpty() && newBccAdresses.isEmpty();
    }

    public void handleUnsecureDeliveryWarning() {
        int unsecureRecipientsCount = K9.isPlanckForwardWarningEnabled()
                && account.isPlanckPrivacyProtected()
                ? getUnsecureRecipientsCount()
                : ZERO_RECIPIENTS;
        handleUnsecureDeliveryWarning(unsecureRecipientsCount);
    }

    private void handleUnsecureDeliveryWarning(int unsecureRecipientsCount) {
        if (unsecureRecipientsCount > ZERO_RECIPIENTS) {
            recipientMvpView.showUnsecureDeliveryWarning(unsecureRecipientsCount);
        } else {
            recipientMvpView.hideUnsecureDeliveryWarning();
        }
    }

    private void handleSingleAddressHandshakeFeedback(
            List<Address> newToAdresses,
            List<Address> newCcAdresses,
            List<Address> newBccAdresses
    ) {
        if (canHandshakeSingleAddress(newToAdresses, newCcAdresses, newBccAdresses)) {
            recipientMvpView.showSingleRecipientHandshakeBanner();
        } else {
            recipientMvpView.hideSingleRecipientHandshakeBanner();
        }
    }

    private boolean canHandshakeSingleAddress(
            List<Address> newToAdresses,
            List<Address> newCcAdresses,
            List<Address> newBccAdresses
    ) {
        if (!newBccAdresses.isEmpty()) return false;
        List<Address> candidates = new ArrayList<>();
        candidates.addAll(newToAdresses);
        candidates.addAll(newCcAdresses);
        return candidates.size() == ONE_ADDRESS
                && PlanckUtils.isRatingReliable(privacyState)
                && account!= null && account.isPlanckPrivacyProtected()
                && !candidates.get(0).getAddress().equalsIgnoreCase(account.getEmail()); // recipient not my own account
    }

    private int getUnsecureRecipientsCount() {
        return toPresenter.getUnsecureAddressChannelCount() +
                ccPresenter.getUnsecureAddressChannelCount() +
                bccPresenter.getUnsecureAddressChannelCount();
    }

    private void showDefaultStatus() {
        privacyState = Rating.pEpRatingUndefined;
        showRatingFeedback(privacyState);
    }

    private void showRatingFeedback(Rating rating) {
        recipientMvpView.setPlanckRating(rating);
        handlepEpState();
    }

    public void notifyRecipientsChanged() {
        recipientMvpView.doUiOperationRestoringFocus(() -> {
            toPresenter.notifyRecipientsChanged();
            ccPresenter.notifyRecipientsChanged();
            bccPresenter.notifyRecipientsChanged();
        });
    }
}
