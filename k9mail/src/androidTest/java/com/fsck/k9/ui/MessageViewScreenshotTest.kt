package com.fsck.k9.ui

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.R
import org.junit.Test
import org.junit.runner.RunWith
/*
 *   For this test we need to:
 *   1 - create one thread conversation
 *   2 - send 1 message to 3 different bots
 */

@LargeTest
@RunWith(AndroidJUnit4::class)
class MessageViewScreenshotTest : BaseScreenshotTest() {

    @Test
    fun openMessages() {
        openFirstScreen()
        messageClicks()
        privacyStatus()
        privacyStatusActions()
    }

    private fun messageClicks() {
        setTestSet("D")
        openSingleInboxMessage(0)
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
        openSingleInboxMessage(0)
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

    private fun openSingleInboxMessage(position: Int) {
        clickListItem(R.id.message_list, position)
    }

    private fun openMoreOptions() {
        click(R.id.message_more_options)
        getScreenShotMessageList("click more options")
        Espresso.pressBack()
    }

    private fun openMessageOptionsMenu() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotMessageList("click options menu")
        Espresso.pressBack()
    }

    private fun clickSend() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.single_message_options_action))
        getScreenShotMessageList("click send... options menu")
        Espresso.pressBack()
    }

    private fun clickRefile() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.refile_action))
        getScreenShotMessageList("click refile... options menu")
        Espresso.pressBack()
    }

    private fun showHideHeaders() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.show_headers_action))
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
        openSingleInboxMessage(0)
        openPrivacyStatus()
        clickListChildItem(R.id.my_recycler_view, R.id.rejectHandshake)
        sleep(1000)
        getScreenShotCurrentActivity("reject handshake")
        Espresso.pressBack()
        Espresso.pressBack()
    }

    private fun acceptHandshake() {
        openSingleInboxMessage(1)
        openPrivacyStatus()
        clickListChildItem(R.id.my_recycler_view, R.id.confirmHandshake)
        sleep(1000)
        getScreenShotCurrentActivity("accept handshake")
        Espresso.pressBack()
        Espresso.pressBack()
    }

    private fun resetCommunication() {
        openSingleInboxMessage(2)
        openPrivacyStatus()
        click(R.id.button_identity_key_reset)
        sleep(1000)
        getScreenShotCurrentActivity("reset communication")
        Espresso.pressBack()
        Espresso.pressBack()
    }

    private fun openThreadMessage() {
        openSingleInboxMessage( 3)
        getScreenShotMessageList("thread message")
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotMessageList("click options menu")
        Espresso.pressBack()
        Espresso.pressBack()
    }
}