package com.fsck.k9.pEp.ui.message_compose

import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.UtilsPackage.viewIsDisplayed
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class PEpFabMenuTest {

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)
    var device: UiDevice? = null

    @Before
    fun startUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    }

    @Test
    fun viewInitDisplayExpected() {

        waitListView()

        clickListItem(1)

        onView(withId(R.id.openCloseButton)).check(matches(isDisplayed()))
    }

    @Test
    fun shouldOpenMenu() {
        waitListView()

        clickListItem(1)

        textsAreNotVisible()
        fabsAreNotVisible()

        longClickMainButton()

        textsAreVisible()
        fabsAreVisible()
    }

    @Test
    fun shouldOpenCloseMenu() {
        waitListView()

        clickListItem(1)

        textsAreNotVisible()
        fabsAreNotVisible()

        longClickMainButton()

        textsAreVisible()
        fabsAreVisible()

        longClickMainButton()

        textsAreNotVisible()
        fabsAreNotVisible()
    }

    private fun waitListView() {
        while (!viewIsDisplayed(R.id.fab_button_compose_message));
    }

    private fun clickListItem(position: Int) {
        onData(anything())
                .inAdapterView(withId(R.id.message_list))
                .atPosition(position)
                .perform(click())
        device?.waitForIdle()
    }

    private fun textsAreVisible() {
        onView(withId(R.id.textviewReply)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.textviewReplyAll)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.textviewForward)).check(matches(isCompletelyDisplayed()))
    }

    private fun textsAreNotVisible() {
        onView(withId(R.id.textviewReply)).check(matches(not(isCompletelyDisplayed())))
        onView(withId(R.id.textviewReplyAll)).check(matches(not(isCompletelyDisplayed())))
        onView(withId(R.id.textviewForward)).check(matches(not(isCompletelyDisplayed())))
    }

    private fun fabsAreVisible() {
        while (!viewIsDisplayed(R.id.fabReply))
            device?.waitForIdle()

        onView(withId(R.id.openCloseButton)).check(matches(isDisplayed()))
        onView(withId(R.id.fabReply)).check(matches(isDisplayed()))
        onView(withId(R.id.fabReplyAll)).check(matches(isDisplayed()))
        onView(withId(R.id.fabForward)).check(matches(isDisplayed()))
    }

    private fun fabsAreNotVisible() {
        onView(withId(R.id.fabReply)).check(matches(not(isDisplayed())))
        onView(withId(R.id.fabReplyAll)).check(matches(not(isDisplayed())))
        onView(withId(R.id.fabForward)).check(matches(not(isDisplayed())))
    }

    private fun longClickMainButton() {
        onView(withId(R.id.openCloseButton)).perform(longClick())

        onView(isRoot()).perform(waitFor(1000))
    }

}

private fun waitFor(millis: Long): ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "Wait for $millis milliseconds."
        }

        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(millis)
        }
    }
}
