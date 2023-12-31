package com.fsck.k9.planck.ui.fragments;

import com.fsck.k9.Account;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.planck.ui.infrastructure.exceptions.PlanckSetupException;

public interface PlanckSettingsChecker {

    enum Redirection {OUTGOING, TO_APP};

    void checkSettings(Account account, AccountSetupCheckSettings.CheckDirection checkDirection,
                       Boolean makeDefault, String procedence, Boolean isEditing,
                       ResultCallback<Redirection> callback);

    interface Callback {
        void onError(PlanckSetupException exception);
    }

    interface ResultCallback<Result> extends Callback {
        void onLoaded(Result result);
    }
}
