package com.fsck.k9.pEp.manualsync;

import com.fsck.k9.Account;

import javax.inject.Inject;
import javax.inject.Singleton;

public class ImportKeyController {


    private ImportKeyWizardState state;
    private Account account;
    private boolean starter;
    private ImportWizardListener listener;
    String senderKey = "";


    @Inject
    public ImportKeyController() {
        state = ImportKeyWizardState.INIT;
    }

    public void cancel() {
        finish();
        listener = null;
    }

    public void start() {
        state = state.start();

    }

    public ImportKeyWizardState next() {
        state = state.next();
        return state;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public boolean isStarter() {
        return starter;
    }

    public void setStarter(boolean starter) {
        this.starter = starter;
    }


    public void setListener(ImportWizardListener listener) {
        this.listener = listener;
    }

    public ImportKeyWizardState getState() {
        return state;
    }

    public void finish() {
        state = ImportKeyWizardState.INIT;
        senderKey = "";
    }

    public void waitForKey() {
        state = ImportKeyWizardState.PRIVATE_KEY_WAITING;
    }

    public String getSenderKey() {
        return senderKey;
    }

    public void setSenderKey(String senderKey) {
        this.senderKey = senderKey;
    }

    public interface ImportWizardListener {

    }


}
