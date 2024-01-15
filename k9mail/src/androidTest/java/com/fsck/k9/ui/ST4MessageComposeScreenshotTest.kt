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
class ST4MessageComposeScreenshotTest : BaseScreenshotTest() {

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
    }

    private fun openEmptyCompose() {
        click(R.id.fab_button_compose_message)
        Espresso.closeSoftKeyboard()
        sleep(500)
        getScreenShotCurrentActivity("empty")
        Espresso.pressBack()
    }

    private fun clickReply() {
        runBlocking { waitForIdle() }
        click(R.id.fabReply)
        Espresso.closeSoftKeyboard()
        sleep(500)
        getScreenShotCurrentActivity("reply")
    }

    private fun openFabMenu() {
        runBlocking { waitForIdle() }
        longClick(R.id.openCloseButton)
        getScreenShotCurrentActivity("reply forward menu")
    }

    private fun openListItem() {
        runBlocking { waitForIdle() }
        clickListItem(R.id.message_list, 2)
        getScreenShotMessageList("inbox item 2")
    }

    private fun openRecipientsLayout() {
        runBlocking { waitForIdle() }
        scrollAndClick(R.id.recipient_expander)
        getScreenShotCurrentActivity("recipients expanded")
    }

    private fun openOptionsMenu() {
        runBlocking { waitForIdle() }
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("click options menu")
        Espresso.pressBack()
    }
}