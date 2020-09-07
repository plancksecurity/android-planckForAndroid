package com.fsck.k9.ui

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.R
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenshotTest : BaseScreenshotTest() {


    @Test
    fun mainSettingsTest() {
        openFirstScreen()
        openNavMenu()
        openSettings()
        clickMenu()
    }

    @Test
    fun globalSettingsTest() {
        openFirstScreen()
        openNavMenu()
        openSettings()
        openGlobalDisplaySettings()
        openGlobalInteractionSettings()
        openGlobalNotificationsSettings()
        openGlobalPrivacySettings()
        openGlobalAdvancedSettings()
    }

    @Test
    fun accountSettingsTest() {
        openFirstScreen()
        openNavMenu()
        openSettings()
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


    private fun openGlobalDisplaySettings() {
        clickSetting(R.string.display_preferences)
        getScreenShotCurrentActivity("global display setting")

        clickSettingDialog(R.string.settings_language_label, "global language selection setting")
        clickSettingDialog(R.string.font_size_settings_title, "global font selection setting")
        clickSettingDialog(R.string.global_settings_preview_lines_label, "global preview lines setting")
        clickSettingDialog(R.string.global_settings_messageview_visible_refile_actions_title,
                "global visible message actions setting")

        Espresso.pressBack()
    }

    private fun openGlobalInteractionSettings() {
        clickSetting(R.string.interaction_preferences)
        expandSetting(R.string.start_integrated_inbox_title)
        getScreenShotCurrentActivity("global interaction setting")

        clickSettingDialog(R.string.volume_navigation_title, "global volume key navigation setting")
        clickSettingDialog(R.string.global_settings_confirm_actions_title, "global confirm actions setting")

        Espresso.pressBack()
    }

    private fun openGlobalNotificationsSettings() {
        clickSetting(R.string.notifications_title)
        getScreenShotCurrentActivity("global notifications setting")

        clickSettingDialog(R.string.global_settings_notification_quick_delete_title
                , "global show delete button setting")
        clickSettingDialog(R.string.global_settings_lock_screen_notification_visibility_title
                , "global lock screen notifications setting")

        Espresso.pressBack()
    }

    private fun openGlobalPrivacySettings() {
        clickSetting(R.string.privacy_preferences)
        expandSetting(R.string.reset)
        getScreenShotCurrentActivity("global privacy setting")

        clickSettingDialog(R.string.reset, "global reset accounts setting")
        click(getString(R.string.cancel_action))
        clickSettingDialog(R.string.master_key_management, "global key management setting")
        clickSettingDialog(R.string.passhphrase_new_keys_settings_title, "global passphrase setting")
        clickSettingDialog(R.string.blacklist_title, "global blacklist setting")

        Espresso.pressBack()
    }


    private fun openGlobalAdvancedSettings() {
        clickSetting(R.string.advanced)
        getScreenShotCurrentActivity("global advanced setting")

        clickSettingDialog(R.string.settings_attachment_default_path, "global attachments save path setting")
        clickSettingDialog(R.string.background_ops_label, "global background sync setting")

        Espresso.pressBack()

    }
}