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

import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.PepColoredActivity;
import com.fsck.k9.pEp.ui.keysync.languages.PEpLanguageSelector;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PEpTrustwords extends PepColoredActivity {

    private static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    public static final String PARTNER_POSITION = "partnerPositionKey";
    public static final int DEFAULT_POSITION = -1;
    public static final int HANDSHAKE_REQUEST = 1;
    private static final String MYSELF = "myselfKey";
    private static final String PARTNER_PREFIX = "Partner: ";
    private static final String SHOWING_PGP_FINGERPRINT = "showingPgpKey";
    private static final String ARE_TRUSTWORD_SHORT = "are_trustword_short";
    private static final String TRUSTWORD_LANGUAGE = "trustword_length";

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

    public static void actionRequestHandshake(Activity context, String myself, int partnerPosition) {
        Intent i = new Intent(context, PEpTrustwords.class);
        i.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        i.putExtra(PARTNER_POSITION, partnerPosition);
        i.putExtra(MYSELF, myself);
        context.startActivityForResult(i, HANDSHAKE_REQUEST);

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

        if (getIntent() != null) {

            if (intent.hasExtra(PARTNER_POSITION)) {
                partnerPosition = intent.getIntExtra(PARTNER_POSITION, DEFAULT_POSITION);
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

        }
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrustwords();
    }

    private void loadPartnerRating() {
        getpEp().getRating(partner, new PEpProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                setpEpRating(rating);
                colorActionBar();
            }

            @Override
            public void onError(Throwable throwable) {
                setpEpRating(Rating.pEpRatingUndefined);
            }
        });
    }

    private void loadTrustwords() {
        //Actually what is heavy is update identity and myself.
        getpEp().obtainTrustwords(myself, partner, trustwordsLanguage,
                false,
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

        if (!PEpUtils.isPEpUser(partner)) {
            flipper.setAnimateFirstView(false);
            flipper.setDisplayedChild(1);
            showFingerprints();
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
        MenuItem fingerprintItem = menu.findItem(R.id.action_pgp_fingerprint);

        if (showingPgpFingerprint) {
            fingerprintItem.setTitle(R.string.pEp_trustwords);
            menuItemTrustwordsLanguage.setVisible(false);
            menuItemtrustwordsLength.setVisible(false);
        }

        if(areTrustwordsShort) {
            menuItemtrustwordsLength.setTitle(getString(R.string.pep_menu_long_trustwords));
        } else {
            menuItemtrustwordsLength.setTitle(R.string.pep_menu_short_trustwords);
        }

        return true;
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
                    item.setTitle(R.string.pEp_trustwords);
                    menuItemTrustwordsLanguage.setVisible(false);
                    menuItemtrustwordsLength.setVisible(false);
                }
                else{
                    item.setTitle(getString(R.string.pep_pgp_fingerprint));
                    wrongTrustWords.setText(R.string.pep_wrong_trustwords);
                    menuItemTrustwordsLanguage.setVisible(true);
                    menuItemtrustwordsLength.setVisible(true);
                }
                flipper.showNext();
                showFingerprints();
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

    private void showFingerprints() {
        showingPgpFingerprint = !showingPgpFingerprint;
        invalidateOptionsMenu();
    }

    private void changeTrustwordsLength(Boolean areShort) {
        areTrustwordsShort = areShort;
        if (areShort) tvTrustwords.setText(shortTrustwords);
        else tvTrustwords.setText(fullTrustwords);
    }

    private void showLanguageSelectionDialog() {
        final CharSequence[] pEpLanguages = PEpUtils.getPEpLanguages();
        PEpLanguageSelector.showLanguageSelector(PEpTrustwords.this, pEpLanguages, trustwordsLanguage, (dialog, languagePositon) -> {
            String language = pEpLanguages[languagePositon].toString();
            changeTrustwords(language);
        });
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;
        String trustwords = getpEp().trustwords(myself, partner, language, areTrustwordsShort);
        tvTrustwords.setText(trustwords);
    }

    @OnClick(R.id.confirmTrustWords)
    public void confirmTrustwords() {
        getpEp().trustPersonaKey(partner);
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PARTNER_POSITION, partnerPosition);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();

    }

    @OnClick(R.id.wrongTrustwords)
    public void wrongTrustwords() {
        getpEp().keyCompromised(partner);
        getpEp().getRating(partner);
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PARTNER_POSITION, partnerPosition);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        showingPgpFingerprint = savedInstanceState.getBoolean(SHOWING_PGP_FINGERPRINT);
        areTrustwordsShort = savedInstanceState.getBoolean(ARE_TRUSTWORD_SHORT);
        trustwordsLanguage = savedInstanceState.getString(TRUSTWORD_LANGUAGE);
        if (showingPgpFingerprint) flipper.showNext();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOWING_PGP_FINGERPRINT, showingPgpFingerprint);
        outState.putBoolean(ARE_TRUSTWORD_SHORT, areTrustwordsShort);
        outState.putString(TRUSTWORD_LANGUAGE, trustwordsLanguage);
    }
}
