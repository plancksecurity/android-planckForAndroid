package com.fsck.k9.planck.ui.activities.test

import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.common.BaseAndroidTest
import com.fsck.k9.planck.infrastructure.TestK9
import com.fsck.k9.planck.ui.activities.TestUtils
import security.planck.mdm.FakeRestrictionsManager
import security.planck.mdm.MailIncomingOutgoingSettings
import security.planck.mdm.MailSettings
import timber.log.Timber


class RestrictionsManager: BaseAndroidTest() {

    companion object {
        @JvmStatic
        fun setBooleanRestrictions(restriction: String, value: Boolean) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider()
            if (manager is FakeRestrictionsManager) {
                manager.setBoolean(restriction, value)
                val activity = TestUtils.getCurrentActivity()
                manager.updateTestRestrictions(activity)
            }
        }

        @JvmStatic
        fun setStringRestrictions(setting: String, value: String) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider()
            if (manager is FakeRestrictionsManager) {
                manager.setString(setting, value)
                val activity = TestUtils.getCurrentActivity()
                manager.updateTestRestrictions(activity)
            }

        }

        @JvmStatic
        fun setIncomingBundleSettings(server: String, securityType: String, port: Int, userName: String) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider()
            if (manager is FakeRestrictionsManager) {
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
        }

        @JvmStatic
        fun setOutgoingBundleSettings(server: String, securityType: String, port: Int, userName: String) {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider()
            if (manager is FakeRestrictionsManager) {
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
        }

        @JvmStatic
        fun getRestrictions(): MailSettings? {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider()
            if (manager is FakeRestrictionsManager) {
                var mailSettings = manager.getMailSettings()
                val manifestMailSettings = manager.getManifestMailSettings()
                Timber.e("==== app is $app, mailsettings is $mailSettings, manifestmailsettins is $manifestMailSettings")
                mailSettings = manager.getMailSettings()
                Timber.e("==== app is $app, mailsettings is $mailSettings, manifestmailsettins is $manifestMailSettings")
                return mailSettings
            }
            return null
        }

        @JvmStatic
        fun getSetting(setting: String): String? {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider()
            return if (manager is FakeRestrictionsManager) manager.getString(setting) else ""
        }

        @JvmStatic
        fun compareSetting(server: String, securityType: String, port: Int, userName: String, settings: MailIncomingOutgoingSettings): Boolean {
            if (settings.server != server ||
                    settings.securityType != securityType ||
                    settings.port != port ||
                    settings.userName != userName) {
                return false
            }
            return true
        }

        @JvmStatic
        fun resetSettings() {
            val app = ApplicationProvider.getApplicationContext<TestK9>()
            val manager = app.component.restrictionsProvider()
            if (manager is FakeRestrictionsManager) {
                manager.resetSettings()
            }
        }

    }
}