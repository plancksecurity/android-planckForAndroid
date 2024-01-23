package com.fsck.k9.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.R
import com.fsck.k9.planck.ui.activities.TestUtils
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ST3MessageViewScreenshotTest : BaseScreenshotTest() {

    @Test
    fun openMessages() {
        openFirstScreen()
        getScreenShotMessageList("inbox initial status")
        messageClicks()
        verifyPartner()
        verifyPartnerActions()
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

    private fun verifyPartner() {
        setTestSet("E")
        clickListItem(R.id.message_list, 0)
        openPrivacyStatus()
        privacyStatusLanguageClicks()
        Espresso.pressBack()
    }

    private fun verifyPartnerActions() {
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
        click(getString(R.string.move_action))
        sleep(500)
        getScreenShotMessageList("Choose folder to move messsage")
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
        click(R.id.actionbar_message_view)
    }

    private fun privacyStatusLanguageClicks() {
        getScreenShotCurrentActivity("privacy status")
        click(R.id.show_long_trustwords)
        sleep(1000)
        getScreenShotCurrentActivity("long trustwords")
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        sleep(1000)
        getScreenShotCurrentActivity("click trustwords language")
        onView(withText("Deutsch")).inRoot(isPlatformPopup()).perform(ViewActions.click())
        click(R.id.dissmissActionButton)
    }

    private fun rejectHandshake() {
        clickListItem(R.id.message_list, 0)
        openPrivacyStatus()
        runBlocking { waitForIdle() }
        click(R.id.negativeActionButton)
        sleep(1000)
        getScreenShotCurrentActivity("reject handshake")
        click(R.id.afirmativeActionButton)
        runBlocking { waitForIdle() }
        click(R.id.afirmativeActionButton)
        sleep(1000)
        getScreenShotCurrentActivity("Dangerous status")
        Espresso.pressBack()
    }

    private fun acceptHandshake() {
        clickListItem(R.id.message_list, 1)
        openPrivacyStatus()
        runBlocking { waitForIdle() }
        click(R.id.afirmativeActionButton)
        sleep(1000)
        getScreenShotCurrentActivity("accept handshake")
        click(R.id.afirmativeActionButton)
        runBlocking { waitForIdle() }
        click(R.id.afirmativeActionButton)
        sleep(1000)
        getScreenShotCurrentActivity("Verified status")
        Espresso.pressBack()
    }

    private fun resetCommunication() {
        clickListItem(R.id.message_list, 2)
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.reset_partner_key_action))
        TestUtils.waitForIdle()
        getScreenShotCurrentActivity("reset sender's keys confirmation")
        click(getString(R.string.reset_partner_keys_confirmation_action))
        sleep(1000)
        getScreenShotCurrentActivity("reset sender's feedback dialog")
        click(getString(R.string.close))
        sleep(1000)
        getScreenShotCurrentActivity("message after sender keys reset")
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