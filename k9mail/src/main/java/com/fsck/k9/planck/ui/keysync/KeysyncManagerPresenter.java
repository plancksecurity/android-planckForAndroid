package com.fsck.k9.planck.ui.keysync;

import com.fsck.k9.Account;
import com.fsck.k9.planck.PEpProvider;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.IdentityFlags;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class KeysyncManagerPresenter {

    private KeysyncManagementView view;
    private PEpProvider pEpProvider;
    private List<Account> accounts;

    @Inject
    KeysyncManagerPresenter() {
    }

    public void initialize(KeysyncManagementView view, PEpProvider pEpProvider, List<Account> accounts) {
        this.view = view;
        this.pEpProvider = pEpProvider;
        this.accounts = accounts;
        loadIdentities(view, accounts);
    }

    private void loadIdentities(final KeysyncManagementView view, final List<Account> accounts) {
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
        Identity updatedIdentity = pEpProvider.updateIdentity(identity);

        if (!checked) {
            pEpProvider.setIdentityFlag(updatedIdentity, IdentityFlags.pEpIdfNotForSync.value, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable throwable) {
                    view.showError();
                }
            });
        } else {
            pEpProvider.unsetIdentityFlag(updatedIdentity, IdentityFlags.pEpIdfNotForSync.value, new PEpProvider.CompletedCallback() {
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
}
