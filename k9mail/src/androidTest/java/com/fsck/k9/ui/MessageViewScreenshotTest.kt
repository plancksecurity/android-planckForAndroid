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
class MessageViewScreenshotTest : BaseScreenshotTest(){

    @Test
    fun afterAccountSetup() {
        openFirstScreen()
        openSingleInboxMessage()
    }

    private fun openSingleInboxMessage() {
        getScreenShotMessageList("inbox list")
        sleep(2000)
        clickListItem(R.id.message_list, 0)
        getScreenShotMessageList("inbox item 0")
        click(R.id.message_more_options)
        getScreenShotMessageList("click more options")
        Espresso.pressBack()
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotMessageList("click options menu")
    }

}