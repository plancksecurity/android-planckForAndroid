package com.fsck.k9.planck.ui.privacy.status;

import com.fsck.k9.planck.models.PEpIdentity;

import foundation.pEp.jniadapter.Rating;

import java.util.List;

public interface PEpStatusView {
    void setupRecipients(List<PEpIdentity> pEpIdentities);

    void setupBackIntent(Rating rating, boolean forceUncrypted, boolean alwaysSecure);

    void updateIdentities(List<PEpIdentity> updatedIdentities);

    void setRating(Rating pEpRating);

    void showDataLoadError();

    void showResetpEpDataFeedback();

    void finish();

    void showUndoTrust(String username);

    void showUndoMistrust(String username);

    void showMistrustFeedback(String username);

    void showItsOnlyOwnMsg();

    void updateToolbarColor(Rating rating);

    void showResetPartnerKeyErrorFeedback();
    void showResetPartnerKeySuccessFeedback();
    void showResetPartnerKeyRequestDialog(PEpIdentity identity);
}
