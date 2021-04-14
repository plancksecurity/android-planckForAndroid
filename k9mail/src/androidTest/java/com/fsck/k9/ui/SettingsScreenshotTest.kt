package com.fsck.k9.ui

import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.R
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenshotTest : BaseScreenshotTest() {

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
        openAboutAndLicense()
        Espresso.pressBack()
    }

    private fun globalSettingsTest() {
        setTestSet("I")
        openGlobalDisplaySettings()
        openGlobalInteractionSettings()
        openGlobalNotificationsSettings()
        openGlobalPrivacySettings()
        openGlobalAdvancedSettings()
    }

    private fun accountSettingsTest() {
        setTestSet("J")
        openAccountSettings()
        openAccountGeneralSettings()
        openAccountFetchingSettings()
        openAccountSendingSettings()
        openAccountDefaultFoldersSettings()
        openAccountNotificationsSettings()
        openAccountSearchSettings()
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

    private fun openAboutAndLicense() {
        click(getString(R.string.about_action))
        getScreenShotCurrentActivity("about")
        swipeScrollView()
        sleep(1500)
        getScreenShotCurrentActivity("about scrolled")
        click(R.id.license_button)
        getScreenShotCurrentActivity("license")
        Espresso.pressBack()
    }

    private fun openGlobalDisplaySettings() {
        clickSetting(R.string.display_preferences)
        getScreenShotCurrentActivity("global display setting")
        swipeSettingsView()
        sleep(1000)
        getScreenShotCurrentActivity("global display setting scrolled")

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

        clickSettingDialog(R.string.global_settings_notification_quick_delete_title, "global show delete button setting")
        clickSettingDialog(R.string.global_settings_lock_screen_notification_visibility_title, "global lock screen notifications setting")

        Espresso.pressBack()
    }

    private fun openGlobalPrivacySettings() {
        clickSetting(R.string.privacy_preferences)
        expandSetting(R.string.reset)
        getScreenShotCurrentActivity("global privacy setting")
        swipeSettingsView()
        sleep(1000)
        getScreenShotCurrentActivity("global privacy setting scrolled")

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            clickSettingDialog(R.string.settings_attachment_default_path, "global attachments save path setting")
        }
        clickSettingDialog(R.string.background_ops_label, "global background sync setting")
        Espresso.pressBack()
    }

    private fun openAccountSettings() {
        runBlocking { waitForIdle() }
        clickListItem(R.id.accounts_list, 0)
        getScreenShotCurrentActivity("account main setting")
    }

    private fun openAccountGeneralSettings() {
        clickSetting(R.string.account_settings_general_title)
        expandSetting(R.string.account_settings_reading_mail)
        sleep(500)
        getScreenShotCurrentActivity("account general setting")

        clickSettingDialog(R.string.account_settings_description_label, "account name setting")
        Espresso.pressBack()
        clickSettingDialog(R.string.account_settings_show_pictures_label, "account always show images setting")

        Espresso.pressBack()
    }

    private fun openAccountFetchingSettings() {
        clickSetting(R.string.account_settings_sync)
        expandSetting(R.string.account_settings_mail_display_count_label)
        sleep(500)
        getScreenShotCurrentActivity("account fetching email setting")
        swipeSettingsView()
        sleep(500)
        getScreenShotCurrentActivity("account fetching email setting scrolled")

        clickSettingDialog(R.string.account_settings_incoming_label, "account incoming server setting")
        clickSettingDialog(R.string.account_settings_mail_display_count_label, "account local folder size setting")
        clickSettingDialog(R.string.account_settings_message_age_label, "account sync messages from setting")
        clickSettingDialog(R.string.account_settings_autodownload_message_size_label, "account fetch messages up to setting")
        clickSettingDialog(R.string.account_settings_mail_check_frequency_label, "account folder poll frequency setting")
        clickSettingDialog(R.string.account_settings_folder_sync_mode_label, "account poll folders setting")
        clickSettingDialog(R.string.account_settings_folder_push_mode_label, "account push folders setting")
        clickSettingDialog(R.string.account_setup_incoming_delete_policy_label, "account delete message behaviour setting")
        clickSettingDialog(R.string.account_setup_expunge_policy_label, "account erase deleted messages behaviour setting")
        clickSettingDialog(R.string.account_setup_push_limit_label, "account max folders push check setting")
        clickSettingDialog(R.string.idle_refresh_period_label, "account refresh idle connections setting")

        Espresso.pressBack()
    }

    private fun openAccountSendingSettings() {
        clickSetting(R.string.account_settings_composition)
        expandSetting(R.string.account_settings_quote_style_label)
        sleep(500)
        getScreenShotCurrentActivity("account sending email setting")

        clickSettingDialog(R.string.account_settings_composition_label, "account composition defaults setting")
        clickSettingDialog(R.string.account_settings_identities_label, "account manage identities setting")
        clickSettingDialog(R.string.account_settings_message_format_label, "account message format setting")
        clickSettingDialog(R.string.account_settings_outgoing_label, "account outgoing server setting")
        clickSettingDialog(R.string.account_settings_quote_style_label, "account reply quoting style setting")
        clickSettingDialog(R.string.account_settings_quote_prefix_label, "account quoted text preview setting")
        Espresso.pressBack()

        Espresso.pressBack()
    }

    private fun openAccountDefaultFoldersSettings() {
        clickSetting(R.string.account_settings_folders)
        getScreenShotCurrentActivity("account default folders setting")

        clickSettingDialog(R.string.account_setup_auto_expand_folder, "account auto-expand folder setting")
        clickSettingDialog(R.string.account_settings_folder_display_mode_label, "account folders to display setting")
        clickSettingDialog(R.string.account_settings_folder_target_mode_label, "account move copy destination folders setting")
        clickSettingDialog(R.string.archive_folder_label, "account archive folder setting")
        clickSettingDialog(R.string.drafts_folder_label, "account drafts folder setting")
        clickSettingDialog(R.string.sent_folder_label, "account sent folder setting")
        clickSettingDialog(R.string.spam_folder_label, "account spam folder setting")
        clickSettingDialog(R.string.trash_folder_label, "account trash folder setting")

        Espresso.pressBack()
    }

    private fun openAccountNotificationsSettings() {
        clickSetting(R.string.notifications_title)
        expandSetting(R.string.account_settings_folder_notify_new_mail_mode_label)
        sleep(500)
        getScreenShotCurrentActivity("account notifications setting")


        clickSettingDialog(R.string.account_settings_folder_notify_new_mail_mode_label, "account notification folders setting")

        Espresso.pressBack()
    }

    private fun openAccountSearchSettings() {
        clickSetting(R.string.account_settings_search)
        getScreenShotCurrentActivity("account search setting")

        clickSettingDialog(R.string.account_settings_remote_search_num_label, "account Server search limit setting")

        Espresso.pressBack()
    }

    private fun openAccountPrivacySettings() {
        clickSetting(R.string.privacy_preferences)
        expandSetting(R.string.pep_sync_enable_account)
        sleep(500)
        getScreenShotCurrentActivity("account privacy setting")

        clickSettingDialog(R.string.reset, "account reset setting")
        click(getString(R.string.cancel_action))

        expandSetting(R.string.pep_sync_enable_account)
        importKey()
        Espresso.pressBack()
    }

    private fun importKey() {
        testUtils.externalAppRespondWithFile(R.raw.test_key)

        clickSetting(R.string.pgp_key_import_title)
        runBlocking { waitForIdle() }
        sleep(1000)
        getScreenShotCurrentActivity("import key step 1")

        click(R.id.acceptButton)
        runBlocking { waitForIdle() }
        sleep(1000)
        getScreenShotCurrentActivity("import key step 2")
    }

}