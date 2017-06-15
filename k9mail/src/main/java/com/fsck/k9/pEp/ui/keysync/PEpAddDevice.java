package com.fsck.k9.pEp.ui.keysync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.adapters.IdentitiesAdapter;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;

import org.pEp.jniadapter.Identity;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class PEpAddDevice extends PepActivity implements AddDeviceView {

    public static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    private static final String TRUSTWORDS = "trustwordsKey";
    private static final String MYSELF = "partnerAdress";
    private static final String PARTNER = "partnerUserUd";
    private static final String MY_ADRESS = "myAddress";
    private static final String EXPLANATION = "explanation";

    @Inject AddDevicePresenter presenter;

    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    @Bind(R.id.advenced_keys_title)
    TextView advancedKeysTextView;
    @Bind(R.id.explanation)
    TextView explanationTextView;
    @Bind(R.id.advanced_options_key_list)
    RecyclerView identitiesList;
    @Bind(R.id.show_long_trustwords)
    ImageView showLongTrustwords;
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
        setUpToolbar(false);
        getToolbar().setOverflowIcon(getResources().getDrawable(R.drawable.ic_language));
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

        setupFloatingWindow();
    }

    @Override
    public void search(String query) {

    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
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
            case R.id.home:
                finish();
                break;
            case R.id.action_advanced_options:
                if (advancedOptionsMenuItem.getTitle().equals(getResources().getString(R.string.advanced_options))) {
                    advancedOptionsMenuItem.setTitle(R.string.basic_options);
                    presenter.advancedOptionsClicked();
                } else {
                    advancedOptionsMenuItem.setTitle(getResources().getString(R.string.advanced_options));
                    presenter.basicOptionsClicked();
                }
                break;
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
            case R.id.catalan:
                return changeTrustwordsLanguage(0);
            case R.id.german:
                return changeTrustwordsLanguage(1);
            case R.id.spanish:
                return changeTrustwordsLanguage(2);
            case R.id.french:
                return changeTrustwordsLanguage(3);
            case R.id.turkish:
                return changeTrustwordsLanguage(4);
            case R.id.english:
                return changeTrustwordsLanguage(5);
        }
        return true;
    }

    private boolean changeTrustwordsLanguage(Integer languagePosition) {
        final CharSequence[] pEpLanguages = PEpUtils.getPEpLanguages();
        String language = pEpLanguages[languagePosition].toString();
        changeTrustwords(language);
        return true;
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
                FeedbackTools.showShortFeedback(getRootView(), throwable.getMessage());
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
                    FeedbackTools.showShortFeedback(getRootView(), throwable.getMessage());
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
                    FeedbackTools.showShortFeedback(getRootView(), throwable.getMessage());
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
    public void showPartnerFormat(Identity partner) {

    }

    @Override
    public void showCompletePartnerFormat(Identity partner) {
    }

    @Override
    public void close() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
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
        FeedbackTools.showShortFeedback(getRootView(), getResources().getString(R.string.openpgp_unknown_error));
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

    @OnLongClick(R.id.trustwords)
    public boolean onTrustwordsLongClick() {
        areTrustwordsShort = !areTrustwordsShort;
        if (areTrustwordsShort) {
            showLongTrustwords.setVisibility(View.VISIBLE);
        } else {
            showLongTrustwords.setVisibility(View.GONE);
        }
        changeTrustwordsLength(areTrustwordsShort);
        return true;
    }

    @OnClick(R.id.add_device_background)
    public void onClickOutside() {
        presenter.cancelHandshake();
    }

    @OnClick(R.id.show_long_trustwords)
    public void onClickShowLongTrustwords() {
        showLongTrustwords.setVisibility(View.GONE);
        areTrustwordsShort = !areTrustwordsShort;
        changeTrustwordsLength(areTrustwordsShort);
    }

    public class DismissKeysyncDialogReceiver extends BroadcastReceiver {
        public DismissKeysyncDialogReceiver() {
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            PEpAddDevice.this.finish();
        }
    }

    protected void setupFloatingWindow() {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
