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
    private Context context;
    private KeyImportView view;
    private PEpProvider.KeyDetail keyDetail;
    private String from;

    @Inject
    public KeyImportPresenter(@Named("MainUI") PEpProvider pEp) {
        this.pEp = pEp;

    }

    public void initialize(KeyImportView view, String fingerprint, String address, String username, String from) {
        this.view = view;
        context = view.getApplicationContext();
        keyDetail = new PEpProvider.KeyDetail(fingerprint, new Address(address, username));
        this.from = from;
        view.renderDialog(keyDetail, from);
    }

    public void onAccept() {
        view.showPositiveFeedback();
        Identity id = PEpUtils.createIdentity(keyDetail.getAddress(), context);
        id.fpr = keyDetail.getFpr();
        pEp.myself(id);
        view.finish();
    }

    public void onReject() {
        view.showPositiveFeedback();
        view.finish();
    }

    public String formatFingerprint(String fpr) {
        String formattedFpr = PEpUtils.formatFpr(fpr);
        return formattedFpr.substring(0, formattedFpr.length() / 2 - 1) +
                "\n" + formattedFpr.substring(formattedFpr.length() / 2,
                formattedFpr.length() / 2 + PEpProvider.HALF_FINGERPRINT_LENGTH);
    }
}