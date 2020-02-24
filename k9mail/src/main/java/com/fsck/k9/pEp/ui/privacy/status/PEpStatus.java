package com.fsck.k9.pEp.ui.privacy.status;

import android.app.Activity;
import android.app.AlertDialog;
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

import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.ui.PepColoredActivity;

import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import foundation.pEp.jniadapter.Rating;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PEpStatus extends PepColoredActivity implements PEpStatusView {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String SENDER = "isComposedKey";
    private static final String MYSELF = "myself";
    private static final String RATING = "rating";
    private static final String MESSAGE_REFERENCE = "messageReference";
    private static final String MESSAGE_DIRECTION = "messageDirection";
    public static final int REQUEST_STATUS = 2;

    @Inject PEpStatusPresenter presenter;

    //@Bind(R.id.pEpTitle)
    //TextView pEpTitle;
    //@Bind(R.id.title_status_badge)
    //ImageView statusBadge;
    //@Bind(R.id.pEpSuggestion)
    //TextView pEpSuggestion;
    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    //PEpIdentitiesAdapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;
    //PepStatusRendererAdapter statusAdapter;
    private RVRendererAdapter<PEpIdentity> recipientsAdapter;
    //PEpIdentitiesAdapter recipientsAdapter;

    String sender = "";
    private MessageReference messageReference;
    private String myself = "";

    public static void actionShowStatus(Activity context, Rating currentRating, String sender, MessageReference messageReference, Boolean isMessageIncoming, String myself) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
        i.putExtra(CURRENT_RATING, currentRating.toString());
        i.putExtra(SENDER, sender);
        i.putExtra(MYSELF, myself);
        if (messageReference != null) {
            i.putExtra(MESSAGE_REFERENCE, messageReference.toIdentityString());
        } else {
            i.putExtra(MESSAGE_REFERENCE, "");
        }
        i.putExtra(MESSAGE_DIRECTION, isMessageIncoming);
        context.startActivityForResult(i, REQUEST_STATUS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPepRating();
        setContentView(R.layout.pep_status);
        ButterKnife.bind(PEpStatus.this);
        initPep();
        if (getIntent() != null && getIntent().hasExtra(SENDER)
                && getIntent().hasExtra(MESSAGE_REFERENCE)) {
            sender = getIntent().getStringExtra(SENDER);
            myself = getIntent().getStringExtra(MYSELF);
            messageReference = MessageReference.parse(getIntent().getExtras().getString(MESSAGE_REFERENCE));
            boolean isMessageIncoming = getIntent().getBooleanExtra(MESSAGE_DIRECTION, false);
            presenter.initilize(this, getUiCache(), getpEp(), isMessageIncoming, new Address(sender));
            presenter.loadMessage(messageReference);
        }
        restorePEpRating(savedInstanceState);
        setUpActionBar();
        presenter.loadRecipients();
        presenter.loadRating(getpEpRating());
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
            titleTV.setText(stringResource);
            //getSupportActionBar().setTitle(getResources().getString(stringResource));
        }
    }

    @Override
    public void initializeToolbar(Boolean showUpButton, String title) {
        setUpToolbar(showUpButton);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            TextView titleTV = findViewById(R.id.titleText);
            titleTV.setText(title);
            //getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setupRecipients(List<PEpIdentity> pEpIdentities) {
        Preferences preferences = Preferences.getPreferences(PEpStatus.this);
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        //presenter.initializeAddressesOnDevice(preferences.getAccounts());
        //PEpStatusRenderer renderer111 = new PEpStatusRenderer(presenter.getAddressesOnDevice());
        RendererBuilder<PEpIdentity> rendererBuilder =
                new PEpStatusRendererBuilder(
                        preferences.getAccounts(),
                        getOnResetClickListener(),
                        getOnTrustRessetClickListener(),
                        getOnHandshakeResultListener(),
                        myself
                );
        //Timber.e("the identities: " + pEpIdentities);
        ListAdapteeCollection<PEpIdentity> adapteeCollection = new ListAdapteeCollection<>(pEpIdentities);
        recipientsAdapter = new RVRendererAdapter<>(rendererBuilder, adapteeCollection);



        /*recipientsAdapter = new PEpIdentitiesAdapter(preferences.getAccounts(),
                getOnResetGreenClickListener(),
                getOnResetRedClickListener(),
                getOnHandshakeClickListener(),
                getContextActions());
        recipientsAdapter.setIdentities(pEpIdentities);*/
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.setVisibility(View.VISIBLE);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    @NonNull
    private PEpStatusRendererBuilder.ResetClickListener getOnResetClickListener() {
        return identity -> presenter.resetpEpData(identity);
    }

    @NonNull
    private PEpStatusRendererBuilder.TrustResetClickListener getOnTrustRessetClickListener() {
        return identity -> presenter.resetRecipientTrust(identity);
    }

    @NonNull
    private View.OnClickListener getOnHandshakeClickListener() {
        return v -> {
            int partnerPosition = ((Integer) v.getTag());
            PEpTrustwords.actionRequestHandshake(PEpStatus.this, myself, partnerPosition);
        };
    }

    @NonNull
    private PEpStatusRendererBuilder.HandshakeResultListener getOnHandshakeResultListener() {
        return (identity, trust) -> presenter.onHandshakeResult(identity, trust);
    }

    /*@NonNull
    private View.OnClickListener getOnResetRedClickListener() {
        return view -> new AlertDialog.Builder(view.getContext())
                .setMessage(R.string.handshake_reset_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            int position = ((Integer) view.getTag());
            presenter.resetRecipientTrust(position);
        }).setNegativeButton(R.string.cancel_action, null).show();
    }*/

    /*@NonNull
    private View.OnClickListener getOnResetGreenClickListener() {
        return view -> {
            int position = ((Integer) view.getTag());
            presenter.resetRecipientTrust(position);
        };
    }*/

    @Override
    public void setupBackIntent(Rating rating) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(CURRENT_RATING, rating);
        setResult(Activity.RESULT_OK, returnIntent);
    }

    @Override
    public void showPEpTexts(String title, String suggestion) {
        //pEpTitle.setText(title);
        //pEpSuggestion.setText(suggestion);
    }

    @Override
    public void updateIdentities(List<PEpIdentity> updatedIdentities) {
        recipientsAdapter.setCollection(new ListAdapteeCollection<>(updatedIdentities));
        //recipientsAdapter.setIdentities(updatedIdentities);
        recipientsAdapter.notifyDataSetChanged();
    }

    @Override
    public void setRating(Rating pEpRating) {
        setpEpRating(pEpRating);
        colorActionBar();
        presenter.loadRating(pEpRating);
    }

    @Override
    public void showDataLoadError() {
        FeedbackTools.showLongFeedback(getRootView() ,getResources().getString(R.string.status_loading_error));
    }

    @Override
    public void showBadge(Rating rating) {
        //statusBadge.setVisibility(View.VISIBLE);
        //statusBadge.setImageDrawable(PEpUtils.getDrawableForRating(PEpStatus.this, rating));
    }

    @Override
    public void hideBadge() {
        //statusBadge.setVisibility(View.GONE);
    }

    @Override
    public void showResetpEpDataFeedback() {
        FeedbackTools.showLongFeedback(getRootView(),getString(R.string.key_reset_identity_feedback));
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PEpTrustwords.REQUEST_HANDSHAKE) {
            if (resultCode == RESULT_OK) {
                presenter.onResult(data);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isIncoming = getIntent().getBooleanExtra(MESSAGE_DIRECTION, false);
        if(isIncoming) return false;

        getMenuInflater().inflate(R.menu.menu_pep_status, menu);
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
                return true;
            case R.id.is_always_secure:
                return true;

            /*case R.id.action_explanation:
                showExplanationDialog();
                return true;*/
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
    }
}
