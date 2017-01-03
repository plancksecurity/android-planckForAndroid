package com.fsck.k9.pEp.ui.keysync;

import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.Presenter;

import org.pEp.jniadapter.Identity;

import javax.inject.Inject;

public class AddDevicePresenter implements Presenter {

    private AddDeviceView view;
    private PEpProvider pEpProvider;
    private Identity partner;

    @Inject
    public AddDevicePresenter() {
    }

    public void initialize(AddDeviceView view, PEpProvider pEpProvider, String partnerUserId, String partnerAddress) {
        this.view = view;
        this.pEpProvider = pEpProvider;
        partner = new Identity();
        partner.user_id = partnerUserId;
        partner.address = partnerAddress;
        partner = pEpProvider.updateIdentity(partner);

        if (!partner.username.equals(partner.address)) {
            view.showCompletePartnerFormat(partner);
        } else {
            view.showPartnerFormat(partner);
        }
    }

    public void acceptHandshake() {
        pEpProvider.acceptHandshake(partner);
        view.close();
    }

    public void rejectHandshake() {
        pEpProvider.rejectHandshake(partner);
        view.close();
    }

    public void cancelHandshake() {
        pEpProvider.cancelHandshake(partner);
        view.goBack();
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
