package com.fsck.k9.pEp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PEpTrustwords extends PepColoredActivity {

    private static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    public static final String PARTNER_POSITION = "partnerPositionKey";
    public static final int DEFAULT_POSITION = -1;
    public static final int HANDSHAKE_REQUEST = 1;
    private static final String MYSELF = "myselfKey";
    private static final String PARTNER_PREFIX = "Partner: ";
    private static final String SHOWING_PGP_FINGERPRINT = "showingPgpKey";

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
        context = getApplicationContext();
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        //TODO> View threadfactory to use only one engine
        executor = Executors.newSingleThreadExecutor();
        initPep();

        if (getIntent() != null) {

            if (intent.hasExtra(PARTNER_POSITION)) {
                partnerPosition = intent.getIntExtra(PARTNER_POSITION, DEFAULT_POSITION);
                partner = getUiCache().getRecipients().get(partnerPosition);
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
            //
            loadTrustwords();

        }


    }

    private void loadPartnerRating() {
        getpEp().identityRating(partner, new PEpProvider.Callback<Rating>() {
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
        //Actually what is heavy is update identity and sender.
        getpEp().trustwords(myself, partner, trustwordsLanguage, new PEpProvider.Callback<HandshakeData>() {
            @Override
            public void onLoaded(final HandshakeData handshakeData) {
                fullTrustwords = handshakeData.fullTrustwords;
                shortTrustwords = handshakeData.shortTrustwords;
                if (areTrustwordsShort) {
                    tvTrustwords.setText(shortTrustwords);
                } else {
                    tvTrustwords.setText(fullTrustwords);
                }

                myself = handshakeData.myself;
                partner = handshakeData.partner;
                myselfFpr.setText(PEpUtils.formatFpr(myself.fpr));
                partnerFpr.setText(PEpUtils.formatFpr(partner.fpr));
                loading.setVisibility(View.GONE);


            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pep_trustwords, menu);

        menuItemTrustwordsLanguage = menu.findItem(R.id.action_language);
        menuItemtrustwordsLength = menu.findItem(R.id.long_trustwords);
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
                    wrongTrustWords.setText(R.string.pep_wrong_fingerprint);
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
                showingPgpFingerprint = !showingPgpFingerprint;
//                invalidateOptionsMenu();
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

    private void changeTrustwordsLength(Boolean areShort) {
        areTrustwordsShort = areShort;
        if (areShort) tvTrustwords.setText(shortTrustwords);
        else tvTrustwords.setText(fullTrustwords);
    }

    private void showLanguageSelectionDialog() {
        final CharSequence[] pEpLanguages = PEpUtils.getPEpLanguages();
        CharSequence[] displayLanguages = prettifyLanguages(pEpLanguages);
        new AlertDialog.Builder(PEpTrustwords.this).setTitle(getResources().getString(R.string.settings_language_label))
                .setItems(displayLanguages, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String language = pEpLanguages[i].toString();
                        changeTrustwords(language);
                    }
        }).create().show();
    }

    @NonNull
    private CharSequence[] prettifyLanguages(CharSequence[] pEpLocales) {
        CharSequence[] pEpLanguages = new CharSequence[pEpLocales.length];
        for (Locale locale : Locale.getAvailableLocales()) {
            for (int i = 0; i < pEpLocales.length; i++) {
                if (locale.getLanguage().equals(pEpLocales[i])) {
                    String uppercasedLanguage = uppercaseFirstCharacter(locale);
                    pEpLanguages[i] = uppercasedLanguage;
                }
            }
        }
        return pEpLanguages;
    }

    @NonNull
    private String uppercaseFirstCharacter(Locale locale) {
        String displayLanguage = locale.getDisplayLanguage();
        return displayLanguage.substring(0, 1).toUpperCase() + displayLanguage.substring(1);
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;

        String partnerFullTrustwords = PEpUtils.getTrustWords(getpEp(), partner, language);
        String myFullTrustwords = PEpUtils.getTrustWords(getpEp(), myself, language);
        String partnerShortTrustwords = PEpUtils.getShortTrustWords(getpEp(), partner, language);
        String myShortTrustWords = PEpUtils.getShortTrustWords(getpEp(), myself, language);

        if (myself.fpr.compareTo(partner.fpr) > 0) {
            fullTrustwords = partnerFullTrustwords + myFullTrustwords;
            shortTrustwords = partnerShortTrustwords + myShortTrustWords;
        } else {
            fullTrustwords = myFullTrustwords + partnerFullTrustwords;
            shortTrustwords = myShortTrustWords + partnerShortTrustwords;
        }

        if (areTrustwordsShort) {
            tvTrustwords.setText(shortTrustwords);
        } else {
            tvTrustwords.setText(fullTrustwords);
        }
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
        getpEp().identityRating(partner);
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PARTNER_POSITION, partnerPosition);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        showingPgpFingerprint = savedInstanceState.getBoolean(SHOWING_PGP_FINGERPRINT);
        if (showingPgpFingerprint) flipper.showNext();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOWING_PGP_FINGERPRINT, showingPgpFingerprint);
    }
}
