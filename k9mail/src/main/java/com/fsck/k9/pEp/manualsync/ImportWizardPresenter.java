package com.fsck.k9.pEp.manualsync;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.pEp.infrastructure.Presenter;
import com.fsck.k9.pEp.ui.keysync.PEpAddDevice;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

public class ImportWizardPresenter implements Presenter {

    private final ImportKeyController importKeyController;
    MessagingController messagingController;
    private boolean ispEp;
    private ImportWizardFromPGPView view;

    @Inject
    public ImportWizardPresenter(ImportKeyController importKeyController) {
        this.messagingController = MessagingController.getInstance();
        this.importKeyController = importKeyController;
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {
        importKeyController.cancel();
    }

    public void onStartClicked(Account account, Callback callback) {
        //new Thread(() ->
                messagingController.startKeyImport(account, callback, ispEp);
                //).start();
    }

    public void init(ImportWizardFromPGPView view, Account account, boolean isStarter,
                     KeySourceType serializableExtra) {
        this.view = view;
        importKeyController.setAccount(account);
        importKeyController.setStarter(isStarter);
        messagingController.setImportKeyController(importKeyController);

        showInitialScreen(serializableExtra);
    }

    private void showInitialScreen(KeySourceType serializableExtra) {

        if (importKeyController.isStarter()) {
            switch (serializableExtra) {
                case PEP:
                    view.renderpEpInitialScreen();
                    ispEp = true;
                    break;
                case PGP:
                    view.setImportTitle("PGP");
                    view.renderPGPInitialScreen();
                    ispEp = false;
                    break;
            }
        } else {
            if (serializableExtra == KeySourceType.PEP) {
                view.renderpEpSecondlScreen();
                ispEp = true;
            } // No else, PGP key is always started from pEp device

        }
    }

    public void cancel() {
        importKeyController.cancel();
        view.cancel();
    }

    public void next() {
        ImportKeyWizardState currentState = importKeyController.next();
        Timber.e(currentState.name()+ " :: " + importKeyController.getState().name());

        switch (currentState) {
            //It will be reached?: don't think so
            case INIT:
                Timber.e("INIT");
                break;
            case BEACON_SENT:
                Timber.e("Beacon sent");
                if (ispEp) {
                    view.renderWaitingForHandshake();
                } else {
                    view.renderWaitingForPGPHandshake();
                }
                break;
            case BEACON_RECEIVED:
                Timber.e("Beacon received");
                break;
            case HANDSHAKE_REQUESTED:
                Timber.e("Handshake requested");
                break;
            case PRIVATE_KEY_WAITING:
                Timber.e("Private key waiting");
                break;
        }
    }

    public void processHandshakeResult(PEpAddDevice.Result result) {
        Timber.e(importKeyController.getState().name());

        if (result.equals(PEpAddDevice.Result.ACCEPTED)) {
            if (importKeyController.isStarter()) {
                view.notifyAcceptedHandshakeAndWaitingForPrivateKey();
                importKeyController.next();
            } else {
                view.notifyKeySent();
                importKeyController.next();
            }
        }
    }

    public void onPrivateKeyReceived() {
        Timber.e(importKeyController.getState().name());
        view.finishImportSuccefully();
    }

    public void close() {
        importKeyController.finish();
        view.close();
    }


    public interface Callback {
        void onStart();
        void onFinish(boolean successful);
    }

}
