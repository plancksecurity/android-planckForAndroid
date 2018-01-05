package com.fsck.k9.pEp.ui.handshake;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpLanguage;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.ui.HandshakeData;
import com.fsck.k9.pEp.ui.keysync.languages.PEpLanguageSelector;
import com.fsck.k9.pEp.ui.listeners.HandshakeListener;
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;

import org.pEp.jniadapter.Identity;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

class HandshakeViewHolder extends ChildViewHolder {

    private final PEpProvider pEpProvider;
    private final TextView partnerFpr;
    private final TextView partnerLabel;
    private final TextView myselfLabel;
    private final TextView myselfFpr;
    private final Button wrongTrustWords;
    private final Button confirmTrustWords;
    private final TextView tvTrustwords;
    private final TextView partnerView;
    private final TextView myselfView;
    private final ViewSwitcher flipper;
    private final View loading;
    private Identity myself, partner;
    private boolean showingPgpFingerprint = false;
    private Boolean areTrustwordsShort = true;
    private String trustwordsLanguage;
    private String fullTrustwords = "";
    private String shortTrustwords = "";
    private HandshakeListener handshakeListener;

    HandshakeViewHolder(View itemView) {
        super(itemView);
        pEpProvider = ((K9) itemView.getContext().getApplicationContext()).getpEpProvider();
        partnerFpr = itemView.findViewById(R.id.partnerLabel);
        partnerLabel = itemView.findViewById(R.id.partnerFpr);
        myselfLabel = itemView.findViewById(R.id.myselfLabel);
        myselfFpr = itemView.findViewById(R.id.myselfFpr);
        wrongTrustWords = itemView.findViewById(R.id.wrongTrustwords);
        confirmTrustWords = itemView.findViewById(R.id.confirmTrustWords);
        tvTrustwords = itemView.findViewById(R.id.trustwords);
        partnerView = itemView.findViewById(R.id.tvPartner);
        myselfView = itemView.findViewById(R.id.tvMyself);
        flipper = itemView.findViewById(R.id.flipper);
        loading = itemView.findViewById(R.id.loading);
        setupShowTrustwords(itemView);
        setupShowFpr(itemView);
        setupChangeLanguage(itemView);
        setupShowLongTrustwords(itemView);
        setupWrongTrustwords();
        setupConfirmTrustwords();
    }

    private void setupConfirmTrustwords() {
        confirmTrustWords.setOnClickListener(view -> {
            pEpProvider.trustPersonaKey(partner);
            handshakeListener.onTrustwordsPerformedAction();
        });
    }

    private void setupWrongTrustwords() {
        wrongTrustWords.setOnClickListener(view -> {
            pEpProvider.keyCompromised(partner);
            pEpProvider.getRating(partner);
            handshakeListener.onTrustwordsPerformedAction();
        });
    }

    private void setupShowLongTrustwords(View itemView) {
        ImageView showLongTrustwords = itemView.findViewById(R.id.show_long_trustwords);
        showLongTrustwords.setOnClickListener(view -> {
            areTrustwordsShort = !areTrustwordsShort;
            changeTrustwordsLength(areTrustwordsShort);
        });
    }

    private void setupChangeLanguage(View itemView) {
        ImageView changeLanguageView = itemView.findViewById(R.id.change_language);
        changeLanguageView.setOnClickListener(view -> showLanguageSelectionDialog());
    }

    private void setupShowFpr(View itemView) {
        ImageView showFpr = itemView.findViewById(R.id.show_fpr);
        showFpr.setOnClickListener(view -> showFprClicked());
    }

    private void setupShowTrustwords(View itemView) {
        ImageView showTrustwords = itemView.findViewById(R.id.show_trustwords);
        showTrustwords.setOnClickListener(view -> showFprClicked());
    }

    private void showFprClicked() {
        if (!showingPgpFingerprint){
            wrongTrustWords.setText(R.string.pep_wrong_trustwords);
        }
        flipper.showNext();
        showFingerprints(true);
    }

    void render(Identity myself, PEpIdentity identity, HandshakeListener handshakeListener) {
        this.partner = identity;
        this.myself = myself;
        this.handshakeListener = handshakeListener;
        setupPartnerViews();
        setupMyselfViews(myself);
        loadLanguage();
        loadTrustwords();
    }

    private void setupMyselfViews(Identity myself) {
        if (!myself.username.equals(myself.address)) {
            myselfView.setText(String.format(itemView.getContext().getString(R.string.pep_complete_myself_format), myself.username, myself.address));
            myselfLabel.setText(String.format(itemView.getContext().getString(R.string.pep_complete_myself_format), myself.username, myself.address));
        } else {
            myselfView.setText(String.format(itemView.getContext().getString(R.string.pep_myself_format),myself.address));
            myselfLabel.setText(String.format(itemView.getContext().getString(R.string.pep_myself_format),myself.address));
        }
    }

    private void setupPartnerViews() {
        if (!partner.username.equals(partner.address)) {
            partnerView.setText(String.format(itemView.getContext().getString(R.string.pep_complete_partner_format), partner.username, partner.address));
            partnerLabel.setText(String.format(itemView.getContext().getString(R.string.pep_complete_partner_format), partner.username, partner.address));
        } else {
            partnerView.setText(String.format(itemView.getContext().getString(R.string.pep_partner_format),partner.address));
            partnerLabel.setText(String.format(itemView.getContext().getString(R.string.pep_partner_format),partner.address));
        }
    }

    private void loadLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (isLanguageInPEPLanguages(language)) {
            trustwordsLanguage = language;
        }
    }

    private boolean isLanguageInPEPLanguages(String language) {
        Map<String, PEpLanguage> languages = pEpProvider.obtainLanguages();
        return languages.keySet().contains(language);
    }

    private void loadTrustwords() {
        pEpProvider.obtainTrustwords(myself, partner, trustwordsLanguage,
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

        if (!PEpUtils.isPEpUser(partner) && showingPgpFingerprint) {
            flipper.setAnimateFirstView(false);
            if (flipper.getDisplayedChild() != 1) {
                flipper.setDisplayedChild(1);
            }
            showingPgpFingerprint = true;
            showFingerprints(false);
        }
        flipper.setVisibility(View.VISIBLE);
    }

    private void changeTrustwordsLength(Boolean areShort) {
        areTrustwordsShort = areShort;
        if (areShort) tvTrustwords.setText(shortTrustwords);
        else tvTrustwords.setText(fullTrustwords);
    }

    private void showLanguageSelectionDialog() {
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

        PEpLanguageSelector.showLanguageSelector(itemView.getContext(), pEpLocales, pEpLanguages, trustwordsLanguage, (dialog, languagePositon) -> {
            String language = pEpLocales[languagePositon].toString();
            changeTrustwords(language);
        });
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;
        loadTrustwords();
    }

    private void showFingerprints(boolean isRequestedByUser) {
        if (isRequestedByUser) {
            showingPgpFingerprint = !showingPgpFingerprint;
        }
    }
}
