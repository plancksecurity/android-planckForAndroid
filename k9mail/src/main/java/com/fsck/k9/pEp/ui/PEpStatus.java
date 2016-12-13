package com.fsck.k9.pEp.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.pEp.PEpStatusPresenter;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.ui.adapters.PEpIdentitiesAdapter;

import org.pEp.jniadapter.Rating;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PEpStatus extends PepColoredActivity implements PEpStatusView {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String MYSELF = "isComposedKey";
    private static final String RATING = "rating";
    private static final String MESSAGE_REFERENCE = "message_reference";
    public static final int REQUEST_STATUS = 2;

    @Inject PEpUtils pEpUtils;
    @Inject PEpStatusPresenter presenter;

    @Bind(R.id.pEpTitle)
    TextView pEpTitle;
    @Bind(R.id.pEpSuggestion)
    TextView pEpSuggestion;
    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    PEpIdentitiesAdapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    String myself = "";
    private MessageReference messageReference;

    public static void actionShowStatus(Activity context, Rating currentRating, String myself, MessageReference messageReference) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
        i.putExtra(CURRENT_RATING, currentRating.toString());
        i.putExtra(MYSELF, myself);
        i.putExtra(MESSAGE_REFERENCE, messageReference);
        context.startActivityForResult(i, REQUEST_STATUS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPepRating();
        setContentView(R.layout.pep_status);
        ButterKnife.bind(PEpStatus.this);
        initPep();
        presenter.initilize(this, uiCache, getpEp());
        if (getIntent() != null && getIntent().hasExtra(MYSELF)
                && getIntent().hasExtra(MESSAGE_REFERENCE)) {
            myself = getIntent().getStringExtra(MYSELF);
            messageReference = (MessageReference) getIntent().getExtras().get(MESSAGE_REFERENCE);
            presenter.loadMessage(messageReference);
        }
        restorePEpRating(savedInstanceState);
        setUpActionBar();
        presenter.loadRecipients();
        presenter.loadPepTexts(getpEpRating());
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
        recipientsAdapter = new PEpIdentitiesAdapter(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = ((Integer) view.getTag());
                presenter.updateTrust(position);
            }
        }, new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                new AlertDialog.Builder(view.getContext()).setMessage(R.string.handshake_reset_dialog_message).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int position = ((Integer) view.getTag());
                        presenter.updateTrust(position);
                    }
                }).setNegativeButton(R.string.cancel_action, null).show();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int partnerPosition = ((Integer) v.getTag());
                PEpTrustwords.actionRequestHandshake(activity, myself, partnerPosition);
            }
        });
        recipientsAdapter.setIdentities(pEpIdentities);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

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
        messageReference.saveLocalMessage(this, localMessage);
    }

    @Override
    public void setRating(Rating pEpRating) {
        setpEpRating(pEpRating);
        colorActionBar();
        presenter.loadPepTexts(pEpRating);
    }

    @Override
    public void showError(int status_loading_error) {
        Toast.makeText(getApplicationContext(), R.string.status_loading_error, Toast.LENGTH_LONG).show();
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
                .setMessage(uiCache.getExplanation(getpEpRating()))
                .setPositiveButton(R.string.okay_action,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
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
