package com.fsck.k9.pEp.ui.privacy.status;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.ui.PepColoredActivity;
import com.fsck.k9.pEp.ui.adapters.PEpIdentitiesAdapter;

import org.pEp.jniadapter.Rating;

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

    @Inject PEpUtils pEpUtils;
    @Inject PEpStatusPresenter presenter;

    @Bind(R.id.pEpTitle)
    TextView pEpTitle;
    @Bind(R.id.title_status_badge)
    ImageView statusBadge;
    @Bind(R.id.pEpSuggestion)
    TextView pEpSuggestion;
    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    PEpIdentitiesAdapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    String sender = "";
    private MessageReference messageReference;
    private String myself = "";

    public static void actionShowStatus(Activity context, Rating currentRating, String sender, MessageReference messageReference, Boolean isMessageIncoming, String myself) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
        i.putExtra(CURRENT_RATING, currentRating.toString());
        i.putExtra(SENDER, sender);
        i.putExtra(MYSELF, myself);
        i.putExtra(MESSAGE_REFERENCE, messageReference);
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
            messageReference = (MessageReference) getIntent().getExtras().get(MESSAGE_REFERENCE);
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
    protected void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(this))
                .pEpModule(new PEpModule(this, getLoaderManager(), getFragmentManager()))
                .build()
                .inject(this);
    }

    private void restorePEpRating(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pEpRating = (Rating) savedInstanceState.getSerializable(RATING);
            setpEpRating(pEpRating);
        }
    }

    private void setUpActionBar() {
        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(getString(R.string.pep_title_activity_privacy_status));
            colorActionBar();
        }
    }

    @Override
    public void setupRecipients(List<PEpIdentity> pEpIdentities) {
        final Activity activity = this;
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        recipientsView.setVisibility(View.VISIBLE);
        recipientsAdapter = new PEpIdentitiesAdapter(getOnResetGreenClickListener(), getOnResetRedClickListener(), getOnHandshakeClickListener());
        recipientsAdapter.setIdentities(pEpIdentities);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    @NonNull
    private View.OnClickListener getOnHandshakeClickListener() {
        return v -> {
            int partnerPosition = ((Integer) v.getTag());
            PEpTrustwords.actionRequestHandshake(PEpStatus.this, myself, partnerPosition);
        };
    }

    @NonNull
    private View.OnClickListener getOnResetRedClickListener() {
        return view -> new AlertDialog.Builder(view.getContext())
                .setMessage(R.string.handshake_reset_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    Button button = (Button) view;
                    button.setText(R.string.message_list_loading);
                    int position = ((Integer) view.getTag());
                    presenter.resetRecipientTrust(position);
                }).setNegativeButton(R.string.cancel_action, null).show();
    }

    @NonNull
    private View.OnClickListener getOnResetGreenClickListener() {
        return view -> {
            Button button = (Button) view;
            button.setText(R.string.message_list_loading);
            int position = ((Integer) view.getTag());
            presenter.resetRecipientTrust(position);
        };
    }

    @Override
    public void setupBackIntent(Rating rating) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(CURRENT_RATING, rating);
        setResult(Activity.RESULT_OK, returnIntent);
    }

    @Override
    public void showPEpTexts(String title, String suggestion) {
        pEpTitle.setText(title);
        pEpSuggestion.setText(suggestion);
    }

    @Override
    public void updateIdentities(List<PEpIdentity> updatedIdentities) {
        recipientsAdapter.setIdentities(updatedIdentities);
        recipientsAdapter.notifyDataSetChanged();
    }

    @Override
    public void saveLocalMessage(LocalMessage localMessage) {
        try {
            messageReference.saveLocalMessage(this, localMessage);
        } catch (Exception e) {
            Toast.makeText(PEpStatus.this, R.string.status_loading_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void setRating(Rating pEpRating) {
        setpEpRating(pEpRating);
        colorActionBar();
        presenter.loadRating(pEpRating);
    }

    @Override
    public void showError(int status_loading_error) {
        Toast.makeText(getApplicationContext(), R.string.status_loading_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showBadge(Rating rating) {
        statusBadge.setVisibility(View.VISIBLE);
        statusBadge.setImageDrawable(PEpUtils.getDrawableForRating(PEpStatus.this, rating));
    }

    @Override
    public void hideBadge() {
        statusBadge.setVisibility(View.GONE);
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
        if (requestCode == PEpTrustwords.HANDSHAKE_REQUEST) {
            if (resultCode == RESULT_OK) {
                int position = data.getIntExtra(PEpTrustwords.PARTNER_POSITION, PEpTrustwords.DEFAULT_POSITION);
                presenter.onResult(position);
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            case R.id.action_explanation:
                showExplanationDialog();
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
    }
}
