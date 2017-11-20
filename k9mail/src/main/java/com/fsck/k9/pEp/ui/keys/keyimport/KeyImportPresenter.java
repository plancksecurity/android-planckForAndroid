package com.fsck.k9.pEp.ui.keys.keyimport;

import android.content.Context;

import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;

import org.pEp.jniadapter.Identity;

import javax.inject.Inject;
import javax.inject.Named;

class KeyImportPresenter {

    private final PEpProvider pEp;
    private KeyImportView view;
    private PEpProvider.KeyDetail keyDetail;

    @Inject
    public KeyImportPresenter(@Named("MainUI") PEpProvider pEp) {
        this.pEp = pEp;

    }

    public void initialize(KeyImportView view, String fingerprint, String address, String username, String from) {
        this.view = view;
        keyDetail = new PEpProvider.KeyDetail(fingerprint, new Address(address, username));
        view.renderDialog(keyDetail, from);
    }

    void onAccept(Context context) {
        view.showPositiveFeedback();
        Identity id = PEpUtils.createIdentity(keyDetail.getAddress(), context);
        id.fpr = keyDetail.getFpr();
        pEp.myself(id);
        view.finish();
    }

    void onReject() {
        view.showNegativeFeedback();
        view.finish();
    }

    String formatFingerprint(String fpr) {
        String formattedFpr = PEpUtils.formatFpr(fpr);
        return formattedFpr.substring(0, formattedFpr.length() / 2 - 1) +
                "\n" + formattedFpr.substring(formattedFpr.length() / 2,
                formattedFpr.length() / 2 + PEpProvider.HALF_FINGERPRINT_LENGTH);
    }
}