package com.fsck.k9.pEp.ui.keysync;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.pEp.PEpProvider;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.IdentityFlags;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AddDevicePresenter {

    private AddDeviceView view;
    private PEpProvider pEpProvider;
    private Identity partner;
    private Identity myself;
    private List<Account> accounts;
    private KeyImportStrategy keyImportStrategy;
    private boolean isPGP = false;
    private String keylist;

    @Inject
    AddDevicePresenter() {
    }

    public void initialize(AddDeviceView view, PEpProvider pEpProvider, Identity myId,
                           Identity partnerId,
                           List<Account> accounts, boolean isManualSync, String keylist) {
        this.view = view;
        this.pEpProvider = pEpProvider;
        this.accounts = accounts;
        partner = partnerId;
        myself = myId;
        isPGP = keylist != null;
        this.keylist = keylist;
        if (isManualSync) {
            if (isPGP) {
                keyImportStrategy = new ManualPGPKeyImportStrategy();
                view.showFPR();
            }
            else {
                keyImportStrategy = new ManualpEpKeyImportStrategy();
            }
        } else {
            keyImportStrategy = new KeySyncStrategy();
            view.showKeySyncTitle();
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
            pEpProvider.setIdentityFlag(identity, IdentityFlags.pEpIdfNotForSync.value, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable throwable) {
                    view.showError();
                }
            });
        } else {
            pEpProvider.unsetIdentityFlag(identity, IdentityFlags.pEpIdfNotForSync.value, new PEpProvider.CompletedCallback() {
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

    public boolean isPGP() {
        return isPGP;
    }

    public boolean ispEp() {
        return !isPGP;
    }

    public boolean myselfHasUserId() {
        return !myself.username.equals(myself.address);
    }

    public String getMyselfAddress() {
        return myself.address;
    }

    public String getMyselfUsername() {
        return myself.username;
    }

    public String getMyFpr() {
        return myself.fpr;
    }

    public String getPartnerFpr() {
        partner.fpr = keylist.replace(myself.fpr, "").replace(",", "");
        return partner.fpr;
    }

    public void setKeylist(String keylist) {
        this.keylist = keylist;
    }

    private abstract class KeyImportStrategy {
        abstract void acceptHandshake(Identity partner);
        abstract void rejectHandshake(Identity partner);
        abstract void cancelHandshake(Identity partner);
    };

    private class ManualpEpKeyImportStrategy extends KeyImportStrategy{
        @Override
        void acceptHandshake(Identity partner) {
            pEpProvider.trustOwnKey(partner);
        }

        @Override
        void rejectHandshake(Identity partner) {
            pEpProvider.keyMistrusted(partner);
        }

        @Override
        void cancelHandshake(Identity partner) {

        }
    }

    private class ManualPGPKeyImportStrategy extends KeyImportStrategy{
        @Override
        void acceptHandshake(Identity partner) {
            pEpProvider.trustOwnKey(partner);
        }

        @Override
        void rejectHandshake(Identity partner) {
            pEpProvider.keyMistrusted(partner);
        }

        @Override
        void cancelHandshake(Identity partner) {

        }
    }
    private class KeySyncStrategy extends KeyImportStrategy {

        @Override
        void acceptHandshake(Identity partner) {
            Log.e("pEpEngine", String.format("acceptSync: myself(%s), partner(%s)", pEpProvider.myself(partner), partner) );
            pEpProvider.acceptSync();
        }

        @Override
        void rejectHandshake(Identity partner) {
            pEpProvider.rejectSync();
        }

        @Override
        void cancelHandshake(Identity partner) {
            pEpProvider.cancelSync();
        }
    }
}
