package com.fsck.k9.planck.manualsync;

import com.fsck.k9.K9;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUtils;

import java.util.List;
import javax.inject.Inject;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.SyncHandshakeSignal;
import security.planck.sync.SyncState;
import timber.log.Timber;

public class ImportWizardPresenter {

    private static final String DEFAULT_TRUSTWORDS_LANGUAGE = "en";
    private boolean formingGroup;
    private ImportWizardFromPGPView view;
    private Identity myself;
    private Identity partner;
    private SyncHandshakeSignal signal;
    private PlanckProvider planck;
    private SyncState state;
    private String trustwordsLanguage = K9.getK9CurrentLanguage();
    private String trustWords = "";
    private boolean showingShort = true;

    @Inject
    public ImportWizardPresenter(PlanckProvider planck) {
        this.planck = planck;
    }


    private void showInitialScreen(boolean formingGroup) {
        if (formingGroup) {
            view.renderpEpCreateDeviceGroupRequest();
        } else {
            view.renderpEpAddToExistingDeviceGroupRequest();
        }
    }

    public void cancel() {
        planck.cancelSync();
        state.finish();
        view.cancel();
        trustWords = "";
    }

    public void next() {
        Timber.e(state.name());
        state = state.next();
        processState();
    }

    private void processState() {
        switch (state) {
            case INITIAL:
                //NOP
                break;
            case HANDSHAKING:
                view.showHandshake(trustWords);
                break;
            case WAITING:
                if (formingGroup) {
                    view.prepareGroupCreationLoading();
                } else {
                    view.prepareGroupJoiningLoading();
                }
                view.showWaitingForSync();
                break;
            case DONE:
                if (formingGroup) {
                    view.showGroupCreated();
                } else {
                    view.showJoinedGroup();
                }
                break;
        }
    }

    public SyncState getState() {
        return state;
    }

    public void setState(SyncState state) {
        this.state = state;
    }

    public void restoreState(SyncState state, String trustWords) {
        this.state = state;
        if (!trustWords.isEmpty()) {
            this.trustWords = trustWords;
        }
        processState();
    }


    public void init(ImportWizardFrompEp view,
                     Identity myself,
                     Identity partner,
                     SyncHandshakeSignal signal,
                     boolean isFormingGroup) {

        this.view = view;
        this.myself = myself;
        this.partner = partner;
        this.signal = signal;
        this.formingGroup = isFormingGroup;
        this.state = SyncState.INITIAL;

        showInitialScreen(isFormingGroup);

        fixUnsupportedLanguage();
        planck.trustwords(myself, partner, trustwordsLanguage, true, new PlanckProvider.SimpleResultCallback<String>() {
            @Override
            public void onLoaded(String newTrustwords) {
                trustWords = newTrustwords;
            }
        });

        this.view.setFingerPrintTexts(PlanckUtils.formatFpr(this.myself.fpr), PlanckUtils.formatFpr(this.partner.fpr));
    }

    private void fixUnsupportedLanguage() {
        if (!PlanckUtils.trustWordsAvailableForLang(trustwordsLanguage)) {
            trustwordsLanguage = DEFAULT_TRUSTWORDS_LANGUAGE;
        }
    }


    public boolean isHandshaking() {
        return state == SyncState.HANDSHAKING;
    }

    void switchTrustwordsLength() {
        showDebugInfo();
        showingShort = !showingShort;

        if (showingShort) {
            view.showLongTrustwordsIndicator();
        } else {
            view.hideLongTrustwordsIndicator();
        }
        refreshTrustWords();
    }

    private void showDebugInfo() {
        Timber.e("------------------------");
        Timber.e(trustWords);
        Timber.e(myself.fpr);
        Timber.e(partner.fpr);
        Timber.e("------------------------");
    }

    boolean changeTrustwordsLanguage(int languagePosition) {
        showDebugInfo();
        final List<String> pEpLanguages = PlanckUtils.getPlanckLocales();
        String language = pEpLanguages.get(languagePosition);
        changeTrustwords(language);
        return true;
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;
        refreshTrustWords();
    }

    private void refreshTrustWords() {
        planck.trustwords(myself, partner, trustwordsLanguage, showingShort, new PlanckProvider.SimpleResultCallback<String>() {
            @Override
            public void onLoaded(String newTrustwords) {
                trustWords = newTrustwords;
                view.showHandshake(trustWords);
            }
        });
    }

    public void acceptHandshake() {
        planck.acceptSync();
        next();
    }

    public void leaveDeviceGroup() {
        view.leaveDeviceGroup();
    }

    public void processSignal(SyncHandshakeSignal signal) {
        switch (signal) {
            case SyncNotifySole:
            case SyncNotifyInGroup:
                if (state == SyncState.WAITING) {
                    if (formingGroup) {
                        view.showGroupCreated();
                    } else {
                        view.showJoinedGroup();
                    }
                } else {
                    view.cancel();
                }
                break;
            case SyncNotifyTimeout:
                state = SyncState.ERROR;
                view.showSomethingWentWrong();
        }
    }

    public void rejectHandshake() {
        planck.rejectSync();
        view.finish();
    }
}
