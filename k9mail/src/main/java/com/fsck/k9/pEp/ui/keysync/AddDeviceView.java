package com.fsck.k9.pEp.ui.keysync;

import org.pEp.jniadapter.Identity;

public interface AddDeviceView {

    void showPartnerFormat(Identity partner);

    void showCompletePartnerFormat(Identity partner);

    void close();

    void goBack();
}
