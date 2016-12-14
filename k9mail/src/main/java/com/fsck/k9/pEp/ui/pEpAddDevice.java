package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;

import org.pEp.jniadapter.Identity;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class pEpAddDevice extends PepColoredActivity {

    public static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    private static final String TRUSTWORDS = "trustwordsKey";
    private static final String PARTNER_ADRESS = "partnerAdress";
    private static final String PARTNER_USER_ID = "partnerUserUd";

    Context context;
    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    @Bind(R.id.tvPartner)
    TextView partnerView;

    Identity partner;
    public static Intent getActionRequestHandshake(Context context, String trustwords, Identity partner) {
        Intent intent = new Intent(context, pEpAddDevice.class);
        intent.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        intent.putExtra(TRUSTWORDS, trustwords);
        intent.putExtra(PARTNER_USER_ID, partner.user_id);
        intent.putExtra(PARTNER_ADRESS, partner.address);
        return intent;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        setContentView(R.layout.pep_add_device);
        ButterKnife.bind(this);
        context = getApplicationContext();
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
        initPep();

        if (getIntent() != null) {
            if (intent.hasExtra(TRUSTWORDS)) {
                tvTrustwords.setText(getIntent().getStringExtra(TRUSTWORDS));
            }
            if (intent.hasExtra(PARTNER_ADRESS) && intent.hasExtra(PARTNER_USER_ID)) {
                partner = new Identity();
                partner.user_id = intent.getStringExtra(PARTNER_USER_ID);
                partner.address = intent.getStringExtra(PARTNER_ADRESS);
                partner = getpEp().updateIdentity(partner);
                if (!partner.username.equals(partner.address)) {
                    partnerView.setText(String.format(getString(R.string.pep_complete_partner_format), partner.username, partner.address));
                } else {
                    partnerView.setText(String.format(getString(R.string.pep_partner_format),partner.address));
                }
            }

        }


    }


    @OnClick(R.id.confirmTrustWords)
    public void confirmTrustwords() {
        getpEp().acceptHandshake(partner);
        finish();

    }

    @OnClick(R.id.wrongTrustwords)
    public void wrongTrustwords() {
        getpEp().rejectHandshake(partner);
        finish();

    }

    @Override
    public void onBackPressed() {
        getpEp().cancelHandshake(partner);
        super.onBackPressed();
    }

    @Override
    public PEpProvider getpEp() {
        return
                ((K9)getApplication()).getpEpSyncProvider();
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
}
