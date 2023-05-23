package com.fsck.k9.planck.ui.keysync;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.manualsync.WizardActivity;
import com.fsck.k9.planck.ui.HandshakeData;
import com.fsck.k9.planck.ui.adapters.IdentitiesAdapter;
import com.fsck.k9.planck.ui.tools.FeedbackTools;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import foundation.pEp.jniadapter.Identity;
import security.planck.ui.resources.ResourcesProvider;
import timber.log.Timber;

public class PlanckAddDevice extends WizardActivity implements AddDeviceView {

    public static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    private static final String TRUSTWORDS = "trustwordsKey";
    private static final String MYSELF = "partnerAdress";
    private static final String PARTNER = "partnerUserUd";
    private static final String MY_ADRESS = "myAddress";
    private static final String EXPLANATION = "explanation";
    private static final String MANUAL = "manual";
    private static final String PEP_KEY_LIST = "keylist";
    public static final String RESULT = "result";
    public static final String IS_PGP = "PGP";
    public static final int REQUEST_ADD_DEVICE_HANDSHAKE = 1;

    @Inject AddDevicePresenter presenter;

    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    @Bind(R.id.advenced_keys_title)
    TextView advancedKeysTextView;
    @Bind(R.id.toolbar_pEp_title_detail)
    TextView toolbarTitleDetail;
    @Bind(R.id.explanation)
    TextView explanationTextView;
    @Bind(R.id.advanced_options_key_list)
    RecyclerView identitiesList;
    @Bind(R.id.show_long_trustwords)
    ImageView showLongTrustwords;

    @Inject
    ResourcesProvider resourcesProvider;

    @Bind(R.id.fingerprintView) View fingerPrints;
    @Bind(R.id.partnerLabel) TextView partnerLabel;
    @Bind(R.id.partnerFpr) TextView partnerFpr;
    @Bind(R.id.myselfLabel) TextView myselfLabel;
    @Bind(R.id.myselfFpr) TextView myselfFpr;

    private MenuItem advancedOptionsMenuItem;
    private String trustwordsLanguage;
    private Boolean areTrustwordsShort = true;
    private String fullTrustwords = "";
    private String shortTrustwords = "";
    private Identity partnerIdentity;
    private Identity myIdentity;
    private DismissKeysyncDialogReceiver receiver;
    private IntentFilter filter;

    public static Intent getActionRequestHandshake(Context context, String trustwords,
                                                   Identity myself, Identity partner,
                                                   String explanation, boolean isManualSync) {
        Intent intent = new Intent(context, PlanckAddDevice.class);
        intent.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        intent.putExtra(TRUSTWORDS, trustwords);
        intent.putExtra(PARTNER, partner);
        intent.putExtra(MYSELF, myself);
        intent.putExtra(EXPLANATION, explanation);
        intent.putExtra(MANUAL, isManualSync);
        return intent;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        receiver = new DismissKeysyncDialogReceiver(this);
        filter = new IntentFilter();
        filter.addAction("KEYSYNC_DISMISS");
        filter.setPriority(1);
        registerReceiver(receiver, filter);

        setContentView(R.layout.planck_add_device);
        ButterKnife.bind(this);
        setUpToolbar(false);

        int resource = resourcesProvider.getAttributeResource(R.attr.iconLanguageGray);
        getToolbar().setOverflowIcon(ContextCompat.getDrawable(this, resource));
        initPep();

        if (intent != null) {
            explanationTextView.setText(intent.getStringExtra(EXPLANATION));
            if (intent.hasExtra(TRUSTWORDS)) {
                tvTrustwords.setText(getIntent().getStringExtra(TRUSTWORDS));
            }
            if (intent.hasExtra(MYSELF) && intent.hasExtra(PARTNER) && intent.hasExtra(MANUAL)) {
                partnerIdentity = (Identity) intent.getSerializableExtra(PARTNER);
                myIdentity = (Identity) intent.getSerializableExtra(MYSELF);
                boolean isManualSync = intent.getBooleanExtra(MANUAL, false);
                List<Account> accounts = Preferences.getPreferences(PlanckAddDevice.this).getAccounts();

                try {
                    showDebugInfo();
                } catch (Exception ignored) {}

                intent.removeExtra(TRUSTWORDS);
                intent.removeExtra(MYSELF);
                intent.removeExtra(PARTNER);
                presenter.initialize(this, getPlanck(), myIdentity, partnerIdentity, accounts, isManualSync, intent.getStringExtra(PEP_KEY_LIST));
            }
        }

        setUpFloatingWindow();
    }

    private void showDebugInfo() {
        Timber.e("------------------------");
        Timber.e(getIntent().getStringExtra(TRUSTWORDS));
        Timber.e(myIdentity.fpr);
        Timber.e(partnerIdentity.fpr);
        //FeedbackTools.showLongFeedback(getRootView(), myIdentity.fpr + "::" + partnerIdentity.fpr);
        Timber.e("------------------------");
    }

    @Override
    public void search(String query) {

    }

