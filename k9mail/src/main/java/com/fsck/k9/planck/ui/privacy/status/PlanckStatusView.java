package com.fsck.k9.planck.ui.privacy.status;

import com.fsck.k9.planck.models.PlanckIdentity;

import foundation.pEp.jniadapter.Rating;

import java.util.List;

public interface PlanckStatusView {
    void setupRecipients(List<PlanckIdentity> pEpIdentities);

    void setupBackIntent(Rating rating, boolean forceUncrypted, boolean alwaysSecure);

    void updateIdentities(List<PlanckIdentity> updatedIdentities);

    void showDataLoadError();

    void finish();
    void showTrustFeedback(String username);

    void showMistrustFeedback(String username);

    void showItsOnlyOwnMsg();
}
