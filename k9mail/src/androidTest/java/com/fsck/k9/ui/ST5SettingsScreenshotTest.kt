package com.fsck.k9.ui

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.R
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ST5SettingsScreenshotTest : BaseScreenshotTest() {

    @Test
    fun settingsTest() {
        openFirstScreen()
        openNavMenu()
        openSettings()
        mainSettingsTest()
        globalSettingsTest()
        accountSettingsTest()
        Espresso.pressBack()
    }

    private fun mainSettingsTest() {
        setTestSet("H")
        clickMenu()
        openAbout()
    }

    private fun globalSettingsTest() {
        setTestSet("I")
        openGlobalDisplaySettings()
        openGlobalPrivacySettings()
    }

    private fun accountSettingsTest() {
        setTestSet("J")
        openAccountSettings()
        openAccountPrivacySettings()
        Espresso.pressBack()
    }

    private fun openNavMenu() {
        clickClosedNavHamburger()
        getScreenShotCurrentActivity("nav menu folders")
    }

    private fun openSettings() {
        click(R.id.menu_header)
        sleep(2000)
        getScreenShotCurrentActivity("nav menu settings")
        click(R.id.configure_account_container)
        getScreenShotCurrentActivity("main settings")
    }

    private fun clickMenu() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("main settings menu")
    }

    private fun openAbout() {
        click(getString(R.string.about_action))
        getScreenShotCurrentActivity("about")
        swipeScrollView()
        sleep(1500)
        getScreenShotCurrentActivity("about scrolled")
        Espresso.pressBack()
    }

    private fun openGlobalDisplaySettings() {
        clickSetting(R.string.display_preferences)
        getScreenShotCurrentActivity("global display setting")
        swipeSettingsView()
        sleep(1000)
        getScreenShotCurrentActivity("global display setting scrolled")
        clickSettingDialog(R.string.settings_language_label, "global language selection setting")
        clickSettingDialog(R.string.settings_theme_label, "global theme selection setting")
        Espresso.pressBack()
    }

    private fun openGlobalPrivacySettings() {
        clickSetting(R.string.privacy_preferences)
        getScreenShotCurrentActivity("global privacy settings")
        expandSetting(R.string.reset)
        getScreenShotCurrentActivity("global settings reset own keys dialog")
        click(getString(R.string.cancel_action))
        expandSetting(R.string.sync_title)
        getScreenShotCurrentActivity("global settings trigger key sync")
        click(getString(R.string.cancel_action))
        //clickSettingDialog(R.string.passhphrase_new_keys_settings_title, "global passphrase setting") // only available in debug

        Espresso.pressBack()
    }

    private fun openAccountSettings() {
        runBlocking { waitForIdle() }
        clickListItem(R.id.accounts_list, 0)
        getScreenShotCurrentActivity("account main setting")
        clickSettingDialog(R.string.account_settings_description_label, "account name setting")
        Espresso.pressBack()
        clickSettingDialog(R.string.account_settings_mail_display_count_label, "local folder size")
        clickSettingDialog(R.string.account_settings_remote_search_num_label, "server search limit")
    }

    private fun openAccountPrivacySettings() {
        clickSetting(R.string.privacy_preferences)
        sleep(500)
        getScreenShotCurrentActivity("account privacy setting")
        Espresso.pressBack()
    }
}