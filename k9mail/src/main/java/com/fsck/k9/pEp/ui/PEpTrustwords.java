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
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProviderFactory;
import com.fsck.k9.pEp.PEpUtils;
import org.pEp.jniadapter.Identity;

import java.util.Locale;

public class PEpTrustwords extends PepColoredActivity {

    private static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    private static final String TRUSTWORDS = "trustwordsKey";
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
    @Bind(R.id.wrongTrustwords)
    Button wrongTrustWords;

    boolean showingPgpFingerprint = false;
    private Boolean areTrustwordsShort = true;
    private String trustwordsLanguage;
    private MenuItem menuItemTrustwordsLanguage;
    private MenuItem menuItemtrustwordsLength;

    public static void actionRequestHandshake(Activity context, String trust, String myself, int partnerPosition) {
        Intent i = new Intent(context, PEpTrustwords.class);
        i.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        i.putExtra(TRUSTWORDS, trust);
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
        initPep();

        if (getIntent() != null) {
            if (intent.hasExtra(TRUSTWORDS)) {
                tvTrustwords.setText(getIntent().getStringExtra(TRUSTWORDS));
            }

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

                partnerFpr.setText(PEpUtils.formatFpr(partner.fpr));
                setpEpRating(getpEp().identityRating(partner));
                colorActionBar();

            }

            if (intent.hasExtra(MYSELF)) {
                myself = PEpUtils.createIdentity(new Address(intent.getStringExtra(MYSELF)), context);
                myself = getpEp().myself(myself);
                if (!myself.username.equals(myself.address)) {
                    myselfView.setText(String.format(getString(R.string.pep_complete_myself_format), myself.username, myself.address));
                    myselfLabel.setText(String.format(getString(R.string.pep_complete_myself_format), myself.username, myself.address));
                } else {
                    myselfView.setText(String.format(getString(R.string.pep_myself_format),myself.address));
                    myselfLabel.setText(String.format(getString(R.string.pep_myself_format),myself.address));
                }
                myselfFpr.setText(PEpUtils.formatFpr(myself.fpr));

            }

        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pep_trustwords, menu);
//        if (showingPgpFingerprint) {
//            menu.findItem(R.id.long_trustwords).setVisible(false);
//            menu.findItem(R.id.action_pgp_fingerprint).setTitle(R.string.pEp_trustwords);
//        } else {
//            menu.findItem(R.id.long_trustwords).setVisible(true);
//            menu.findItem(R.id.action_pgp_fingerprint).setTitle(R.string.pep_pgp_fingerprint);
//
//        }
//
//        if (areTrustwordsShort) {
//            menu.findItem(R.id.long_trustwords).setTitle(R.string.pep_menu_long_trustwords);
//        } else {
//            menu.findItem(R.id.long_trustwords).setTitle(R.string.pep_menu_short_trustwords);
//        }


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
        String trust;
        PEpProvider pEpProvider = PEpProviderFactory.createProvider(context);
        String myTrust;
        String theirTrust;
        if (!areShort) {
            myTrust = PEpUtils.getTrustWords(pEpProvider, myself, trustwordsLanguage);
            theirTrust = PEpUtils.getTrustWords(pEpProvider, partner, trustwordsLanguage);
        } else {
            myTrust = PEpUtils.getShortTrustWords(pEpProvider, myself, trustwordsLanguage);
            theirTrust = PEpUtils.getShortTrustWords(pEpProvider, partner, trustwordsLanguage);
        }
        if (myself.fpr.compareTo(partner.fpr) > 0) {
            trust = theirTrust + myTrust;
        } else {
            trust = myTrust + theirTrust;
        }
        tvTrustwords.setText(trust);
        pEpProvider.close();
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
        String trust;
        PEpProvider pEpProvider = PEpProviderFactory.createProvider(context);
        String myTrust;
        String theirTrust;
        if (areTrustwordsShort) {
            myTrust = PEpUtils.getShortTrustWords(pEpProvider, myself, language);
            theirTrust = PEpUtils.getShortTrustWords(pEpProvider, partner, language);
        } else {
            myTrust = PEpUtils.getTrustWords(pEpProvider, myself, language);
            theirTrust = PEpUtils.getTrustWords(pEpProvider, partner, language);
        }

        if (myself.fpr.compareTo(partner.fpr) > 0) {
            trust = theirTrust + myTrust;
        } else {
            trust = myTrust + theirTrust;
        }
        tvTrustwords.setText(trust);
        pEpProvider.close();
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
