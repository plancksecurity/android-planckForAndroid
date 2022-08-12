package com.fsck.k9.pEp.ui.activities.test

import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Preferences
import com.fsck.k9.common.BaseAndroidTest
import com.fsck.k9.pEp.infrastructure.TestK9
import com.fsck.k9.pEp.ui.activities.TestUtils
import com.fsck.k9.preferences.SettingsExporter
import kotlinx.coroutines.*
import security.pEp.mdm.FakeRestrictionsManager
import security.pEp.mdm.FakeRestrictionsManager.Companion.getProvisioningRestrictions
import security.pEp.mdm.MailIncomingOutgoingSettings
import security.pEp.mdm.MailSettings
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

        @JvmStatic
        fun setStringRestrictions(restriction: String, value: String) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider() as FakeRestrictionsManager
            manager.setString(restriction, value)
            val activity = TestUtils.getCurrentActivity()
            manager.updateTestRestrictions(activity)

        }

        @JvmStatic
        fun setIncomingBundleSettings(server: String, securityType: String, port: Int, userName: String) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider() as FakeRestrictionsManager
            var settings = manager.getMailSettings()!!
            val newSettings = settings.copy(
                incoming = settings!!.incoming!!.copy(
                    server = server,
                    securityType = securityType,
                    port = port,
                    userName = userName
                )
            )
            manager.setMailSettings(newSettings)
            val activity = TestUtils.getCurrentActivity()
            manager.updateTestRestrictions(activity)
        }

        @JvmStatic
        fun setOutgoingBundleSettings(server: String, securityType: String, port: Int, userName: String) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider() as FakeRestrictionsManager
            var settings = manager.getMailSettings()!!
            val newSettings = settings.copy(
                outgoing = settings!!.incoming!!.copy(
                    server = server,
                    securityType = securityType,
                    port = port,
                    userName = userName
                )
            )
            manager.setMailSettings(newSettings)
            val activity = TestUtils.getCurrentActivity()
            manager.updateTestRestrictions(activity)
        }

        @JvmStatic
        fun getRestrictions(): MailSettings? {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider() as FakeRestrictionsManager
            var mailSettings = manager.getMailSettings()
            val manifestMailSettings = manager.getManifestMailSettings()
            Timber.e("==== app is $app, mailsettings is $mailSettings, manifestmailsettins is $manifestMailSettings")
            mailSettings = manager.getMailSettings()
            Timber.e("==== app is $app, mailsettings is $mailSettings, manifestmailsettins is $manifestMailSettings")
            return mailSettings
        }

    }
}