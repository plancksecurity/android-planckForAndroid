package com.fsck.k9.pEp.ui.privacy.status;

import com.fsck.k9.pEp.models.PEpIdentity;

import foundation.pEp.jniadapter.Rating;

import java.util.List;

public interface PEpStatusView {
    void setupRecipients(List<PEpIdentity> pEpIdentities);

    void setupBackIntent(Rating rating);

    void showPEpTexts(String title, String suggestion);

    void updateIdentities(List<PEpIdentity> updatedIdentities);

    void setRating(Rating pEpRating);

    void showDataLoadError();

    void showBadge(Rating rating);

    void hideBadge();

    void showResetpEpDataFeedback();

    void finish();

    void showUndoTrust(String username);

    void showUndoMistrust(String username);

    void showMistrustFeedback(String username);

    void showItsOnlyOwnMsg();
}
