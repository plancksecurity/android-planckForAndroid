package com.fsck.k9.pEp.ui.keysync;

import com.fsck.k9.Account;
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

    @Inject
    AddDevicePresenter() {
    }

    public void initialize(AddDeviceView view, PEpProvider pEpProvider, Identity partnerId, List<Account> accounts) {
        this.view = view;
        this.pEpProvider = pEpProvider;
        this.accounts = accounts;
        partner = partnerId;
        loadPartner(view);
    }

    private void loadPartner(AddDeviceView view) {
    }

    void acceptHandshake() {
        pEpProvider.acceptHandshake(partner);
        view.close();
    }

    void rejectHandshake() {
        pEpProvider.rejectHandshake(partner);
        view.close();
    }

    void cancelHandshake() {
        pEpProvider.cancelHandshake(partner);
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
}
