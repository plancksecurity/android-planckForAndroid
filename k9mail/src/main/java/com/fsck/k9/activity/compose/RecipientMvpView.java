package com.fsck.k9.activity.compose;


import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ViewAnimator;

import androidx.loader.app.LoaderManager;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.ui.ActionRecipientSelectView;
import com.fsck.k9.planck.ui.privacy.status.PlanckStatus;
import com.fsck.k9.planck.ui.tools.FeedbackTools;
import com.fsck.k9.activity.compose.RecipientSelectView.TokenListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;
import security.planck.ui.message_compose.ComposeAccountRecipient;


public class RecipientMvpView implements OnFocusChangeListener, OnClickListener {
    private static final int VIEW_INDEX_HIDDEN = -1;
    private static final int VIEW_INDEX_CRYPTO_STATUS_DISABLED = 0;
    private static final int VIEW_INDEX_CRYPTO_STATUS_ERROR = 1;
    private static final int VIEW_INDEX_CRYPTO_STATUS_NO_RECIPIENTS = 2;
    private static final int VIEW_INDEX_CRYPTO_STATUS_ERROR_NO_KEY = 3;
    private static final int VIEW_INDEX_CRYPTO_STATUS_DISABLED_NO_KEY = 4;
    private static final int VIEW_INDEX_CRYPTO_STATUS_UNTRUSTED = 5;
    private static final int VIEW_INDEX_CRYPTO_STATUS_TRUSTED = 6;
    private static final int VIEW_INDEX_CRYPTO_STATUS_SIGN_ONLY = 0;

    private static final int VIEW_INDEX_CRYPTO_SPECIAL_PGP_INLINE = 0;
    private static final int VIEW_INDEX_CRYPTO_SPECIAL_SIGN_ONLY = 1;
    private static final int VIEW_INDEX_CRYPTO_SPECIAL_SIGN_ONLY_PGP_INLINE = 2;

    private static final int VIEW_INDEX_BCC_EXPANDER_VISIBLE = 0;
    private static final int VIEW_INDEX_BCC_EXPANDER_HIDDEN = 1;
    public static final String COPIED_RECIPIENT = "copied_recipient";

    private final MessageCompose activity;
    private final View ccWrapper;
    private final View ccDivider;
    private final View bccWrapper;
    private final View bccDivider;
    private final ActionRecipientSelectView toView;
    private final ActionRecipientSelectView ccView;
    private final ActionRecipientSelectView bccView;
    private final ComposeAccountRecipient fromView;
    private final ViewAnimator cryptoStatusView;
    private final ViewAnimator recipientExpanderContainer;
    private final Account mAccount;

    // pEp stuff
    private Rating planckRating = Rating.pEpRatingUndefined;

    PlanckUIArtefactCache pEpUiCache;
    private RecipientPresenter presenter;
    private MessageReference messageReference;


    public RecipientMvpView(MessageCompose activity) {
        this.activity = activity;
        this.mAccount = activity.getAccount();
        fromView =  activity.findViewById(R.id.identity);
        toView = (ActionRecipientSelectView) activity.findViewById(R.id.to);
        ccView = (ActionRecipientSelectView) activity.findViewById(R.id.cc);
        bccView = (ActionRecipientSelectView) activity.findViewById(R.id.bcc);
        ccWrapper = activity.findViewById(R.id.cc_wrapper);
        ccDivider = activity.findViewById(R.id.cc_divider);
        bccWrapper = activity.findViewById(R.id.bcc_wrapper);
        bccDivider = activity.findViewById(R.id.bcc_divider);
        recipientExpanderContainer = (ViewAnimator) activity.findViewById(R.id.recipient_expander_container);
        cryptoStatusView = (ViewAnimator) activity.findViewById(R.id.crypto_status);
        cryptoStatusView.setOnClickListener(this);

        toView.setOnFocusChangeListener(this);
        ccView.setOnFocusChangeListener(this);
        bccView.setOnFocusChangeListener(this);

        setOnCutCopyPasteListenerToView(toView);
        setOnCutCopyPasteListenerToView(ccView);
        setOnCutCopyPasteListenerToView(bccView);

        View recipientExpander = activity.findViewById(R.id.recipient_expander);
        recipientExpander.setOnClickListener(this);

        activity.findViewById(R.id.to_label).setOnClickListener(this);
        activity.findViewById(R.id.cc_label).setOnClickListener(this);
        activity.findViewById(R.id.bcc_label).setOnClickListener(this);
        activity.findViewById(R.id.to_wrapper).setOnClickListener(this);
        ccWrapper.setOnClickListener(this);
        bccWrapper.setOnClickListener(this);

        pEpUiCache = PlanckUIArtefactCache.getInstance(activity.getApplicationContext());
    }

