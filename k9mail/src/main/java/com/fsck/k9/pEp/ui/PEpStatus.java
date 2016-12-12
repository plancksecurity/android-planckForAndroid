package com.fsck.k9.pEp.ui;

import android.app.ActionBar;
import android.app.Activity;
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
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.adapters.PEpIdentitiesAdapter;
import com.fsck.k9.pEp.ui.models.PEpIdentity;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PEpStatus extends PepColoredActivity {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String MYSELF = "isComposedKey";
    private static final String RATING = "rating";
    private static final String MESSAGE_REFERENCE = "message_reference";
    public static final int REQUEST_STATUS = 2;

    @Bind(R.id.pEpTitle)
    TextView pEpTitle;
    @Bind(R.id.pEpSuggestion)
    TextView pEpSuggestion;
    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    PEpIdentitiesAdapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    String myself = "";
    private LocalMessage localMessage;
    private PEpStatusController pEpStatusController;

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
        pEpStatusController = new PEpStatusController();
        if (getIntent() != null && getIntent().hasExtra(MYSELF)
                && getIntent().hasExtra(MESSAGE_REFERENCE)) {
            myself = getIntent().getStringExtra(MYSELF);
            pEpStatusController.loadMessage(this);
        }
        restorePEpRating(savedInstanceState);
        initPep();
        setUpActionBar();
        setUpContactList(myself, getpEp());
        loadPepTexts();
    }

    private void restorePEpRating(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pEpRating = (Rating) savedInstanceState.getSerializable(RATING);
            setpEpRating(pEpRating);
        }
    }


    private void loadPepTexts() {
        pEpTitle.setText(uiCache.getTitle(getpEpRating()));
        pEpSuggestion.setText(uiCache.getSuggestion(getpEpRating()));
    }

    private void setUpActionBar() {
        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(getString(R.string.pep_title_activity_privacy_status));
            colorActionBar();
        }
    }


    private void setUpContactList(final String myself, final PEpProvider pEp) {
        final Activity activity = this;
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        recipientsView.setVisibility(View.VISIBLE);
        final ArrayList<PEpIdentity> recipients = pEpStatusController.getRecipients();
        recipientsAdapter = new PEpIdentitiesAdapter(recipients, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = ((Integer) view.getTag());
                Identity id = recipientsAdapter.get(position);
                id = pEp.updateIdentity(id);
                Log.i("KeysAdapter", "onResetGreenClick " + id.address);
                pEp.resetTrust(id);
                recipientsAdapter.notifyDataSetChanged();
                onRatingChanged(Rating.pEpRatingReliable);
            }
        }, new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                new AlertDialog.Builder(view.getContext()).setMessage(R.string.handshake_reset_dialog_message).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int position = ((Integer) view.getTag());
                        Identity id = recipientsAdapter.get(position);
                        id = pEp.updateIdentity(id);
                        Log.i("KeysAdapter", "onResetGreenClick " + id.address);
                        pEp.resetTrust(id);
                        recipientsAdapter.notifyDataSetChanged();
                        onRatingChanged(Rating.pEpRatingReliable);
                        recipientsAdapter.handshake(view);
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
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    private void onRatingChanged(Rating rating) {
        if (localMessage != null) {
            localMessage.setpEpRating(rating);
            localMessage.setHeader(MimeHeader.HEADER_PEP_RATING, PEpUtils.ratingToString(rating));
            pEpStatusController.saveLocalMessage(getApplicationContext(), localMessage);
        }
        setpEpRating(rating);
        colorActionBar();
        Intent returnIntent = new Intent();
        returnIntent.putExtra(CURRENT_RATING, rating);
        setResult(Activity.RESULT_OK, returnIntent);
        loadPepTexts();
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
                loadPepRating();
                initPep();
                setUpActionBar();
                setUpContactList(myself, getpEp());
                loadPepTexts();
            }
        }
    }

    public MessageLoaderHelper.MessageLoaderCallbacks callback() {
        return new MessageLoaderHelper.MessageLoaderCallbacks() {
            @Override
            public void onMessageDataLoadFinished(LocalMessage message) {
                localMessage = message;
            }

            @Override
            public void onMessageDataLoadFailed() {
                Toast.makeText(getApplicationContext(), R.string.status_loading_error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo) {
            }

            @Override
            public void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo) {
            }

            @Override
            public void setLoadingProgress(int current, int max) {
            }

            @Override
            public void onDownloadErrorMessageNotFound() {
            }

            @Override
            public void onDownloadErrorNetworkError() {
            }

            @Override
            public void startIntentSenderForMessageLoaderHelper(IntentSender si, int requestCode, Intent fillIntent,
                                                                int flagsMask, int flagValues, int extraFlags) {
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(RATING, pEpRating);
    }

    private class PEpStatusController {
        private MessageReference messageReference;

        void loadMessage(Context context) {
            messageReference = (MessageReference) getIntent().getExtras().get(MESSAGE_REFERENCE);
            if (messageReference != null) {
                MessageLoaderHelper messageLoaderHelper = new MessageLoaderHelper(context, getLoaderManager(), getFragmentManager(), callback());
                messageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, null);
            }
        }

        void saveLocalMessage(Context context, LocalMessage localMessage) {
            messageReference.saveLocalMessage(context, localMessage);
        }

        ArrayList<PEpIdentity> getRecipients() {
            ArrayList<Identity> recipients = uiCache.getRecipients();
            return mapRecipients(recipients);
        }

        private ArrayList<PEpIdentity> mapRecipients(ArrayList<Identity> recipients) {
            ArrayList<PEpIdentity> pEpIdentities = new ArrayList<>(recipients.size());
            for (Identity recipient : recipients) {
                pEpIdentities.add(mapRecipient(recipient));
            }
            return pEpIdentities;
        }

        private PEpIdentity mapRecipient(Identity recipient) {
            PEpIdentity pEpIdentity = new PEpIdentity();
            pEpIdentity.address = recipient.address;
            pEpIdentity.comm_type = recipient.comm_type;
            pEpIdentity.flags = recipient.flags;
            pEpIdentity.fpr = recipient.fpr;
            pEpIdentity.lang = recipient.lang;
            pEpIdentity.me = recipient.me;
            pEpIdentity.user_id = recipient.user_id;
            pEpIdentity.username = recipient.username;
            pEpIdentity.setRating(getpEp().identityRating(recipient));
            return pEpIdentity;
        }
    }
}
