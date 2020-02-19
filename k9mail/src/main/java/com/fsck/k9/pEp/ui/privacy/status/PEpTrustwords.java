package com.fsck.k9.pEp.ui.privacy.status;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.PEpLanguage;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.PepColoredActivity;
import com.fsck.k9.pEp.ui.keysync.languages.PEpLanguageSelector;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import security.pEp.ui.toolbar.ToolBarCustomizer;
import timber.log.Timber;

public class PEpTrustwords extends PepColoredActivity {

    private static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    public static final String PARTNER_POSITION = "partnerPositionKey";
    public static final String PARTNER_DATA= "partnerKey";
    public static final String PARTNER_ACTION= "partnerAction";
    public static final int DEFAULT_POSITION = -1;
    public static final int REQUEST_HANDSHAKE = 1;
    private static final String MYSELF = "myselfKey";
    private static final String PARTNER_PREFIX = "Partner: ";
    private static final String SHOWING_PGP_FINGERPRINT = "showingPgpKey";
    private static final String ARE_TRUSTWORD_SHORT = "are_trustword_short";
    private static final String TRUSTWORD_LANGUAGE = "trustword_length";
    private static final String PEP_KEY_LIST = "keylist";

    private Identity partner, myself;
    private int partnerPosition;
    Context context;
    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    @Bind(R.id.tvPartner)
    TextView partnerView;
    @Bind(R.id.tvMyself)
    TextView myselfView;
    @Bind(R.id.flipper)
    ViewSwitcher flipper;

    @Bind(R.id.partnerLabel) TextView partnerLabel;
    @Bind(R.id.partnerFpr) TextView partnerFpr;
    @Bind(R.id.myselfLabel) TextView myselfLabel;
    @Bind(R.id.myselfFpr) TextView myselfFpr;
    @Bind(R.id.wrongTrustwords) Button wrongTrustWords;
    @Bind(R.id.confirmTrustWords) Button confirmTrustWords;

    @Bind(R.id.loading) View loading;

    Executor executor;

    boolean showingPgpFingerprint = false;
    private Boolean areTrustwordsShort = true;
    private String trustwordsLanguage;
    private MenuItem menuItemTrustwordsLanguage;
    private MenuItem menuItemtrustwordsLength;
    private String fullTrustwords = "";
    private String shortTrustwords = "";
    private boolean includeIdentityData = false;
    @Inject
    ToolBarCustomizer toolBarCustomizer;


    public static void actionRequestMultipleOwnAccountIdsHandshake(Activity context, String myself, List<String> keys, int partnerPosition, Rating pEpRating) {
        Intent i = new Intent(context, PEpTrustwords.class);
        i.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        i.putExtra(PARTNER_POSITION, partnerPosition);
        i.putExtra(MYSELF, myself);
        StringBuilder keyList = new StringBuilder();
        for (String key : keys) {
            keyList.append(key).append(PEpProvider.PEP_KEY_LIST_SEPARATOR);
        }
        i.putExtra(PEP_KEY_LIST, keyList.toString());
        i.putExtra(CURRENT_RATING, pEpRating.toString());
        context.startActivityForResult(i, REQUEST_HANDSHAKE);
    }

    public static void actionRequestHandshake(Activity context, String myself, int partnerPosition, Rating partnerRating) {
        Intent i = new Intent(context, PEpTrustwords.class);
        i.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        i.putExtra(PARTNER_POSITION, partnerPosition);
        i.putExtra(MYSELF, myself);
        i.putExtra(CURRENT_RATING, partnerRating.toString());
        context.startActivityForResult(i, REQUEST_HANDSHAKE);

    }
    public static void actionRequestHandshake(Activity context, String myself, int partnerPosition) {
        Intent i = new Intent(context, PEpTrustwords.class);
        i.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        i.putExtra(PARTNER_POSITION, partnerPosition);
        i.putExtra(MYSELF, myself);
        context.startActivityForResult(i, REQUEST_HANDSHAKE);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        setContentView(R.layout.pep_trustwords);
        ButterKnife.bind(this);
        flipper.setVisibility(View.INVISIBLE);

        setUpToolbar(true);
        PEpUtils.colorToolbar(getToolbar(), getResources().getColor(R.color.light_primary_color));
        context = getApplicationContext();

        //TODO> View threadfactory to use only one engine
        executor = Executors.newSingleThreadExecutor();
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initPep();

        String language = K9.getK9Language().isEmpty() ? Locale.getDefault().getLanguage() : K9.getK9Language();
        if (isLanguageInPEPLanguages(language)) {
            trustwordsLanguage = language;
        }

        if (getIntent() != null) {
            if (intent.hasExtra(CURRENT_RATING)) {
                loadPepRating();
            }
            if (intent.hasExtra(MYSELF)) {
                myself = PEpUtils.createIdentity(new Address(intent.getStringExtra(MYSELF)), context);
                if (!myself.username.equals(myself.address)) {
                    myselfView.setText(String.format(getString(R.string.pep_complete_myself_format), myself.username, myself.address));
                    myselfLabel.setText(String.format(getString(R.string.pep_complete_myself_format), myself.username, myself.address));
                } else {
                    myselfView.setText(String.format(getString(R.string.pep_myself_format),myself.address));
                    myselfLabel.setText(String.format(getString(R.string.pep_myself_format),myself.address));
                }

            }

            partnerPosition = intent.getIntExtra(PARTNER_POSITION, DEFAULT_POSITION);
            if (partnerPosition != DEFAULT_POSITION) {
                partner = getUiCache().getRecipients().get(partnerPosition);
                partner = getpEp().updateIdentity(partner);
                if (!partner.username.equals(partner.address)) {
                    partnerView.setText(String.format(getString(R.string.pep_complete_partner_format), partner.username, partner.address));
                    partnerLabel.setText(String.format(getString(R.string.pep_complete_partner_format), partner.username, partner.address));
                } else {
                    partnerView.setText(String.format(getString(R.string.pep_partner_format),partner.address));
                    partnerLabel.setText(String.format(getString(R.string.pep_partner_format),partner.address));
                }

                loadPartnerRating();

            } else {
                loadPepRating();
                //in this case we know partner address = myself address
                includeIdentityData = true;
                myself = getpEp().myself(myself);
                partner = PEpUtils.createIdentity(new Address(intent.getStringExtra(MYSELF)), context);
                String keys = intent.getStringExtra(PEP_KEY_LIST);
                partner.fpr = keys.replace(myself.fpr, "").replace(",", "");
            }

        }
    }

