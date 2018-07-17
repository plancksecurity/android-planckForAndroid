package com.fsck.k9.pEp.manualsync;

import android.app.Application;
import android.content.Context;
import android.test.mock.MockApplication;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

public class ImportKeyController {


    private final K9 context;
    private ImportKeyWizardState state;
    private Account account;
    private boolean starter;
    private ImportWizardListener listener;
    String senderKey = "";


    @Inject
    public ImportKeyController(Application context) {
        state = ImportKeyWizardState.INIT;
        this.context = ((K9) context);
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
        if (!state.equals(ImportKeyWizardState.INIT)) {
            context.enableFastPolling();
        }
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
        context.disableFastPolling();
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
