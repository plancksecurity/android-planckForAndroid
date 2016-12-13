package com.fsck.k9.pEp.ui;

import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.pEp.models.PEpIdentity;

import org.pEp.jniadapter.Rating;

import java.util.List;

public interface PEpStatusView {
    void setupRecipients(List<PEpIdentity> pEpIdentities);

    void setupBackIntent(Rating rating);

    void showPEpTexts(String title, String suggestion);

    void updateIdentities(List<PEpIdentity> updatedIdentities);

    void saveLocalMessage(LocalMessage localMessage);

    void setRating(Rating pEpRating);

    void showError(int status_loading_error);
}
