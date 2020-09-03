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
class MessageListScreenshotTest : BaseScreenshotTest() {

    @Test
    fun threadConversation() {
        openFirstScreen()
        openListItem()
    }

    private fun openListItem() {
        runBlocking { waitForIdle() }
        clickListItem(R.id.message_list, 1)
        getScreenShotMessageList("inbox item 0")
    }

    private fun openOptionsMenu() {
        runBlocking { waitForIdle() }
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("click options menu")
    }
}