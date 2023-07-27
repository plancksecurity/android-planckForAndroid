package com.fsck.k9.planck.ui.keysync;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.planck.PlanckProvider;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.IdentityFlags;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class KeysyncManagerPresenter {

    private KeysyncManagementView view;
    private final PlanckProvider planckProvider;
    private final List<Account> accounts;

    @Inject
    KeysyncManagerPresenter(PlanckProvider planckProvider, Preferences preferences) {
        this.planckProvider = planckProvider;
        this.accounts = preferences.getAccounts();
    }

    public void initialize(KeysyncManagementView view) {
        this.view = view;
        loadIdentities(view, accounts);
    }

    private void loadIdentities(final KeysyncManagementView view, final List<Account> accounts) {
        planckProvider.loadOwnIdentities(new PlanckProvider.ResultCallback<List<Identity>>() {
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
        Identity updatedIdentity = planckProvider.updateIdentity(identity);

        if (!checked) {
            planckProvider.setIdentityFlag(updatedIdentity, IdentityFlags.pEpIdfNotForSync.value, new PlanckProvider.CompletedCallback() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable throwable) {
                    view.showError();
                }
            });
        } else {
            planckProvider.unsetIdentityFlag(updatedIdentity, IdentityFlags.pEpIdfNotForSync.value, new PlanckProvider.CompletedCallback() {
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
