package com.fsck.k9.pEp.ui.keysync;

import org.pEp.jniadapter.Identity;

import java.util.List;

public interface AddDeviceView {

    void showPartnerFormat(Identity partner);

    void showCompletePartnerFormat(Identity partner);

    void close();

    void goBack();

    void showIdentities(List<Identity> identities);

    void showError();

    void hideIdentities();
}
