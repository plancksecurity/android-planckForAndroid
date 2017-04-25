package com.fsck.k9.pEp.ui.fragments;

import com.fsck.k9.activity.setup.AccountSetupCheckSettings;

public interface PEpSettingsChecker {

    enum Redirection {OUTGOING, TO_APP};

    void checkSettings(String accountUuid, AccountSetupCheckSettings.CheckDirection checkDirection,
                       Boolean makeDefault, String procedence, Boolean isEditing,
                       ResultCallback<Redirection> callback);

    interface Callback {
        void onError(String customMessage);
    }

    interface ResultCallback<Result> extends Callback {
        void onLoaded(Result result);
    }
}