    private void setOnCutCopyPasteListenerToView(ActionRecipientSelectView view) {
        view.setOnCutCopyPasteListener(new ActionRecipientSelectView.OnCutCopyPasteListener() {
            @Override
            public void onCut() {
                cutFromView(RecipientMvpView.this.toView);
            }

            @Override
            public void onCopy() {
                copyFromView(PlanckUtils.addressesToString(RecipientMvpView.this.toView.getAddresses()));
            }
        });
    }

    private void copyFromView(String text) {
        copyAdressesToClipboard(text);
    }

    private void cutFromView(ActionRecipientSelectView view) {
        copyFromView(PlanckUtils.addressesToString(view.getAddresses()));
        view.emptyAddresses();
    }

    private void copyAdressesToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) toView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(COPIED_RECIPIENT, text);
        clipboard.setPrimaryClip(clip);
    }

    public void setPresenter(final RecipientPresenter presenter) {
        this.presenter = presenter;

        if (presenter == null) {
            toView.setTokenListener(null);
            ccView.setTokenListener(null);
            bccView.setTokenListener(null);
            return;
        }

        toView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onToTokenAdded();
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onToTokenRemoved();
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onToTokenChanged();
            }

            @Override
            public void handleUnsecureTokenWarning() {
                presenter.handleUnsecureDeliveryWarning();
            }

            @Override
            public void onError(Throwable throwable) {
                showError(throwable);
            }
        });

        ccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onCcTokenAdded();
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onCcTokenRemoved();
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onCcTokenChanged();
            }

            @Override
            public void handleUnsecureTokenWarning() {
                presenter.handleUnsecureDeliveryWarning();
            }

            @Override
            public void onError(Throwable throwable) {
                showError(throwable);
            }
        });

        bccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onBccTokenAdded();
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onBccTokenRemoved();
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onBccTokenChanged();
            }

            @Override
            public void handleUnsecureTokenWarning() {
                presenter.handleUnsecureDeliveryWarning();
            }

            @Override
            public void onError(Throwable throwable) {
                showError(throwable);
            }
        });
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        toView.addTextChangedListener(textWatcher);
        ccView.addTextChangedListener(textWatcher);
        bccView.addTextChangedListener(textWatcher);
    }

    public void setCryptoProvider(String openPgpProvider) {
        toView.setCryptoProvider(openPgpProvider);
        ccView.setCryptoProvider(openPgpProvider);
        bccView.setCryptoProvider(openPgpProvider);
    }

    public void requestFocusOnToField() {
        toView.requestFocus();
    }

    public void requestFocusOnCcField() {
        ccView.requestFocus();
    }

    public void requestFocusOnBccField() {
        bccView.requestFocus();
    }

    public void setFontSizes(FontSizes fontSizes, int fontSize) {
        fontSizes.setViewTextSize(toView, fontSize);
        fontSizes.setViewTextSize(ccView, fontSize);
        fontSizes.setViewTextSize(bccView, fontSize);
    }

    public void addRecipients(RecipientType recipientType, Recipient... recipients) {
        switch (recipientType) {
            case TO: {
                toView.addRecipients(recipients);
                break;
            }
            case CC: {
                ccView.addRecipients(recipients);
                break;
            }
            case BCC: {
                bccView.addRecipients(recipients);
                break;
            }
        }
    }

    public void setCcVisibility(boolean visible) {
        ccWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
        ccDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setBccVisibility(boolean visible) {
        bccWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
        bccDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setRecipientExpanderVisibility(boolean visible) {
        int childToDisplay = visible ? VIEW_INDEX_BCC_EXPANDER_VISIBLE : VIEW_INDEX_BCC_EXPANDER_HIDDEN;
        if (recipientExpanderContainer.getDisplayedChild() != childToDisplay) {
            recipientExpanderContainer.setDisplayedChild(childToDisplay);
        }
    }

    public boolean isCcVisible() {
        return ccWrapper.getVisibility() == View.VISIBLE;
    }

    public boolean isBccVisible() {
        return bccWrapper.getVisibility() == View.VISIBLE;
    }

    public void showNoRecipientsError() {
        toView.setError(toView.getContext().getString(R.string.message_compose_error_no_recipients));
    }

    public List<Address> getToAddresses() {
        return Arrays.asList(toView.getAddresses());
    }

    public List<Address> getCcAddresses() {
        return Arrays.asList(ccView.getAddresses());
    }

    public List<Address> getBccAddresses() {
        return Arrays.asList(bccView.getAddresses());
    }

    public List<Recipient> getToRecipients() {
        return toView.getObjects();
    }

    public List<Recipient> getCcRecipients() {
        return ccView.getObjects();
    }

    public List<Recipient> getBccRecipients() {
        return bccView.getObjects();
    }

    public int getToUnsecureRecipientCount() {
        return toView.getUnsecureRecipientCount();
    }

    public int getCcUnsecureRecipientCount() {
        return ccView.getUnsecureRecipientCount();
    }

    public int getBccUnsecureRecipientCount() {
        return bccView.getUnsecureRecipientCount();
    }

    public void clearUnsecureRecipients() {
        toView.clearUnsecureRecipients();
        ccView.clearUnsecureRecipients();
        bccView.clearUnsecureRecipients();
    }

    public boolean recipientToHasUncompletedText() {
        return toView.hasUncompletedText();
    }

    public boolean recipientCcHasUncompletedText() {
        return ccView.hasUncompletedText();
    }

    public boolean recipientBccHasUncompletedText() {
        return bccView.hasUncompletedText();
    }

    public boolean recipientToTryPerformCompletion() {
        return toView.tryPerformCompletion();
    }

    public boolean recipientCcTryPerformCompletion() {
        return ccView.tryPerformCompletion();
    }

    public boolean recipientBccTryPerformCompletion() {
        return bccView.tryPerformCompletion();
    }

    public void showToUncompletedError() {
        toView.setError(toView.getContext().getString(R.string.compose_error_incomplete_recipient));
    }

    public void showCcUncompletedError() {
        ccView.setError(ccView.getContext().getString(R.string.compose_error_incomplete_recipient));
    }

    public void showBccUncompletedError() {
        bccView.setError(bccView.getContext().getString(R.string.compose_error_incomplete_recipient));
    }

    public void showCryptoSpecialMode(CryptoSpecialModeDisplayType cryptoSpecialModeDisplayType) {
        boolean shouldBeHidden = cryptoSpecialModeDisplayType.childToDisplay == VIEW_INDEX_HIDDEN;
        if (shouldBeHidden) {
            return;
        }

        activity.invalidateOptionsMenu();
    }

    public boolean isPepStatusClickable() {
        return PlanckUtils.isPepStatusClickable(pEpUiCache.getRecipients(), planckRating);
    }

    public void showCryptoStatus(CryptoStatusDisplayType cryptoStatusDisplayType) {
        boolean shouldBeHidden = cryptoStatusDisplayType.childToDisplay == VIEW_INDEX_HIDDEN;
        if (shouldBeHidden) {
            cryptoStatusView.setVisibility(View.GONE);
            return;
        }

        cryptoStatusView.setVisibility(View.VISIBLE);
        cryptoStatusView.setDisplayedChild(cryptoStatusDisplayType.childToDisplay);
    }

    public void showContactPicker(int requestCode) {
        activity.showContactPicker(requestCode);
    }

    public void showErrorIsSignOnly() {
        FeedbackTools.showShortFeedback(activity.getRootView() , activity.getResources().getString(R.string.error_sign_only_no_encryption));
    }

    public void showErrorContactNoAddress() {
        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.error_contact_address_not_found));
    }

    public void showErrorOpenPgpConnection() {
        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.error_crypto_provider_connect));
    }

    public void showErrorOpenPgpUserInteractionRequired() {
        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.error_crypto_provider_ui_required));
    }

    public void showErrorMissingSignKey() {
        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.compose_error_no_signing_key));
    }

    public void showErrorPrivateButMissingKeys() {
        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.compose_error_private_missing_keys));
    }

    public void showErrorAttachInline() {
        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.error_crypto_inline_attach));
    }

    public void showErrorInlineSignOnly() {
//        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.error_crypto_inline_sign_only));
    }

    public void showErrorInlineAttach() {
        FeedbackTools.showLongFeedback(activity.getRootView(), activity.getString(R.string.error_crypto_inline_attach));
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        switch (view.getId()) {
            case R.id.to: {
                presenter.onToFocused();
                break;
            }
            case R.id.cc: {
                presenter.onCcFocused();
                break;
            }
            case R.id.bcc: {
                presenter.onBccFocused();
                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.to_wrapper:
            case R.id.to_label: {
                presenter.onClickToLabel();
                break;
            }
            case R.id.cc_wrapper:
            case R.id.cc_label: {
                presenter.onClickCcLabel();
                break;
            }
            case R.id.bcc_wrapper:
            case R.id.bcc_label: {
                presenter.onClickBccLabel();
                break;
            }
            case R.id.recipient_expander: {
                presenter.onClickRecipientExpander();
                break;
            }
            case R.id.crypto_status: {
                presenter.onClickCryptoStatus();
                break;
            }
            case R.id.crypto_special_mode: {
                presenter.onClickCryptoSpecialModeIndicator();
            }
        }
    }

    public void showCryptoDialog(CryptoMode currentCryptoMode) {
        CryptoSettingsDialog dialog = CryptoSettingsDialog.newInstance(currentCryptoMode);
        dialog.show(activity.getFragmentManager(), "crypto_settings");
    }

    public void showOpenPgpInlineDialog(boolean firstTime) {
        PgpInlineDialog dialog = PgpInlineDialog.newInstance(firstTime, R.id.crypto_special_mode);
        dialog.show(activity.getSupportFragmentManager(), "openpgp_inline");
    }

    public void showOpenPgpSignOnlyDialog(boolean firstTime) {
        PgpSignOnlyDialog dialog = PgpSignOnlyDialog.newInstance(firstTime, R.id.crypto_special_mode);
        dialog.show(activity.getSupportFragmentManager(), "openpgp_signonly");
    }

    public void launchUserInteractionPendingIntent(PendingIntent pendingIntent, int requestCode) {
        activity.launchUserInteractionPendingIntent(pendingIntent, requestCode);
    }

    public void setLoaderManager(LoaderManager loaderManager) {
        toView.setLoaderManager(loaderManager);
        ccView.setLoaderManager(loaderManager);
        bccView.setLoaderManager(loaderManager);
    }

    public void addpEpOnFocusChangeListener(OnFocusChangeListener pEpChangeTracker) {
        // those trigger indicator changes
        toView.setOnFocusChangeListener(pEpChangeTracker);
        ccView.setOnFocusChangeListener(pEpChangeTracker);
        bccView.setOnFocusChangeListener(pEpChangeTracker);
    }

    public void setPlanckRating(Rating planckRating) {
        this.planckRating = planckRating;
    }

    public Rating getPlanckRating() {
        return planckRating;
    }

    void handlepEpState(boolean forceHide) {
        if (mAccount.isPlanckPrivacyProtected()) {
            activity.setToolbarRating(planckRating, forceHide);
        } else {
            activity.setToolbarRating(Rating.pEpRatingUnencrypted, forceHide);
        }
    }

    public void showUnsecureDeliveryWarning(int unsecureRecipientsCount) {
        activity.showUnsecureDeliveryWarning(unsecureRecipientsCount);
    }

    public void hideUnsecureDeliveryWarning() {
        activity.hideUnsecureDeliveryWarning();
    }

    public void showSingleRecipientHandshakeBanner() {
        activity.showSingleRecipientHandshakeBanner();
    }

    public void hideSingleRecipientHandshakeBanner() {
        activity.hideSingleRecipientHandshakeBanner();
    }

    public void refreshRecipients() {
        ArrayList<Identity> recipients = new ArrayList<>();
        recipients.addAll(PlanckUtils.createIdentities(getToAddresses(), activity.getApplicationContext()));
        recipients.addAll(PlanckUtils.createIdentities(getCcAddresses(), activity.getApplicationContext()));
        recipients.addAll(PlanckUtils.createIdentities(getBccAddresses(), activity.getApplicationContext()));
        pEpUiCache.setRecipients(mAccount, recipients);
    }

        void onPEpPrivacyStatus() {
            PendingIntent pendingIntent = PlanckStatus.pendingIntentShowStatus(activity, getFrom(), messageReference, false, getFrom(), presenter.isForceUnencrypted(), presenter.isAlwaysSecure());
            launchUserInteractionPendingIntent(pendingIntent, PlanckStatus.REQUEST_STATUS);

        //FIXME P4A-934: "Caused by: android.os.TransactionTooLargeException: data parcel size 1064328 bytes", not always reproducible.
    }

    public void setMessageReference(MessageReference reference) {
        this.messageReference = reference;
    }

    public Address getFromAddress() {
        return new Address(getFrom());
    }

    public void messageRatingIsBeingLoaded() {
        activity.messageRatingIsBeingLoaded();
    }

    public void messageRatingLoaded() {
        activity.messageRatingLoaded();
    }

    public void notifyRecipientsChanged(
            List<Recipient> toRecipients,
            List<Recipient> ccRecipients,
            List<Recipient> bccRecipients
    ) {
        View currentFocus = activity.getCurrentFocus();
        notifyRecipientsChanged(toView, toRecipients);
        notifyRecipientsChanged(ccView, ccRecipients);
        notifyRecipientsChanged(bccView, bccRecipients);
        if (currentFocus != null) {
            bccView.post(currentFocus::requestFocus);
        }
    }

    private void notifyRecipientsChanged(
            ActionRecipientSelectView view,
            List<Recipient> recipients
    ) {
        for (Recipient recipient : recipients) {
            view.removeObject(recipient);
            view.addRecipients(recipient);
        }
        view.restoreFirstRecipientTruncation();
    }

    public void updateRecipientsFromEcho(String echoSender) {
        toView.updateRecipientsFromEcho(echoSender);
        ccView.updateRecipientsFromEcho(echoSender);
    }

    public void showError(Throwable throwable) {
        activity.setAndShowError(throwable);
    }

    public enum CryptoStatusDisplayType {
        UNCONFIGURED(VIEW_INDEX_HIDDEN),
        UNINITIALIZED(VIEW_INDEX_HIDDEN),
        DISABLED(VIEW_INDEX_CRYPTO_STATUS_DISABLED),
        SIGN_ONLY(VIEW_INDEX_CRYPTO_STATUS_SIGN_ONLY),
        OPPORTUNISTIC_EMPTY(VIEW_INDEX_CRYPTO_STATUS_NO_RECIPIENTS),
        OPPORTUNISTIC_NOKEY(VIEW_INDEX_CRYPTO_STATUS_DISABLED_NO_KEY),
        OPPORTUNISTIC_UNTRUSTED(VIEW_INDEX_CRYPTO_STATUS_UNTRUSTED),
        OPPORTUNISTIC_TRUSTED(VIEW_INDEX_CRYPTO_STATUS_TRUSTED),
        PRIVATE_EMPTY(VIEW_INDEX_CRYPTO_STATUS_NO_RECIPIENTS),
        PRIVATE_NOKEY(VIEW_INDEX_CRYPTO_STATUS_ERROR_NO_KEY),
        PRIVATE_UNTRUSTED(VIEW_INDEX_CRYPTO_STATUS_UNTRUSTED),
        PRIVATE_TRUSTED(VIEW_INDEX_CRYPTO_STATUS_TRUSTED),
        ERROR(VIEW_INDEX_CRYPTO_STATUS_ERROR);


        final int childToDisplay;

        CryptoStatusDisplayType(int childToDisplay) {
            this.childToDisplay = childToDisplay;
        }
    }

    public String getFrom() {
        return fromView.getText();
    }

    public enum CryptoSpecialModeDisplayType {
        NONE(VIEW_INDEX_HIDDEN),
        PGP_INLINE(VIEW_INDEX_CRYPTO_SPECIAL_PGP_INLINE),
        SIGN_ONLY(VIEW_INDEX_CRYPTO_SPECIAL_SIGN_ONLY),
        SIGN_ONLY_PGP_INLINE(VIEW_INDEX_CRYPTO_SPECIAL_SIGN_ONLY_PGP_INLINE);


        final int childToDisplay;

        CryptoSpecialModeDisplayType(int childToDisplay) {
            this.childToDisplay = childToDisplay;
        }
    }
}
