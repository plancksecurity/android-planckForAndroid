package com.fsck.k9.pEp.manualsync;


import com.fsck.k9.K9;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.infrastructure.Presenter;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.SyncHandshakeSignal;
import security.pEp.sync.SyncState;
import timber.log.Timber;

public class ImportWizardPresenter implements Presenter {

    private boolean formingGroup;
    private ImportWizardFromPGPView view;
    private Identity myself;
    private Identity partner;
    private SyncHandshakeSignal signal;
    private PEpProvider pEp;
    private SyncState state;
    private String trustwordsLanguage = K9.getK9CurrentLanguage();
    private String trustWords = "";
    private boolean showingShort = true;

    @Inject
    public ImportWizardPresenter(@Named("Background") PEpProvider pEp) {
        this.pEp = pEp;
    }


    private void showInitialScreen(boolean formingGroup) {
        if (formingGroup) {
            view.renderpEpCreateDeviceGroupRequest();
        } else {
            view.renderpEpAddToExistingDeviceGroupRequest();
        }
    }

    public void cancel() {
        pEp.cancelSync();
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
        trustWords = pEp.trustwords(myself, partner, trustwordsLanguage, true);


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
        trustWords = pEp.trustwords(myself, partner, trustwordsLanguage, showingShort);
        view.showHandshake(trustWords);
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
        final List pEpLanguages = PEpUtils.getPEpLocales();
        String language = pEpLanguages.get(languagePosition).toString();
        changeTrustwords(language);
        return true;
    }

    private void changeTrustwords(String language) {
        trustwordsLanguage = language;
        trustWords = pEp.trustwords(myself, partner, trustwordsLanguage, showingShort);
        view.showHandshake(trustWords);
    }

    public void acceptHandshake() {
        pEp.acceptSync();
        next();
    }

    public void leaveDeviceGroup() {
        view.disableSync();
    }


    @Override
    public void resume() {

    }

    @Override
    public void pause() {


    }

    @Override
    public void destroy() {
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
        pEp.rejectSync();
        view.disableSync();
    }
}
