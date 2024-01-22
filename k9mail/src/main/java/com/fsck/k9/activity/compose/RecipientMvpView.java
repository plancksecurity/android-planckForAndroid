package com.fsck.k9.activity.compose;


import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextWatcher;
import android.view.MenuItem;
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
import com.fsck.k9.mail.Message;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.ui.ActionRecipientSelectView;
import com.fsck.k9.planck.ui.tools.FeedbackTools;

import foundation.pEp.jniadapter.Rating;
import security.planck.ui.message_compose.ComposeAccountRecipient;
import security.planck.ui.resetpartnerkey.ResetPartnerKeyDialogKt;


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
    private final ViewAnimator recipientExpanderContainer;
    private final Account mAccount;

    // pEp stuff
    private Rating planckRating = Rating.pEpRatingUndefined;

    PlanckUIArtefactCache pEpUiCache;
    private RecipientPresenter presenter;
    private MessageReference messageReference;
    private MenuItem resetPartnerKeysItem;


    public RecipientMvpView(MessageCompose activity) {
        this.activity = activity;
        this.mAccount = activity.getAccount();
        fromView =  activity.findViewById(R.id.identity);
        toView = (ActionRecipientSelectView) activity.findViewById(R.id.to);
        ccView = (ActionRecipientSelectView) activity.findViewById(R.id.cc);
        bccView = (ActionRecipientSelectView) activity.findViewById(R.id.bcc);
        toView.setErrorTextView(activity.findViewById(R.id.to_error));
        ccView.setErrorTextView(activity.findViewById(R.id.cc_error));
        bccView.setErrorTextView(activity.findViewById(R.id.bcc_error));
        ccWrapper = activity.findViewById(R.id.cc_wrapper);
        ccDivider = activity.findViewById(R.id.cc_divider);
        bccWrapper = activity.findViewById(R.id.bcc_wrapper);
        bccDivider = activity.findViewById(R.id.bcc_divider);
        recipientExpanderContainer = (ViewAnimator) activity.findViewById(R.id.recipient_expander_container);

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
        toView.setPresenter(presenter, Message.RecipientType.TO);
        ccView.setPresenter(presenter, Message.RecipientType.CC);
        bccView.setPresenter(presenter, Message.RecipientType.BCC);
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
        }
    }

    public void showCryptoDialog(CryptoMode currentCryptoMode) {
        CryptoSettingsDialog dialog = CryptoSettingsDialog.newInstance(currentCryptoMode);
        dialog.show(activity.getFragmentManager(), "crypto_settings");
    }

    public void launchUserInteractionPendingIntent(PendingIntent pendingIntent, int requestCode) {
        activity.launchUserInteractionPendingIntent(pendingIntent, requestCode);
    }

    public void setLoaderManager(LoaderManager loaderManager) {
        toView.setLoaderManager(loaderManager);
        ccView.setLoaderManager(loaderManager);
        bccView.setLoaderManager(loaderManager);
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

    void onPlanckPrivacyStatus() {
        activity.launchVerifyPartnerIdentity(getFrom(), messageReference);
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

    public void doUiOperationRestoringFocus(Runnable uiOperation) {
        View currentFocus = activity.getCurrentFocus();
        uiOperation.run();
        if (currentFocus != null) {
            bccView.post(currentFocus::requestFocus);
        }
    }

    public void showError(Throwable throwable) {
        activity.setAndShowError(throwable);
    }

    public void setResetPartnerKeysItem(MenuItem resetPartnerKeysItem) {
        this.resetPartnerKeysItem = resetPartnerKeysItem;
    }

    public void resetPartnerKeys(String partner) {
        ResetPartnerKeyDialogKt.showResetPartnerKeyDialog(activity, partner);
    }

    public void showResetPartnerKeyOption() {
        resetPartnerKeysItem.setVisible(true);
    }

    public void hideResetPartnerKeyOption() {
        resetPartnerKeysItem.setVisible(false);
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
