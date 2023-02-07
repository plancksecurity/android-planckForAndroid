package com.fsck.k9.pEp.ui.privacy.status;

import static com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.pEp.infrastructure.MessageView;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.ui.PepColoredActivity;

import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import org.jetbrains.annotations.NotNull;

import foundation.pEp.jniadapter.Rating;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PEpStatus extends PepColoredActivity implements PEpStatusView, ConfirmationDialogFragment.ConfirmationDialogFragmentListener {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String SENDER = "isComposedKey";
    private static final String MYSELF = "myself";
    private static final String RATING = "rating";
    private static final String MESSAGE_REFERENCE = "messageReference";
    private static final String MESSAGE_DIRECTION = "messageDirection";
    public static final int REQUEST_STATUS = 5; // Do not use a value below 5 because it would collide with other constants in RecipientPresenter.

    @Inject
    PEpStatusPresenter presenter;

    @Inject
    PEpStatusRendererBuilder rendererBuilder;

    @Inject
    @MessageView
    DisplayHtml displayHtml;

    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;

    @Bind(R.id.its_own_messageTV)
    TextView itsOwnMessageTV;

    RecyclerView.LayoutManager recipientsLayoutManager;
    private RVRendererAdapter<PEpIdentity> recipientsAdapter;

    String sender = "";
    private MessageReference messageReference;
    private String myself = "";

    private static final String FORCE_UNENCRYPTED = "forceUnencrypted";
    private static final String ALWAYS_SECURE = "alwaysSecure";

    public static PendingIntent pendingIntentShowStatus(
            Activity context, Rating currentRating, String sender, MessageReference messageReference,
            Boolean isMessageIncoming, String myself, boolean forceUnencrypted, boolean alwaysSecure) {
        Intent intent = createShowStatusIntent(context, currentRating, sender, messageReference, isMessageIncoming, myself, forceUnencrypted, alwaysSecure);
        return PendingIntent.getActivity(context, REQUEST_STATUS, intent, PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    @NotNull
    private static Intent createShowStatusIntent(
            Activity context, Rating currentRating, String sender, MessageReference messageReference,
            Boolean isMessageIncoming, String myself, boolean forceUnencrypted, boolean alwaysSecure) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
        String ratingName = forceUnencrypted ? Rating.pEpRatingUnencrypted.toString() : currentRating.toString();
        i.putExtra(CURRENT_RATING, ratingName);
        i.putExtra(SENDER, sender);
        i.putExtra(MYSELF, myself);
        if (messageReference != null) {
            i.putExtra(MESSAGE_REFERENCE, messageReference.toIdentityString());
        } else {
            i.putExtra(MESSAGE_REFERENCE, "");
        }
        i.putExtra(MESSAGE_DIRECTION, isMessageIncoming);
        i.putExtra(FORCE_UNENCRYPTED, forceUnencrypted);
        i.putExtra(ALWAYS_SECURE, alwaysSecure);
        return i;
    }

    public static void actionShowStatus(Activity context, Rating currentRating, String sender,
                                        MessageReference messageReference, Boolean isMessageIncoming, String myself) {
        Intent intent = createShowStatusIntent(context, currentRating, sender, messageReference, isMessageIncoming, myself, false, false);
        context.startActivityForResult(intent, REQUEST_STATUS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPepRating();
        setContentView(R.layout.pep_status);
        ButterKnife.bind(PEpStatus.this);
        initPep();
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SENDER)
                && intent.hasExtra(MESSAGE_REFERENCE)) {
            sender = intent.getStringExtra(SENDER);
            myself = intent.getStringExtra(MYSELF);
            messageReference = MessageReference.parse(intent.getExtras().getString(MESSAGE_REFERENCE));
            boolean isMessageIncoming = intent.getBooleanExtra(MESSAGE_DIRECTION, false);
            boolean forceUnencrypted = intent.getBooleanExtra(FORCE_UNENCRYPTED, false);
            boolean alwaysSecure = intent.getBooleanExtra(ALWAYS_SECURE, false);
            presenter.initialize(this, getUiCache(), getpEp(), displayHtml, isMessageIncoming,
                    new Address(sender), forceUnencrypted, alwaysSecure);
            presenter.loadMessage(messageReference);
        }

        restorePEpRating(savedInstanceState);
        presenter.restoreInstanceState(savedInstanceState);
        setUpActionBar();
        presenter.loadRecipients();
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
    }

    private void restorePEpRating(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pEpRating = (Rating) savedInstanceState.getSerializable(RATING);
            setpEpRating(pEpRating);
        }
    }

    private void setUpActionBar() {
        initializeToolbar(false, R.string.pep_title_activity_privacy_status);
        colorActionBar();
    }

    @Override
    public void initializeToolbar(Boolean showUpButton, @StringRes int stringResource) {
        setUpToolbar(showUpButton);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            TextView titleTV = findViewById(R.id.titleText);
            titleTV.setText(getString(stringResource).toUpperCase());
        }
    }

    @Override
    public void initializeToolbar(Boolean showUpButton, String title) {
        setUpToolbar(showUpButton);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            TextView titleTV = findViewById(R.id.titleText);
            titleTV.setText(title);
        }
    }

    @Override
    public void setupRecipients(List<PEpIdentity> pEpIdentities) {
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        rendererBuilder.setUp(
                getOnResetClickListener(),
                getOnHandshakeResultListener(),
                myself
        );
        recipientsAdapter = new RVRendererAdapter<PEpIdentity>(rendererBuilder, new ListAdapteeCollection(pEpIdentities));
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.setVisibility(View.VISIBLE);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    @NonNull
    private PEpStatusRendererBuilder.ResetClickListener getOnResetClickListener() {
        return showResetDialog();
    }

    private PEpIdentity pEpIdentity;

    private PEpStatusRendererBuilder.ResetClickListener showResetDialog() {
        return identity -> showDialogFragment(R.id.dialog_reset_partner_key_confirmation, identity);
    }

    private void showDialogFragment(int dialogId, PEpIdentity identity) {
        if (isDestroyed()) return;

        ConfirmationDialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_reset_partner_key_confirmation:
                pEpIdentity = identity;
                fragment = ConfirmationDialogFragment.newInstance(
                        dialogId,
                        getString(R.string.reset_partner_keys_title),
                        getString(R.string.reset_partner_keys_description),
                        getString(R.string.reset_partner_keys_confirmation_action),
                        getString(R.string.pep_dialog_confirm_forward_weaker_trust_level_cancel_button)
                );
                fragment.setCancelable(false);
                break;
            case R.id.dialog_reset_partner_key_success:
                fragment = ConfirmationDialogFragment.newInstance(
                        dialogId,
                        getString(R.string.reset_partner_keys_title),
                        getString(R.string.reset_partner_keys_successful_result),
                        getString(R.string.reset_partner_keys_close_action)
                );
                break;
            case R.id.dialog_reset_partner_key_error:
                fragment = ConfirmationDialogFragment.newInstance(
                        dialogId,
                        getString(R.string.reset_partner_keys_title),
                        getString(R.string.reset_partner_keys_failure),
                        getString(R.string.reset_partner_keys_try_again_action),
                        getString(R.string.reset_partner_keys_close_action)
                );
                break;
            default:
                return;
        }

        getSupportFragmentManager().beginTransaction()
                .add(fragment, getDialogTag(dialogId))
                .commitAllowingStateLoss();
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_reset_partner_key_confirmation:
                try {
                    presenter.resetpEpData(pEpIdentity);

                    showDialogFragment(R.id.dialog_reset_partner_key_success, null);
                } catch (Exception e) {
                    showDialogFragment(R.id.dialog_reset_partner_key_error, null);
                }
                break;
            case R.id.dialog_reset_partner_key_error:
                rendererBuilder.getResetClickListener().keyReset(pEpIdentity);
                break;
            default:
                break;
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {

    }

    @Override
    public void dialogCancelled(int dialogId) {

    }

    @NonNull
    private PEpStatusRendererBuilder.HandshakeResultListener getOnHandshakeResultListener() {
        return (identity, trust) -> presenter.onHandshakeResult(identity, trust);
    }

    @Override
    public void setupBackIntent(Rating rating, boolean forceUnencrypted, boolean alwaysSecure) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(CURRENT_RATING, rating);
        returnIntent.putExtra(FORCE_UNENCRYPTED, forceUnencrypted);
        returnIntent.putExtra(ALWAYS_SECURE, alwaysSecure);
        setResult(Activity.RESULT_OK, returnIntent);
    }

    @Override
    public void updateIdentities(List<PEpIdentity> updatedIdentities) {
        recipientsAdapter.setCollection(new ListAdapteeCollection<>(updatedIdentities));
        recipientsAdapter.notifyDataSetChanged();
    }

    @Override
    public void setRating(Rating pEpRating) {
        setpEpRating(pEpRating);
        colorActionBar();
    }

    @Override
    public void showDataLoadError() {
        FeedbackTools.showLongFeedback(getRootView(), getResources().getString(R.string.status_loading_error));
    }

    @Override
    public void showResetpEpDataFeedback() {
        FeedbackTools.showLongFeedback(getRootView(), getString(R.string.key_reset_identity_feedback));
    }

    @Override
    public void showUndoTrust(String username) {
        showUndo(getString(R.string.trust_identity_feedback, username));
    }

    @Override
    public void showUndoMistrust(String username) {
        showUndo(getString(R.string.mistrust_identity_feedback, username));
    }

    @Override
    public void showMistrustFeedback(String username) {
        FeedbackTools.showLongFeedback(getRootView(), getString(R.string.mistrust_identity_feedback, username));
    }

    public void showUndo(String feedback) {
        FeedbackTools.showLongFeedback(getRootView(),
                feedback,
                getString(R.string.message_list_item_undo),
                v -> presenter.undoTrust()
        );
    }

    public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        public SimpleDividerItemDecoration(Context context) {
            mDivider = ContextCompat.getDrawable(context, R.drawable.line_divider);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isIncoming = getIntent().getBooleanExtra(MESSAGE_DIRECTION, false);
        if (isIncoming) return false;

        getMenuInflater().inflate(R.menu.menu_pep_status, menu);
        menu.findItem(R.id.force_unencrypted).setTitle(presenter.isForceUnencrypted()
                ? R.string.pep_force_protected
                : R.string.pep_force_unprotected
        );

        menu.findItem(R.id.is_always_secure).setTitle(presenter.isAlwaysSecure()
                ? R.string.is_not_always_secure
                : R.string.is_always_secure
        );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.force_unencrypted:
                presenter.setForceUnencrypted(!presenter.isForceUnencrypted());
                item.setTitle(presenter.isForceUnencrypted()
                        ? R.string.pep_force_protected
                        : R.string.pep_force_unprotected
                );
                return true;
            case R.id.is_always_secure:
                presenter.setAlwaysSecure(!presenter.isAlwaysSecure());
                item.setTitle(presenter.isAlwaysSecure()
                        ? R.string.is_not_always_secure
                        : R.string.is_always_secure
                );
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.pep_explanation)
                .setMessage(getUiCache().getExplanation(getpEpRating()))
                .setPositiveButton(R.string.okay_action,
                        (dialog, which) -> dialog.dismiss())
                .create()
                .show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        colorActionBar();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(RATING, pEpRating);
        presenter.saveInstanceState(outState);
    }

    @Override
    public void showItsOnlyOwnMsg() {
        recipientsView.setVisibility(View.GONE);
        itsOwnMessageTV.setVisibility(View.VISIBLE);
    }

    @Override
    public void updateToolbarColor(Rating rating) {
        colorActionBar(rating);
    }

}
