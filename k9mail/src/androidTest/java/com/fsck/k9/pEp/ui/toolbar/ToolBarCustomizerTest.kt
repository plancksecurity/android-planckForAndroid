package com.fsck.k9.pEp.ui.toolbar

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.R
import com.fsck.k9.activity.MessageList
import foundation.pEp.jniadapter.Rating
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ToolBarCustomizerTest {
    @get:Rule
    var mActivityRule = ActivityTestRule(MessageList::class.java)

    @Test
    fun check_if_status_bar_changes_color_by_color_resource() {
        mActivityRule.activity.toolBarCustomizer.setStatusBarPepColor(R.color.white)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val beforeColour = mActivityRule.activity.window.statusBarColor
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        mActivityRule.activity.toolBarCustomizer.setStatusBarPepColor(R.color.green)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        Espresso.onView(withId(android.R.id.statusBarBackground))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        val afterColour = mActivityRule.activity.window.statusBarColor

        Assert.assertEquals(beforeColour, afterColour)
    }

    @Test
    fun check_if_status_bar_changes_color_by_rating() {
        mActivityRule.activity.toolBarCustomizer.setStatusBarPepColor(Rating.pEpRatingReliable)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val beforeColour = mActivityRule.activity.window.statusBarColor
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        mActivityRule.activity.toolBarCustomizer.setStatusBarPepColor(Rating.pEpRatingReliable)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        Espresso.onView(withId(android.R.id.statusBarBackground))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        val afterColour = mActivityRule.activity.window.statusBarColor

        Assert.assertEquals(beforeColour, afterColour)
    }

}