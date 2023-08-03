package com.fsck.k9.planck.ui.privacy.status;

import static com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.planck.models.PlanckIdentity;
import com.fsck.k9.planck.ui.tools.FeedbackTools;
import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import dagger.hilt.android.AndroidEntryPoint;
import foundation.pEp.jniadapter.Rating;
import security.planck.ui.toolbar.ToolBarCustomizer;

@AndroidEntryPoint
public class PlanckStatus extends K9Activity implements PlanckStatusView {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String SENDER = "isComposedKey";
    private static final String MYSELF = "myself";
    private static final String MESSAGE_REFERENCE = "messageReference";
    private static final String MESSAGE_DIRECTION = "messageDirection";
    public static final int REQUEST_STATUS = 5; // Do not use a value below 5 because it would collide with other constants in RecipientPresenter.
    public static final String CURRENT_RATING = "current_color";

    @Inject
    PlanckStatusPresenter presenter;

    @Inject
    PlanckStatusRendererBuilder rendererBuilder;

    @Inject
    ToolBarCustomizer toolBarCustomizer;

    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;

    @Bind(R.id.its_own_messageTV)
    TextView itsOwnMessageTV;

    RecyclerView.LayoutManager recipientsLayoutManager;
    private RVRendererAdapter<PlanckIdentity> recipientsAdapter;

    String sender = "";
    private MessageReference messageReference;
    private String myself = "";

    private static final String FORCE_UNENCRYPTED = "forceUnencrypted";
    private static final String ALWAYS_SECURE = "alwaysSecure";

    public static PendingIntent pendingIntentShowStatus(
            Activity context, String sender, MessageReference messageReference,
            Boolean isMessageIncoming, String myself, boolean forceUnencrypted, boolean alwaysSecure) {
        Intent intent = createShowStatusIntent(context, sender, messageReference, isMessageIncoming, myself, forceUnencrypted, alwaysSecure);
        return PendingIntent.getActivity(context, REQUEST_STATUS, intent, PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    @NotNull
    private static Intent createShowStatusIntent(
            Activity context, String sender, MessageReference messageReference,
            Boolean isMessageIncoming, String myself, boolean forceUnencrypted, boolean alwaysSecure) {
        Intent i = new Intent(context, PlanckStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
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

    public static void actionShowStatus(Activity context, String sender,
                                        MessageReference messageReference, Boolean isMessageIncoming, String myself) {
        Intent intent = createShowStatusIntent(context, sender, messageReference, isMessageIncoming, myself, false, false);
        context.startActivityForResult(intent, REQUEST_STATUS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.planck_status);
        ButterKnife.bind(PlanckStatus.this);
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(SENDER)
                && intent.hasExtra(MESSAGE_REFERENCE)) {
            sender = intent.getStringExtra(SENDER);
            myself = intent.getStringExtra(MYSELF);
            messageReference = MessageReference.parse(intent.getExtras().getString(MESSAGE_REFERENCE));
            boolean isMessageIncoming = intent.getBooleanExtra(MESSAGE_DIRECTION, false);
            boolean forceUnencrypted = intent.getBooleanExtra(FORCE_UNENCRYPTED, false);
            boolean alwaysSecure = intent.getBooleanExtra(ALWAYS_SECURE, false);
            presenter.initialize(this, isMessageIncoming,
                    new Address(sender), forceUnencrypted, alwaysSecure);
            presenter.loadMessage(messageReference);
        }

        presenter.restoreInstanceState(savedInstanceState);
        setUpActionBar();
        presenter.loadRecipients();
    }

    private void setUpActionBar() {
        initializeToolbar(true, R.string.pep_title_activity_privacy_status);
        colorActionBar();
    }

    private void colorActionBar() {
        toolBarCustomizer.setDefaultToolbarColor();
        toolBarCustomizer.setDefaultStatusBarColor();
    }

    @Override
    public void setupRecipients(List<PlanckIdentity> pEpIdentities) {
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        rendererBuilder.setUp(
                getOnHandshakeResultListener(),
                myself
        );
        recipientsAdapter = new RVRendererAdapter<>(rendererBuilder, pEpIdentities);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.setVisibility(View.VISIBLE);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    @NonNull
    private PlanckStatusRendererBuilder.HandshakeResultListener getOnHandshakeResultListener() {
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
    public void updateIdentities(List<PlanckIdentity> updatedIdentities) {
        recipientsAdapter.setCollection(new ListAdapteeCollection<>(updatedIdentities));
        recipientsAdapter.notifyDataSetChanged();
    }

    @Override
    public void showDataLoadError() {
        FeedbackTools.showLongFeedback(getRootView(), getResources().getString(R.string.status_loading_error));
    }

    @Override
    public void showTrustFeedback(String username) {
        FeedbackTools.showLongFeedback(
                getRootView(),
                getString(R.string.trust_identity_feedback, username)
        );
    }

    @Override
    public void showMistrustFeedback(String username) {
        FeedbackTools.showLongFeedback(getRootView(), getString(R.string.mistrust_identity_feedback, username));
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
        if (isIncoming || BuildConfig.IS_ENTERPRISE) return false;

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        presenter.saveInstanceState(outState);
    }

    @Override
    public void showItsOnlyOwnMsg() {
        recipientsView.setVisibility(View.GONE);
        itsOwnMessageTV.setVisibility(View.VISIBLE);
    }
}
