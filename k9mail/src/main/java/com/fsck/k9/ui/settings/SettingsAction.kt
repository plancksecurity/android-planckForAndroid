package com.fsck.k9.ui.settings

import android.app.Activity
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.activity.setup.AccountSetupBasics
import security.pEp.ui.about.AboutActivity

internal enum class SettingsAction {
    GENERAL_SETTINGS {
        override fun execute(activity: Activity) {
            SettingsActivity.launch(activity)
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
