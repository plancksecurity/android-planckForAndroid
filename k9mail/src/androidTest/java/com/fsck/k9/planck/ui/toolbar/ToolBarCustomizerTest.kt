package com.fsck.k9.planck.ui.toolbar

import androidx.annotation.AttrRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.R
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.matchers.withBackgroundColour
import com.fsck.k9.planck.ui.activities.UtilsPackage.waitUntilIdle
import com.fsck.k9.planck.ui.tools.ThemeManager
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolBarCustomizerTest {
    @get:Rule
    var mActivityRule = ActivityTestRule(MessageCompose::class.java)

    @Test
    fun `status bar changes color to default`() {
        runBlocking(Dispatchers.Main) { mActivityRule.activity.setDefaultStatusBarColor() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onView(withId(android.R.id.statusBarBackground))
            .check(matches(ViewMatchers.isDisplayed()))

        assertEquals(
            getColor(R.attr.statusbarDefaultColor),
            mActivityRule.activity.window.statusBarColor
        )
    }

    @Test
    fun `status bar changes color to message`() {
        runBlocking(Dispatchers.Main) { mActivityRule.activity.setMessageStatusBarColor() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onView(withId(android.R.id.statusBarBackground))
            .check(matches(ViewMatchers.isDisplayed()))

        assertEquals(
            getColor(R.attr.messageViewStatusBarColor),
            mActivityRule.activity.window.statusBarColor
        )
    }

    @Test
    fun `toolbar changes color to default`() {
        waitForToolbar(50)
        runBlocking(Dispatchers.Main) { mActivityRule.activity.setDefaultToolbarColor() }
        waitForToolbar(70)


        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColour(getResource(R.attr.toolbarDefaultColor))))
    }

    @Test
    fun `toolbar changes color to message`() {
        waitForToolbar(50)
        runBlocking(Dispatchers.Main) { mActivityRule.activity.setMessageToolbarColor() }
        waitForToolbar(70)


        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColour(getResource(R.attr.messageViewToolbarColor))))
    }

    private fun waitForToolbar(i: Int) {
        for (waitLoop in 0..i) {
            waitUntilIdle()
            onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()))
            onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()))
            waitUntilIdle()
        }
    }

    private fun getResource(@AttrRes attr: Int): Int =
        ThemeManager.getAttributeResource(mActivityRule.activity, attr)

    private fun getColor(@AttrRes attr: Int): Int =
        ThemeManager.getColorFromAttributeResource(mActivityRule.activity, attr)
}