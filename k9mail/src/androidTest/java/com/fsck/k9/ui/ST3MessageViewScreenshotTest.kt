package com.fsck.k9.ui

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.R
import com.fsck.k9.planck.ui.activities.TestUtils
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ST3MessageViewScreenshotTest : BaseScreenshotTest() {

    @Test
    fun openMessages() {
        openFirstScreen()
        clickListItem(R.id.message_list, 0)
        Espresso.pressBack()
        sleep(500)
        getScreenShotMessageList("inbox initial status")
        messageClicks()
        privacyStatus()
        privacyStatusActions()
        getScreenShotMessageList("inbox all status")
    }

    private fun messageClicks() {
        setTestSet("D")
        clickListItem(R.id.message_list, 0)
        getScreenShotMessageList("inbox item 0")
        openMoreOptions()
        openMessageOptionsMenu()
        clickSend()
        clickRefile()
        showHideHeaders()
        Espresso.pressBack()
    }

    private fun privacyStatus() {
        setTestSet("E")
        clickListItem(R.id.message_list, 0)
        openPrivacyStatus()
        privacyStatusLanguageClicks()
        Espresso.pressBack()
        Espresso.pressBack()
    }

    private fun privacyStatusActions() {
        setTestSet("F")
        rejectHandshake()
        acceptHandshake()
        resetCommunication()
        openThreadMessage()
    }

    private fun openMoreOptions() {
        click(R.id.message_more_options)
        getScreenShotMessageList("click more options")
        Espresso.pressBack()
    }

    private fun openMessageOptionsMenu() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        sleep(500)
        getScreenShotMessageList("click options menu")
        Espresso.pressBack()
    }

    private fun clickSend() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.single_message_options_action))
        sleep(500)
        getScreenShotMessageList("click send... options menu")
        Espresso.pressBack()
    }

    private fun clickRefile() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.refile_action))
        sleep(500)
        getScreenShotMessageList("click refile... options menu")
        Espresso.pressBack()
    }

    private fun showHideHeaders() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.show_headers_action))
        sleep(500)
        getScreenShotMessageList("message headers")
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.hide_headers_action))
    }

    private fun openPrivacyStatus() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.pep_title_activity_privacy_status))
    }

    private fun privacyStatusLanguageClicks() {
        getScreenShotCurrentActivity("privacy status")
        clickListChildItem(R.id.my_recycler_view, R.id.trustwords)
        sleep(1000)
        getScreenShotCurrentActivity("long trustwords")

        clickListChildItem(R.id.my_recycler_view, R.id.change_language)
        sleep(1000)
        getScreenShotCurrentActivity("click trustwords language")
        Espresso.pressBack()
    }

    private fun rejectHandshake() {
        clickListItem(R.id.message_list, 0)
        openPrivacyStatus()
        clickListChildItem(R.id.my_recycler_view, R.id.rejectHandshake)
        sleep(1000)
        getScreenShotCurrentActivity("reject handshake")
        Espresso.pressBack()
        getScreenShotCurrentActivity("red status")
        Espresso.pressBack()
    }

    private fun acceptHandshake() {
        clickListItem(R.id.message_list, 1)
        openPrivacyStatus()
        clickListChildItem(R.id.my_recycler_view, R.id.confirmHandshake)
        sleep(1000)
        getScreenShotCurrentActivity("accept handshake")
        Espresso.pressBack()
        getScreenShotCurrentActivity("green status")
        Espresso.pressBack()
    }

    private fun resetCommunication() {
        clickListItem(R.id.message_list, 2)
        openPrivacyStatus()
        //click(R.id.button_identity_key_reset)
        TestUtils.waitForIdle()
        getScreenShotCurrentActivity("reset partner's keys confirmation")
        click(context.getString(R.string.reset_partner_keys_confirmation_action))
        sleep(1000)
        getScreenShotCurrentActivity("reset partner's feedback dialog")
        click(context.getString(R.string.close))
        getScreenShotCurrentActivity("after reset partner keys")
        Espresso.pressBack()
        getScreenShotCurrentActivity("message after partner keys reset")
        Espresso.pressBack()
    }

    private fun openThreadMessage() {
        clickListItem(R.id.message_list, 3)
        getScreenShotMessageList("thread message")
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotMessageList("click options menu")
        Espresso.pressBack()
        Espresso.pressBack()
    }
}