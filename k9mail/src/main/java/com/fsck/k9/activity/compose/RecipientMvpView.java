package com.fsck.k9.activity.compose;


import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Context;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.PEpStatus;
import com.fsck.k9.view.RecipientSelectView;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.TokenListener;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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

    private static final int VIEW_INDEX_BCC_EXPANDER_VISIBLE = 0;
    private static final int VIEW_INDEX_BCC_EXPANDER_HIDDEN = 1;

    private final MessageCompose activity;
    private final View ccWrapper;
    private final View ccDivider;
    private final View bccWrapper;
    private final View bccDivider;
    private final RecipientSelectView toView;
    private final RecipientSelectView ccView;
    private final RecipientSelectView bccView;
    private final TextView fromView;
    private final ViewAnimator cryptoStatusView;
    private final ViewAnimator recipientExpanderContainer;
    private final ViewAnimator cryptoSpecialModeIndicator;
    // pEp stuff
    private MenuItem pEpIndicator;
    private Color pEpColor = Color.pEpRatingUndefined;
    PePUIArtefactCache pEpUiCache;

    private RecipientPresenter presenter;


    public RecipientMvpView(MessageCompose activity) {
        this.activity = activity;

        fromView = (TextView) activity.findViewById(R.id.identity);
        toView = (RecipientSelectView) activity.findViewById(R.id.to);
        ccView = (RecipientSelectView) activity.findViewById(R.id.cc);
        bccView = (RecipientSelectView) activity.findViewById(R.id.bcc);
        ccWrapper = activity.findViewById(R.id.cc_wrapper);
        ccDivider = activity.findViewById(R.id.cc_divider);
        bccWrapper = activity.findViewById(R.id.bcc_wrapper);
        bccDivider = activity.findViewById(R.id.bcc_divider);
        recipientExpanderContainer = (ViewAnimator) activity.findViewById(R.id.recipient_expander_container);
        cryptoStatusView = (ViewAnimator) activity.findViewById(R.id.crypto_status);
        cryptoStatusView.setOnClickListener(this);
        cryptoSpecialModeIndicator = (ViewAnimator) activity.findViewById(R.id.crypto_special_mode);
        cryptoSpecialModeIndicator.setOnClickListener(this);

        toView.setOnFocusChangeListener(this);
        ccView.setOnFocusChangeListener(this);
        bccView.setOnFocusChangeListener(this);

        View recipientExpander = activity.findViewById(R.id.recipient_expander);
        recipientExpander.setOnClickListener(this);

        View toLabel = activity.findViewById(R.id.to_label);
        View ccLabel = activity.findViewById(R.id.cc_label);
        View bccLabel = activity.findViewById(R.id.bcc_label);
        toLabel.setOnClickListener(this);
        ccLabel.setOnClickListener(this);
        bccLabel.setOnClickListener(this);

        pEpUiCache = PePUIArtefactCache.getInstance(activity.getApplicationContext());
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
                presenter.onToTokenAdded(recipient);
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onToTokenRemoved(recipient);
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onToTokenChanged(recipient);
            }
        });

        ccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onCcTokenAdded(recipient);
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onCcTokenRemoved(recipient);
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onCcTokenChanged(recipient);
            }
        });

        bccView.setTokenListener(new TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient recipient) {
                presenter.onBccTokenAdded(recipient);
            }

            @Override
            public void onTokenRemoved(Recipient recipient) {
                presenter.onBccTokenRemoved(recipient);
            }

            @Override
            public void onTokenChanged(Recipient recipient) {
                presenter.onBccTokenChanged(recipient);
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
            cryptoSpecialModeIndicator.setVisibility(View.GONE);
            return;
        }

        cryptoSpecialModeIndicator.setVisibility(View.VISIBLE);
        cryptoSpecialModeIndicator.setDisplayedChild(cryptoSpecialModeDisplayType.childToDisplay);
        activity.invalidateOptionsMenu();
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
        Toast.makeText(activity, R.string.error_sign_only_no_encryption, Toast.LENGTH_LONG).show();
    }

    public void showErrorContactNoAddress() {
        Toast.makeText(activity, R.string.error_contact_address_not_found, Toast.LENGTH_LONG).show();
    }

    public void showErrorOpenPgpConnection() {
        Toast.makeText(activity, R.string.error_crypto_provider_connect, Toast.LENGTH_LONG).show();
    }

    public void showErrorOpenPgpUserInteractionRequired() {
        Toast.makeText(activity, R.string.error_crypto_provider_ui_required, Toast.LENGTH_LONG).show();
    }

    public void showErrorMissingSignKey() {
        Toast.makeText(activity, R.string.compose_error_no_signing_key, Toast.LENGTH_LONG).show();
    }

    public void showErrorPrivateButMissingKeys() {
        Toast.makeText(activity, R.string.compose_error_private_missing_keys, Toast.LENGTH_LONG).show();
    }

    public void showErrorSignOnlyInline() {
        Toast.makeText(activity, R.string.error_crypto_sign_only_inline, Toast.LENGTH_LONG).show();
    }

    public void showErrorInlineSignOnly() {
        Toast.makeText(activity, R.string.error_crypto_inline_sign_only, Toast.LENGTH_LONG).show();
    }

    public void showErrorInlineAttach() {
        Toast.makeText(activity, R.string.error_crypto_inline_attach, Toast.LENGTH_LONG).show();
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
            case R.id.to_label: {
                presenter.onClickToLabel();
                break;
            }
            case R.id.cc_label: {
                presenter.onClickCcLabel();
                break;
            }
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
        dialog.show(activity.getFragmentManager(), "openpgp_inline");
    }

    public void showOpenPgpSignOnlyDialog(boolean firstTime) {
        PgpSignOnlyDialog dialog = PgpSignOnlyDialog.newInstance(firstTime, R.id.crypto_special_mode);
        dialog.show(activity.getFragmentManager(), "openpgp_signonly");
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

    public void setpEpColor(Color pEpColor) {
        this.pEpColor = pEpColor;
    }
    public Color getpEpColor() {
        return pEpColor;
    }
    public void handlepEpState(boolean... withToast) {
        boolean reallyWithToast = true;
        if(withToast.length>0) reallyWithToast = withToast[0];
        updatePePState();
        PEpUtils.colorActionBar(pEpUiCache, activity.getActionBar(), pEpColor);

        if(pEpIndicator!=null) {
            pEpIndicator.setIcon(pEpUiCache.getIcon(pEpColor));
            String msg = pEpUiCache.getTitle(pEpColor);
            if(reallyWithToast && !"".equals(msg)) {

//                Snackbar snack = Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG);
//                View view = snack.getView();
//                FrameLayout.LayoutParams params =(FrameLayout.LayoutParams)view.getLayoutParams();
//                params.gravity = Gravity.TOP;
//                view.setLayoutParams(params);
//                snack.show();
//                Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
            }
        }
    }

     void updatePePState() {
        presenter.updatepEpState();
    }

    public void setpEpIndicator(MenuItem pEpIndicator) {
        this.pEpIndicator = pEpIndicator;
    }

    public void onPepIndicator() {
        ArrayList<Identity> recipients = new ArrayList<>();
        // update color, just to be sure...
        recipients.addAll(PEpUtils.createIdentities(getToAddresses(), activity.getApplicationContext()));
        recipients.addAll(PEpUtils.createIdentities(getCcAddresses(), activity.getApplicationContext()));
        recipients.addAll(PEpUtils.createIdentities(getBccAddresses(), activity.getApplicationContext()));

//        mIgnoreOnPause = true;  // do *not* save state
        pEpUiCache.setRecipients(recipients);
        PEpStatus.actionShowStatus(activity, pEpColor, getFrom());
    }

    public Address getFromAddress() {
        return new Address(getFrom());
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
        return fromView.getText().toString();
    }

    public enum CryptoSpecialModeDisplayType {
        NONE(VIEW_INDEX_HIDDEN),
        PGP_INLINE(VIEW_INDEX_CRYPTO_SPECIAL_PGP_INLINE),
        SIGN_ONLY(VIEW_INDEX_CRYPTO_SPECIAL_SIGN_ONLY);


        final int childToDisplay;

        CryptoSpecialModeDisplayType(int childToDisplay) {
            this.childToDisplay = childToDisplay;
        }
    }
}
