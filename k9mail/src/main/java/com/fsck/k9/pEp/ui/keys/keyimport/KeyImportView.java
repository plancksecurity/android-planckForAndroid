package com.fsck.k9.pEp.ui.keys.keyimport;

import com.fsck.k9.pEp.PEpProvider;

public interface KeyImportView {

    void showPositiveFeedback();

    void finish();

    void renderDialog(PEpProvider.KeyDetail keyDetail, String from);

    void showNegativeFeedback();
}
