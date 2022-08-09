package com.fsck.k9.pEp.ui.activities.test

import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.common.BaseAndroidTest
import com.fsck.k9.pEp.infrastructure.TestK9
import com.fsck.k9.pEp.ui.activities.TestUtils
import security.pEp.mdm.FakeRestrictionsManager
import security.pEp.mdm.MailSettings
import security.pEp.mdm.RESTRICTION_PEP_ENABLE_PRIVACY_PROTECTION
import security.pEp.mdm.RESTRICTION_PEP_EXTRA_KEYS
import timber.log.Timber


class RestrictionsManager: BaseAndroidTest() {

    companion object {
        @JvmStatic
        fun setBooleanRestrictions(restriction: String, value: Boolean) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider() as FakeRestrictionsManager
            manager.setBoolean(restriction, value)
            val activity = TestUtils.getCurrentActivity()
            manager.updateTestRestrictions(activity)
        }

        fun setStringRestrictions(restriction: String, value: String) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider() as FakeRestrictionsManager
            manager.setString(restriction, value)
            val activity = TestUtils.getCurrentActivity()
            manager.updateTestRestrictions(activity)

        }

        fun getRestrictions(): MailSettings? {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider() as FakeRestrictionsManager
            var mailSettings = manager.getMailSettings()
            val manifestMailSettings = manager.getManifestMailSettings()
            Timber.e("==== app is $app, mailsettings is $mailSettings, manifestmailsettins is $manifestMailSettings")
            manager.applicationRestrictions.clear()
            mailSettings = manager.getMailSettings()
            Timber.e("==== app is $app, mailsettings is $mailSettings, manifestmailsettins is $manifestMailSettings")
            return mailSettings
        }

    }
}