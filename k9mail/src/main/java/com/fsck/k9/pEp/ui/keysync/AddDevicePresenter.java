package com.fsck.k9.pEp.ui.keysync;

import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.Presenter;

import org.pEp.jniadapter.Identity;

import java.util.List;

import javax.inject.Inject;

public class AddDevicePresenter implements Presenter {

    private AddDeviceView view;
    private PEpProvider pEpProvider;
    private Identity partner;

    @Inject
    AddDevicePresenter() {
    }

    public void initialize(AddDeviceView view, PEpProvider pEpProvider, String partnerUserId, String partnerAddress) {
        this.view = view;
        this.pEpProvider = pEpProvider;
        partner = new Identity();
        partner.user_id = partnerUserId;
        partner.address = partnerAddress;
        partner = pEpProvider.updateIdentity(partner);
        loadPartner(view);
    }

    private void loadPartner(AddDeviceView view) {
        if (!partner.username.equals(partner.address)) {
            view.showCompletePartnerFormat(partner);
        } else {
            view.showPartnerFormat(partner);
        }
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
                view.showIdentities(identities);
            }

            @Override
            public void onError(Throwable throwable) {
                view.showError();
            }
        });
    }

    public void identityCheckStatusChanged(Identity identity, Boolean checked) {
        if (checked) {
            pEpProvider.setIdentityFlag(identity, 0, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable throwable) {
                    view.showError();
                }
            });
        } else {
            pEpProvider.unsetIdentityFlag(identity, 0, new PEpProvider.CompletedCallback() {
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
