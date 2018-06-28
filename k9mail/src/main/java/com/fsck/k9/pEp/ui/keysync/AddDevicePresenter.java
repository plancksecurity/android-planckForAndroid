package com.fsck.k9.pEp.ui.keysync;

import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.Presenter;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.IdentityFlags;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AddDevicePresenter implements Presenter {

    private AddDeviceView view;
    private PEpProvider pEpProvider;
    private Identity partner;
    private List<Account> accounts;
    private KeyImportStrategy keyImportStrategy;

    @Inject
    AddDevicePresenter() {
    }

    public void initialize(AddDeviceView view, PEpProvider pEpProvider, Identity partnerId,
                           List<Account> accounts, boolean isManualSync) {
        this.view = view;
        this.pEpProvider = pEpProvider;
        this.accounts = accounts;
        partner = partnerId;

        if (isManualSync) {
            keyImportStrategy = new ManualKeyImportStrategy();
        } else {
            keyImportStrategy = new KeySyncStrategy();
        }
    }

    void acceptHandshake() {
        keyImportStrategy.acceptHandshake(partner);
        view.close(true);
    }

    void rejectHandshake() {
        keyImportStrategy.rejectHandshake(partner);
        view.close(false);
    }

    void cancelHandshake() {
        keyImportStrategy.cancelHandshake(partner);
        view.goBack();
    }

    public void advancedOptionsClicked() {
        pEpProvider.loadOwnIdentities(new PEpProvider.ResultCallback<List<Identity>>() {
            @Override
            public void onLoaded(List<Identity> identities) {
                ArrayList<Identity> identitiesToShow = new ArrayList<>();
                for (Identity identity : identities) {
                    for (Account account : accounts) {
                        if (account.getEmail().equals(identity.address)) {
                            identitiesToShow.add(identity);
                        }
                    }
                }
                identities.removeAll(identitiesToShow);
                identitiesCheckStatusChanged(identities, true);
                view.showIdentities(identitiesToShow);
            }

            @Override
            public void onError(Throwable throwable) {
                view.showError();
            }
        });
    }

    private void identitiesCheckStatusChanged(List<Identity> identities, Boolean checked) {
        if (!identities.isEmpty()) {
            for (Identity identity : identities) {
                identityCheckStatusChanged(identity, checked);
            }
        }
    }

    public void identityCheckStatusChanged(Identity identity, Boolean checked) {
        if (!checked) {
            pEpProvider.setIdentityFlag(identity, IdentityFlags.PEPIdfNotForSync.value, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable throwable) {
                    view.showError();
                }
            });
        } else {
            pEpProvider.unsetIdentityFlag(identity, IdentityFlags.PEPIdfNotForSync.value, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable throwable) {
                    view.showError();
                }
            });
        }
    }

    public void basicOptionsClicked() {
        view.hideIdentities();
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
    private abstract class KeyImportStrategy {
        abstract void acceptHandshake(Identity partner);
        abstract void rejectHandshake(Identity partner);
        abstract void cancelHandshake(Identity partner);
    };

    private class ManualKeyImportStrategy extends KeyImportStrategy{
        @Override
        void acceptHandshake(Identity partner) {
            pEpProvider.trustPersonaKey(partner);
            new Thread(() -> {
                try {
                    // TODO: 10/05/18 get the current account
                    MessagingController.getInstance().sendOwnKey(accounts.get(0), partner.fpr);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }).start();

        }

        @Override
        void rejectHandshake(Identity partner) {
            pEpProvider.keyCompromised(partner);
        }

        @Override
        void cancelHandshake(Identity partner) {

        }
    }

    private class KeySyncStrategy extends KeyImportStrategy {

        @Override
        void acceptHandshake(Identity partner) {
            pEpProvider.acceptHandshake(partner);
        }

        @Override
        void rejectHandshake(Identity partner) {
            pEpProvider.rejectHandshake(partner);
        }

        @Override
        void cancelHandshake(Identity partner) {
            pEpProvider.cancelHandshake(partner);
        }
    }
}
