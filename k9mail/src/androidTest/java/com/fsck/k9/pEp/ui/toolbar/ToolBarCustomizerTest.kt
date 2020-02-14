package com.fsck.k9.pEp.ui.toolbar

import androidx.core.content.ContextCompat
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
import com.fsck.k9.pEp.ui.activities.UtilsPackage
import com.fsck.k9.pEp.ui.activities.UtilsPackage.waitUntilIdle
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolBarCustomizerTest {
    @get:Rule
    var mActivityRule = ActivityTestRule(MessageCompose::class.java)

    @Test
    fun check_if_status_bar_changes_color_by_color_resource() {
        val colorRes = R.color.white

        mActivityRule.activity.setStatusBarPepColor(colorRes)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val beforeColour = mActivityRule.activity.window.statusBarColor
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        mActivityRule.activity.setStatusBarPepColor(colorRes)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onView(withId(android.R.id.statusBarBackground))
                .check(matches(ViewMatchers.isDisplayed()))

        val afterColour = mActivityRule.activity.window.statusBarColor

        Assert.assertEquals(beforeColour, afterColour)
    }

    @Test
    fun check_if_toolbar_changes_color_by_color_resource() {
        waitForToolbar(50)

        val colorRes = R.color.purple
        val color = ContextCompat.getColor(mActivityRule.activity, colorRes)

        mActivityRule.activity.setToolbarColor(color)

        waitForToolbar(70)

        if (isToolbarVisible())
            onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColour(colorRes)))
    }

    private fun waitForToolbar(i: Int) {
        for (waitLoop in 0..i) {
            waitUntilIdle()
            onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()))
            onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()))
            waitUntilIdle()
        }
    }

    private fun isToolbarVisible(): Boolean {
        return UtilsPackage.exists(onView(withId(R.id.toolbar)))
                && UtilsPackage.viewIsDisplayed(R.id.toolbar)
                && UtilsPackage.viewIsDisplayed(R.id.toolbar_container)
    }


}