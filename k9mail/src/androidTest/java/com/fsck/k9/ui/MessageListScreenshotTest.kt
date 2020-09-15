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
        setTestSet("C")
        openFirstScreen()
        openListItem()
    }

    @Test
    fun messageListTest() {
        setTestSet("D")
        openFirstScreen()
        swipeRightAndLeft()
        openCloseNavMenu()
        clickSearch()
        openOptionsMenu()
        openFoldersList()
        selectAll()
    }

    private fun swipeRightAndLeft() {
        swipeListItem(R.id.message_list, SWIPE_LEFT_ACTION)
        getScreenShotMessageList("item swipe left")
        closeSwipeListItem(R.id.message_list, SWIPE_LEFT_ACTION)

        swipeListItem(R.id.message_list, SWIPE_RIGHT_ACTION)
        getScreenShotMessageList("item swipe right")
        closeSwipeListItem(R.id.message_list, SWIPE_RIGHT_ACTION)
    }

    private fun openListItem() {
        runBlocking { waitForIdle() }
        clickListItem(R.id.message_list, 1)
        getScreenShotMessageList("inbox item 0")
    }

    private fun openCloseNavMenu() {
        clickClosedNavHamburger()
        getScreenShotCurrentActivity("nav menu")
        Espresso.pressBack()
    }

    private fun clickSearch() {
        click(R.id.search)
        getScreenShotCurrentActivity("click search")
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
    }

    private fun openOptionsMenu() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("click options menu")
    }

    private fun openFoldersList() {
        click(getString(R.string.folders_title))
        getScreenShotCurrentActivity("folders list")
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("click folders list options menu")
        Espresso.pressBack()
        Espresso.pressBack()
    }

    private fun selectAll() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.batch_select_all))
        getScreenShotCurrentActivity("all items selected")
        Espresso.pressBack()
    }
}