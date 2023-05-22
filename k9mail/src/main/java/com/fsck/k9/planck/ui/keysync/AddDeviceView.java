package com.fsck.k9.planck.ui.keysync;

import foundation.pEp.jniadapter.Identity;

import java.util.List;

public interface AddDeviceView {

    void showPartnerFormat(Identity partner);

    void showCompletePartnerFormat(Identity partner);

    void close(boolean accepted);

    void goBack();

    void showIdentities(List<Identity> identities);

    void showError();

    void hideIdentities();

    void showFPR();

    void showKeySyncTitle();
}
