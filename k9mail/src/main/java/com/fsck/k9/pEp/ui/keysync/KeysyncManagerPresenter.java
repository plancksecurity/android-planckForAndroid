package com.fsck.k9.pEp.ui.keysync;

import com.fsck.k9.Account;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.Presenter;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.IdentityFlags;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class KeysyncManagerPresenter implements Presenter {

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
            pEpProvider.setIdentityFlag(updatedIdentity, IdentityFlags.PEPIdfNotForSync.value, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable throwable) {
                    view.showError();
                }
            });
        } else {
            pEpProvider.unsetIdentityFlag(updatedIdentity, IdentityFlags.PEPIdfNotForSync.value, new PEpProvider.CompletedCallback() {
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
