package com.fsck.k9.pEp.ui.keysync;

import org.pEp.jniadapter.Identity;

import java.util.ArrayList;

public interface KeysyncManagementView {
    void showIdentities(ArrayList<Identity> identitiesToShow);

    void showError();
}
