package com.fsck.k9.ui

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
class MessageComposeScreenshotTest : BaseScreenshotTest() {

    @Test
    fun replyFirstInboxMessage() {
        setTestSet("G")
        openFirstScreen()
        getScreenShotMessageList("inbox list")
        openEmptyCompose()
        openListItem()
        openFabMenu()
        clickReply()
        runBlocking { waitForIdle() }
        Espresso.closeSoftKeyboard()
        openRecipientsLayout()
        openOptionsMenu()
        longClickStatus()
    }

    private fun openEmptyCompose() {
        click(R.id.fab_button_compose_message)
        getScreenShotCurrentActivity("empty")
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
    }

    private fun clickReply() {
        runBlocking { waitForIdle() }
        click(R.id.fabReply)
        getScreenShotCurrentActivity("reply")
    }

    private fun openFabMenu() {
        runBlocking { waitForIdle() }
        longClick(R.id.openCloseButton)
        getScreenShotCurrentActivity("reply forward menu")
    }

    private fun openListItem() {
        runBlocking { waitForIdle() }
        clickListItem(R.id.message_list, 0)
        getScreenShotMessageList("inbox item 0")
    }

    private fun openRecipientsLayout() {
        runBlocking { waitForIdle() }
        click(R.id.recipient_expander)
        getScreenShotCurrentActivity("recipients expanded")
    }

    private fun openOptionsMenu() {
        runBlocking { waitForIdle() }
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("click options menu")
        Espresso.pressBack()
    }

    private fun longClickStatus() {
        runBlocking { waitForIdle() }
        longClick(R.id.actionbar_message_view)
        sleep(2000)
        getScreenShotCurrentActivity("message status")
        clickPopUpMenuItem("Disable protection")
        getScreenShotCurrentActivity("unencrypted")
        Espresso.pressBack()
    }

}