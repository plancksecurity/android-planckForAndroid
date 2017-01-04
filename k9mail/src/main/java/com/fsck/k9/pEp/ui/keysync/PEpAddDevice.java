package com.fsck.k9.pEp.ui.keysync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.PepColoredActivity;
import com.fsck.k9.pEp.ui.adapters.IdentitiesAdapter;

import org.pEp.jniadapter.Identity;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PEpAddDevice extends PepColoredActivity implements AddDeviceView {

    public static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    private static final String TRUSTWORDS = "trustwordsKey";
    private static final String PARTNER_ADRESS = "partnerAdress";
    private static final String PARTNER_USER_ID = "partnerUserUd";

    @Inject AddDevicePresenter presenter;

    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    @Bind(R.id.tvPartner)
    TextView partnerView;
    @Bind(R.id.advenced_keys_title)
    TextView advancedKeysTextView;
    @Bind(R.id.advanced_options_key_list)
    RecyclerView identitiesList;
    private MenuItem advancedOptionsMenuItem;

    public static Intent getActionRequestHandshake(Context context, String trustwords, Identity partner) {
        Intent intent = new Intent(context, PEpAddDevice.class);
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
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
        initPep();

        if (getIntent() != null) {
            if (intent.hasExtra(TRUSTWORDS)) {
                tvTrustwords.setText(getIntent().getStringExtra(TRUSTWORDS));
            }
            if (intent.hasExtra(PARTNER_ADRESS) && intent.hasExtra(PARTNER_USER_ID)) {
                String partnerUserId = intent.getStringExtra(PARTNER_USER_ID);
                String partnerAddress = intent.getStringExtra(PARTNER_ADRESS);
                List<Account> accounts = Preferences.getPreferences(PEpAddDevice.this).getAccounts();
                presenter.initialize(this, getpEp(), partnerUserId, partnerAddress, accounts);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_device, menu);
        advancedOptionsMenuItem = menu.findItem(R.id.action_advanced_options);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_advanced_options:
                if (advancedOptionsMenuItem.getTitle().equals(getResources().getString(R.string.advanced_options))) {
                    advancedOptionsMenuItem.setTitle(R.string.basic_options);
                    presenter.advancedOptionsClicked();
                } else {
                    advancedOptionsMenuItem.setTitle(getResources().getString(R.string.advanced_options));
                    presenter.basicOptionsClicked();
                }
                break;
        }
        return true;
    }

    @OnClick(R.id.confirmTrustWords)
    public void confirmTrustwords() {
        presenter.acceptHandshake();
    }

    @OnClick(R.id.wrongTrustwords)
    public void wrongTrustwords() {
        presenter.rejectHandshake();
    }

    @Override
    public void onBackPressed() {
        presenter.cancelHandshake();
    }

    @Override
    public PEpProvider getpEp() {
        return ((K9)getApplication()).getpEpSyncProvider();
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

    @Override
    public void showPartnerFormat(Identity partner) {
        partnerView.setText(String.format(getString(R.string.pep_partner_format),partner.address));
    }

    @Override
    public void showCompletePartnerFormat(Identity partner) {
        partnerView.setText(String.format(getString(R.string.pep_complete_partner_format), partner.username, partner.address));
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void goBack() {
        super.onBackPressed();
    }

    @Override
    public void showIdentities(List<Identity> identities) {
        advancedKeysTextView.setVisibility(View.VISIBLE);
        identitiesList.setVisibility(View.VISIBLE);
        IdentitiesAdapter identityAdapter = new IdentitiesAdapter(identities, (identity, check) -> presenter.identityCheckStatusChanged(identity, check));
        identitiesList.setLayoutManager(new LinearLayoutManager(this));
        identitiesList.setAdapter(identityAdapter);
    }

    @Override
    public void showError() {
        Toast.makeText(this, R.string.openpgp_unknown_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void hideIdentities() {
        advancedKeysTextView.setVisibility(View.GONE);
        identitiesList.setVisibility(View.GONE);
    }
}
