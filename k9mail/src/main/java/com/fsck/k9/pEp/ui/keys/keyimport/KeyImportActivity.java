package com.fsck.k9.pEp.ui.keys.keyimport;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;



public class KeyImportActivity extends PepActivity implements KeyImportView {
    @Inject
    KeyImportPresenter presenter;
    @Bind(R.id.tvFpr)
    TextView tvFpr;
    @Bind(R.id.tvAddress)
    TextView tvAddress;
    @Bind(R.id.tvFrom)
    TextView tvFrom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_key_dialog);
        ButterKnife.bind(this);
        setupFloatingWindow();
        Intent intent = getIntent();
        presenter.initialize(this, intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_FPR),
                intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_ADDRESS),
                intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_USERNAME),
                intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_FROM));
    }

    @Override
    public void search(String query) {
        //NOP
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void showPositiveFeedback() {
        FeedbackTools.showLongFeedback(getRootView(), getString(R.string.key_import_accept_feedback));
    }

    @Override
    public void renderDialog(PEpProvider.KeyDetail keyDetail, String from) {
        tvFrom.setText(String.format(getString(R.string.pep_from_format), from));
        tvFpr.setText(presenter.formatFingerprint(keyDetail.getFpr()));
        tvAddress.setText(String.format(getString(R.string.pep_user_address_format), keyDetail.getUsername(), keyDetail.getStringAddress()));

    }

    @Override
    public void showNegativeFeedback() {
        FeedbackTools.showLongFeedback(getRootView(), getString(R.string.key_import_reject_feedback));
    }

    protected void setupFloatingWindow() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.floating_width);
        params.height = getResources().getDimensionPixelSize(R.dimen.floating_height);
        params.alpha = 1;
        params.dimAmount = 0.4f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    @OnClick(R.id.bAccept)
    void onAccept() {
        presenter.onAccept();
    }

    @OnClick(R.id.bReject)
    void onReject() {
        presenter.onReject();
    }

    public static void actionShowImportDialog(Context context, Intent intent) {
        if (!intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_FPR) ||
                !intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_ADDRESS) ||
                !intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_USERNAME) ||
                !intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_FROM)) {
            throw new IllegalStateException("The provided intent does not contain the required extras");
        }

        Intent dialogIntent = new Intent(context, KeyImportActivity.class);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        dialogIntent.putExtras(intent);
        context.startActivity(dialogIntent);
    }
}
