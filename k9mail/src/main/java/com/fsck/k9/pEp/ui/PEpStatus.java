package com.fsck.k9.pEp.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.pEp.PEpProvider;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PEpStatus extends PepColoredActivity implements ChangeColorListener{

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String MYSELF = "isComposedKey";
    private static final String MESSAGE_REFERENCE = "message_reference";

    @Bind(R.id.pEpTitle)
    TextView pEpTitle;
    @Bind(R.id.pEpSuggestion)
    TextView pEpSuggestion;
    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    RecyclerView.Adapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    String myself = "";
    private MessageReference messageReference;


    public static void actionShowStatus(Context context, Rating currentRating, String myself, MessageReference messageReference) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
        i.putExtra(CURRENT_RATING, currentRating.toString());
        i.putExtra(MYSELF, myself);
        i.putExtra(MESSAGE_REFERENCE, messageReference);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPepRating();
        setContentView(R.layout.pep_status);
        ButterKnife.bind(PEpStatus.this);
        if (getIntent() != null && getIntent().hasExtra(MYSELF)) {
            myself = getIntent().getStringExtra(MYSELF);
        }
        messageReference = (MessageReference) getIntent().getExtras().get(MESSAGE_REFERENCE);
        initPep();
        setUpActionBar();
        setUpContactList(myself, getpEp());
        loadPepTexts();

    }


    private void loadPepTexts() {
        pEpTitle.setText(uiCache.getTitle(getpEpRating()));
        pEpSuggestion.setText(uiCache.getExplanation(getpEpRating()));
    }

    private void setUpActionBar() {
        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(getString(R.string.pep_title_activity_privacy_status));
            colorActionBar();
            colorActionBar();
        }
    }

    private void colorActionBar() {
        ActionBar actionBar = getActionBar() ;
        if (actionBar != null) {
            PEpUtils.colorActionBar(ui, actionBar, m_pEpColor);
        }
    }


    private void setUpContactList(String myself, PEpProvider pEp) {
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        recipientsView.setVisibility(View.VISIBLE);
        recipientsAdapter = new RecipientsAdapter(this, uiCache.getRecipients(), pEp, myself, this);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    @Override
    public void onRatingChanged(Rating rating) {
        setpEpRating(rating);
        colorActionBar();
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
                Identity partner = uiCache.getRecipients().get(position);
                Rating pEpRating = getpEp().identityRating(partner);
                setpEpRating(pEpRating);
                MessageLoaderHelper messageLoaderHelper = new MessageLoaderHelper(this, getLoaderManager(), getFragmentManager(), callback(pEpRating));
                messageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, null);
                recipientsAdapter.notifyDataSetChanged();
                colorActionBar();
            }
        }
    }

    public MessageLoaderHelper.MessageLoaderCallbacks callback(final Rating rating) {
        return new MessageLoaderHelper.MessageLoaderCallbacks() {
            @Override
            public void onMessageDataLoadFinished(LocalMessage message) {
                message.setpEpRating(rating);
                onRatingChanged(rating);
            }

            @Override
            public void onMessageDataLoadFailed() {
                Toast.makeText(getContext(), R.string.status_loading_error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo) {
                Log.d("mierda", " ");
            }

            @Override
            public void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo) {
                Log.d("mierda 2", " ");
            }

            @Override
            public void setLoadingProgress(int current, int max) {
                Log.d("mierda 3", " ");
            }

            @Override
            public void onDownloadErrorMessageNotFound() {
                Log.d("mierda 4", " ");
            }

            @Override
            public void onDownloadErrorNetworkError() {
                Log.d("mierda 5", " ");
            }

            @Override
            public void startIntentSenderForMessageLoaderHelper(IntentSender si, int requestCode, Intent fillIntent,
                                                                int flagsMask, int flagValues, int extraFlags) {
                Log.d("mierda 6", " ");
            }
        };
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

    public Context getContext() {
        return this;
    }

}
