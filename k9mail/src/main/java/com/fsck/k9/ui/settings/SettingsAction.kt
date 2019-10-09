package com.fsck.k9.ui.settings

import android.app.Activity
import com.fsck.k9.activity.Accounts
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.ui.settings.general.GeneralSettingsActivity
import com.fsck.k9.pEp.ui.AboutActivity

internal enum class SettingsAction {
    GENERAL_SETTINGS {
        override fun execute(activity: Activity) {
            Accounts.start(activity)
        }
    },
    ADD_ACCOUNT {
        override fun execute(activity: Activity) {
            AccountSetupBasics.actionNewAccount(activity)
        }
    },
    ABOUT_SCREEN {
        override fun execute(activity: Activity) {
            AboutActivity.onAbout(activity)
        }
    };

    abstract fun execute(activity: Activity)
}