    private boolean isLanguageInPEPLanguages(String language) {
        PEpProvider pEpProvider = ((K9) getApplication()).getpEpProvider();
        Map<String, PEpLanguage> languages = pEpProvider.obtainLanguages();
        return languages.keySet().contains(language);
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrustwords(includeIdentityData);
    }

    private void loadPartnerRating() {
        Timber.i("Contador de PEpTrustwords+1");
        EspressoTestingIdlingResource.increment();
        getpEp().getRating(partner, new PEpProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                Timber.i("Contador de PEpTrustwords onLoaded");
                setpEpRating(rating);
                Timber.i("Contador de PEpTrustwords antes de colorActionBar");
                colorActionBar();
                Timber.i("Contador de PEpTrustwords onLoaded -1");
                EspressoTestingIdlingResource.decrement();
            }

            @Override
            public void onError(Throwable throwable) {
                Timber.i("Contador de PEpTrustwords onError");
                setpEpRating(Rating.pEpRatingUndefined);
                Timber.i("Contador de PEpTrustwords onError -1");
                EspressoTestingIdlingResource.decrement();
            }
        });
    }

    private void loadTrustwords(boolean shouldRetrieveIdentityData) {
        //Actually what is heavy is update identity and myself.
        getpEp().obtainTrustwords(myself, partner, trustwordsLanguage,
                shouldRetrieveIdentityData,
                new PEpProvider.ResultCallback<HandshakeData>() {
            @Override
            public void onLoaded(final HandshakeData handshakeData) {
                showTrustwords(handshakeData);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private void showTrustwords(HandshakeData handshakeData) {
        fullTrustwords = handshakeData.getFullTrustwords();
        shortTrustwords = handshakeData.getShortTrustwords();
        if (areTrustwordsShort) {
            tvTrustwords.setText(shortTrustwords);
        } else {
            tvTrustwords.setText(fullTrustwords);
        }

        myself = handshakeData.getMyself();
        partner = handshakeData.getPartner();
        myselfFpr.setText(PEpUtils.formatFpr(myself.fpr));
        partnerFpr.setText(PEpUtils.formatFpr(partner.fpr));
        loading.setVisibility(View.GONE);
        wrongTrustWords.setVisibility(View.VISIBLE);
        confirmTrustWords.setVisibility(View.VISIBLE);

        if ((!PEpUtils.isPEpUser(partner) && showingPgpFingerprint) || partnerPosition == DEFAULT_POSITION) {
            flipper.setAnimateFirstView(false);
            flipper.setDisplayedChild(1);
            showingPgpFingerprint = true;
            showFingerprints(false);
        }
        flipper.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pep_trustwords, menu);

        menuItemTrustwordsLanguage = menu.findItem(R.id.action_language);
        menuItemtrustwordsLength = menu.findItem(R.id.long_trustwords);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        if (showingPgpFingerprint) {
            menu.add(Menu.NONE, R.id.action_pgp_fingerprint, Menu.NONE, R.string.pEp_trustwords);
            menu.removeItem(R.id.action_language);
            menu.removeItem(R.id.long_trustwords);
        } else {
            menu.add(Menu.NONE, R.id.action_pgp_fingerprint, Menu.NONE, R.string.pep_pgp_fingerprint);
            menu.add(Menu.NONE, R.id.action_language, Menu.NONE, R.string.settings_language_label);
            menuItemtrustwordsLength = menu.add(Menu.NONE, R.id.long_trustwords, Menu.NONE, R.string.pep_menu_long_trustwords);
            if(areTrustwordsShort) {
                menuItemtrustwordsLength.setTitle(getString(R.string.pep_menu_long_trustwords));
            } else {
                menuItemtrustwordsLength.setTitle(R.string.pep_menu_short_trustwords);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_pgp_fingerprint:
                if (item.getTitle().equals(getString(R.string.pep_pgp_fingerprint))){
                    showingPgpFingerprint = true;
                    item.setTitle(R.string.pEp_trustwords);
                    menuItemTrustwordsLanguage.setVisible(false);
                    menuItemtrustwordsLength.setVisible(false);
                }
                else{
                    showingPgpFingerprint = false;
                    item.setTitle(getString(R.string.pep_pgp_fingerprint));
                    wrongTrustWords.setText(R.string.pep_wrong_trustwords);
                    menuItemTrustwordsLanguage.setVisible(true);
                    menuItemtrustwordsLength.setVisible(true);
                }
                flipper.showNext();
                showFingerprints(true);
                return true;
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
        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    private void showFingerprints(boolean isRequestedByUser) {
        if (isRequestedByUser) {
            showingPgpFingerprint = !showingPgpFingerprint;
        }
        invalidateOptionsMenu();
    }

    private void changeTrustwordsLength(Boolean areShort) {
        areTrustwordsShort = areShort;
        if (areShort) tvTrustwords.setText(shortTrustwords);
        else tvTrustwords.setText(fullTrustwords);
    }

    private void showLanguageSelectionDialog() {
        PEpProvider pEpProvider = ((K9) getApplication()).getpEpProvider();
        Map<String, PEpLanguage> languages = pEpProvider.obtainLanguages();

        Set<String> locales = languages.keySet();
        CharSequence[] pEpLocales = new CharSequence[locales.size()];
        CharSequence[] pEpLanguages = new CharSequence[locales.size()];

        Integer position = 0;
        for (String locale : locales) {
            pEpLocales[position] = locale;
            pEpLanguages[position] = languages.get(locale).getLanguage();
            position++;
        }

        PEpLanguageSelector.showLanguageSelector(PEpTrustwords.this, pEpLocales, pEpLanguages, trustwordsLanguage, (dialog, languagePositon) -> {
            String language = pEpLocales[languagePositon].toString();
            changeTrustwords(language);
        });
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;
        String trustwords = getpEp().trustwords(myself, partner, language, areTrustwordsShort);
        shortTrustwords = getpEp().trustwords(myself, partner, language, true);
        fullTrustwords = getpEp().trustwords(myself, partner, language, false);
        tvTrustwords.setText(trustwords);
    }

    @OnClick(R.id.confirmTrustWords)
    public void confirmTrustwords() {
        if (partner.user_id == null || partner.user_id.isEmpty()) {
            String tempFpr = partner.fpr;
            partner = getpEp().updateIdentity(partner);
            partner.fpr = tempFpr;
        }
        getpEp().trustPersonaKey(partner);
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PARTNER_POSITION, partnerPosition);
        setResult(Activity.RESULT_OK, returnIntent);
        returnIntent.putExtra(PARTNER_DATA, partner);
        returnIntent.putExtra(PARTNER_ACTION, PEpProvider.TrustAction.TRUST);
        finish();

    }

    @OnClick(R.id.wrongTrustwords)
    public void wrongTrustwords() {
        getpEp().keyMistrusted(partner);
        getpEp().getRating(partner);
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PARTNER_POSITION, partnerPosition);
        returnIntent.putExtra(PARTNER_DATA, partner);
        returnIntent.putExtra(PARTNER_ACTION, PEpProvider.TrustAction.MISTRUST);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        showingPgpFingerprint = savedInstanceState.getBoolean(SHOWING_PGP_FINGERPRINT);
        areTrustwordsShort = savedInstanceState.getBoolean(ARE_TRUSTWORD_SHORT);
        trustwordsLanguage = savedInstanceState.getString(TRUSTWORD_LANGUAGE);
        if (showingPgpFingerprint) {
            flipper.showNext();
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOWING_PGP_FINGERPRINT, showingPgpFingerprint);
        outState.putBoolean(ARE_TRUSTWORD_SHORT, areTrustwordsShort);
        outState.putString(TRUSTWORD_LANGUAGE, trustwordsLanguage);
    }

    @Override
    protected void loadPepRating() {
        super.loadPepRating();
        PEpUtils.colorToolbar(getUiCache(), getSupportActionBar(), pEpRating);
        toolBarCustomizer.setStatusBarPepColor(getUiCache().getColor(pEpRating));
    }
}
