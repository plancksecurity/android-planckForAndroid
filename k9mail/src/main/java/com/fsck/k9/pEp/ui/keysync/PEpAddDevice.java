package com.fsck.k9.pEp.ui.keysync;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.UIUtils;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.HandshakeData;
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
    private static final String MYSELF = "partnerAdress";
    private static final String PARTNER = "partnerUserUd";
    private static final String MY_ADRESS = "myAddress";
    private static final String EXPLANATION = "explanation";

    @Inject AddDevicePresenter presenter;

    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    @Bind(R.id.tvPartner)
    TextView partnerView;
    @Bind(R.id.advenced_keys_title)
    TextView advancedKeysTextView;
    @Bind(R.id.explanation)
    TextView explanationTextView;
    @Bind(R.id.advanced_options_key_list)
    RecyclerView identitiesList;
    private MenuItem advancedOptionsMenuItem;
    private String trustwordsLanguage;
    private Boolean areTrustwordsShort = true;
    private String fullTrustwords = "";
    private String shortTrustwords = "";
    private Identity partnerIdentity;
    private Identity myIdentity;
    private DismissKeysyncDialogReceiver receiver;
    private IntentFilter filter;

    public static Intent getActionRequestHandshake(Context context, String trustwords, Identity myself, Identity partner, String explanation) {
        Intent intent = new Intent(context, PEpAddDevice.class);
        intent.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        intent.putExtra(TRUSTWORDS, trustwords);
        intent.putExtra(PARTNER, partner);
        intent.putExtra(MYSELF, myself);
        intent.putExtra(EXPLANATION, explanation);
        return intent;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        receiver = new DismissKeysyncDialogReceiver();
        filter = new IntentFilter();
        filter.addAction("KEYSYNC_DISMISS");
        filter.setPriority(1);
        registerReceiver(receiver, filter);

        setContentView(R.layout.pep_add_device);
        ButterKnife.bind(this);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
        initPep();

        if (getIntent() != null) {
            explanationTextView.setText(intent.getStringExtra(EXPLANATION));
            if (intent.hasExtra(TRUSTWORDS)) {
                tvTrustwords.setText(getIntent().getStringExtra(TRUSTWORDS));
            }
            if (intent.hasExtra(MYSELF) && intent.hasExtra(PARTNER)) {
                partnerIdentity = (Identity) intent.getSerializableExtra(PARTNER);
                myIdentity = (Identity) intent.getSerializableExtra(MYSELF);
                List<Account> accounts = Preferences.getPreferences(PEpAddDevice.this).getAccounts();
                presenter.initialize(this, getpEp(), partnerIdentity, accounts);
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
            case R.id.action_language:
                showLanguageSelectionDialog();
                return true;
            case R.id.long_trustwords:
                if (item.getTitle().equals(getString(R.string.pep_menu_long_trustwords))){
                    item.setTitle(R.string.pep_menu_short_trustwords);
                    changeTrustwordsLength(false);
                }
                else{
                    item.setTitle(getString(R.string.pep_menu_long_trustwords));
                    changeTrustwordsLength(true);
                }

                return true;
        }
        return true;
    }

    private void showLanguageSelectionDialog() {
        final CharSequence[] pEpLanguages = PEpUtils.getPEpLanguages();
        CharSequence[] displayLanguages = UIUtils.prettifyLanguages(pEpLanguages);
        new AlertDialog.Builder(PEpAddDevice.this).setTitle(getResources().getString(R.string.settings_language_label))
                .setItems(displayLanguages, (dialogInterface, i) -> {
                    String language = pEpLanguages[i].toString();
                    changeTrustwords(language);
                }).create().show();
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;

//        myIdentity = getpEp().updateIdentity(myIdentity);
//        partnerIdentity = getpEp().updateIdentity(partnerIdentity);

        getpEp().obtainTrustwords(myIdentity, partnerIdentity, language, areTrustwordsShort, new PEpProvider.ResultCallback<HandshakeData>() {
            @Override
            public void onLoaded(HandshakeData handshakeData) {
                if (areTrustwordsShort) {
                    shortTrustwords = handshakeData.getShortTrustwords();
                    tvTrustwords.setText(shortTrustwords);
                } else {
                    fullTrustwords = handshakeData.getFullTrustwords();
                    tvTrustwords.setText(fullTrustwords);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(PEpAddDevice.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changeTrustwordsLength(Boolean areShort) {
        areTrustwordsShort = areShort;
        if (areShort) {
            getpEp().obtainTrustwords(myIdentity, partnerIdentity, trustwordsLanguage, areTrustwordsShort, new PEpProvider.ResultCallback<HandshakeData>() {
                @Override
                public void onLoaded(HandshakeData handshakeData) {
                    shortTrustwords = handshakeData.getShortTrustwords();
                    tvTrustwords.setText(shortTrustwords);
                }

                @Override
                public void onError(Throwable throwable) {
                    Toast.makeText(PEpAddDevice.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            getpEp().obtainTrustwords(myIdentity, partnerIdentity, trustwordsLanguage, areTrustwordsShort, new PEpProvider.ResultCallback<HandshakeData>() {
                @Override
                public void onLoaded(HandshakeData handshakeData) {
                    fullTrustwords = handshakeData.getFullTrustwords();
                    tvTrustwords.setText(fullTrustwords);
                }

                @Override
                public void onError(Throwable throwable) {
                    Toast.makeText(PEpAddDevice.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        loadTrustwords();
    }

    private void loadTrustwords() {
        //Actually what is heavy is update identity and myself.
        getpEp().obtainTrustwords(myIdentity, partnerIdentity, trustwordsLanguage, areTrustwordsShort, new PEpProvider.ResultCallback<HandshakeData>() {
            @Override
            public void onLoaded(final HandshakeData handshakeData) {
                fullTrustwords = handshakeData.getFullTrustwords();
                shortTrustwords = handshakeData.getShortTrustwords();
                if (areTrustwordsShort) {
                    tvTrustwords.setText(shortTrustwords);
                } else {
                    tvTrustwords.setText(fullTrustwords);
                }

                //myself = handshakeData.getMyself();
                //partner = handshakeData.getPartner();
                //wrongTrustWords.setVisibility(View.VISIBLE);
                //confirmTrustWords.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    public class DismissKeysyncDialogReceiver extends BroadcastReceiver {
        public DismissKeysyncDialogReceiver() {
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            PEpAddDevice.this.finish();
        }
    }
}