    @Override
    public void inject() {
        getPlanckComponent().inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (presenter.ispEp()) {
            getMenuInflater().inflate(R.menu.menu_add_device, menu);
            advancedOptionsMenuItem = menu.findItem(R.id.action_advanced_options);
        }
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
                    try {
                        showDebugInfo();
                    } catch (Exception ignored) {}
                }
                else{
                    item.setTitle(getString(R.string.pep_menu_long_trustwords));
                    changeTrustwordsLength(true);
                    try {
                        showDebugInfo();
                    } catch (Exception ignored) {}
                }

                return true;
            case R.id.english:
                return changeTrustwordsLanguage(0);
            case R.id.german:
                return changeTrustwordsLanguage(1);

        }
        return true;
    }

    private boolean changeTrustwordsLanguage(Integer languagePosition) {
        try {
            showDebugInfo();
        } catch (Exception ignored) {}
        final List pEpLanguages = PlanckUtils.getPlanckLocales();
        String language = pEpLanguages.get(languagePosition).toString();
        changeTrustwords(language);
        return true;
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;

//        myIdentity = getpEp().updateIdentity(myIdentity);
//        partnerIdentity = getpEp().updateIdentity(partnerIdentity);

        getPlanck().obtainTrustwords(myIdentity, partnerIdentity, language,
                true,
                new PlanckProvider.ResultCallback<HandshakeData>() {
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
            getPlanck().obtainTrustwords(myIdentity, partnerIdentity,
                    trustwordsLanguage,
                    true,
                    new PlanckProvider.ResultCallback<HandshakeData>() {
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
            getPlanck().obtainTrustwords(myIdentity, partnerIdentity,
                    trustwordsLanguage,
                    true,
                    new PlanckProvider.ResultCallback<HandshakeData>() {
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
    public void close(boolean accepted) {
        Log.i("pEpKeyImport", "CLOSE");

        Intent returnIntent = new Intent();
        returnIntent.putExtra(RESULT, accepted ? Result.ACCEPTED : Result.REJECTED);
        if (presenter.isPGP()) returnIntent.putExtra(IS_PGP, true);

        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void goBack() {
        setResult(RESULT_CANCELED);
        finish();
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
        FeedbackTools.showShortFeedback(getRootView(), getResources().getString(R.string.status_loading_error));
    }

    @Override
    public void hideIdentities() {
        advancedKeysTextView.setVisibility(View.GONE);
        identitiesList.setVisibility(View.GONE);
    }

    @Override
    public void showFPR() {
        tvTrustwords.setVisibility(View.GONE);
        showLongTrustwords.setVisibility(View.GONE);
        fingerPrints.setVisibility(View.VISIBLE);

        if (presenter.myselfHasUserId()) {
            myselfLabel.setText(String.format(getString(R.string.pep_complete_myself_format), presenter.getMyselfUsername(), presenter.getMyselfAddress()));
        } else {
            myselfLabel.setText(String.format(getString(R.string.pep_myself_format),presenter.getMyselfAddress()));
        }
        myselfFpr.setText(PlanckUtils.formatFpr(presenter.getMyFpr()));
        partnerFpr.setText(PlanckUtils.formatFpr(presenter.getPartnerFpr()));


    }

    @Override
    public void showKeySyncTitle() {
       toolbarTitleDetail.setText("Key Sync");
    }

    @Override
    protected void onResume() {
        super.onResume();
        trustwordsLanguage = Locale.getDefault().getLanguage();
        loadTrustwords();
    }

    private void loadTrustwords() {
        //Actually what is heavy is update identity and myself.
        getPlanck().obtainTrustwords(myIdentity, partnerIdentity,
                trustwordsLanguage,
                true,
                new PlanckProvider.ResultCallback<HandshakeData>() {
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
        //presenter.cancelSync();
    }

    @OnClick(R.id.show_long_trustwords)
    public void onClickShowLongTrustwords() {
        showLongTrustwords.setVisibility(View.GONE);
        areTrustwordsShort = !areTrustwordsShort;
        changeTrustwordsLength(areTrustwordsShort);
    }

    public static Intent getActionRequestHandshake(Context context, Identity myself, Identity partner,
                                                   List<String> keys, String explanation) {
        Intent intent = new Intent(context, PlanckAddDevice.class);
        intent.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        StringBuilder keyList = new StringBuilder();
        intent.putExtra(MYSELF, myself);
        intent.putExtra(PARTNER, partner);

        for (String key : keys) {
            keyList.append(key).append(PlanckProvider.PLANCK_KEY_LIST_SEPARATOR);
        }
        intent.putExtra(PEP_KEY_LIST, keyList.toString());
        intent.putExtra(EXPLANATION, explanation);
        intent.putExtra(MANUAL, true);
        return intent;
    }

    public static class DismissKeysyncDialogReceiver extends BroadcastReceiver {

        private Activity activity;

        public DismissKeysyncDialogReceiver(Activity activity) {
            this.activity = activity;
        }
        public DismissKeysyncDialogReceiver() {
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (activity != null) {
                activity.finish();
            }
        }
    }

    public enum Result {
        ACCEPTED, REJECTED;
    }

}
