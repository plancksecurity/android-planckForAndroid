package com.fsck.k9.ui

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import security.pEp.ui.intro.WelcomeMessage


@LargeTest
@RunWith(AndroidJUnit4::class)
class AppScreenshotsTest : BaseScreenshotTest() {

    @get:Rule
    var splashRule =
            ActivityTestRule(SplashActivity::class.java, false, false)


    @Test
    fun openFirstScreen() {
        val intent = Intent()
        splashRule.launchActivity(intent)

        getScreenShot(splashRule.activity::class.java.simpleName, "splash")

        Thread.sleep(2000)

        getScreenShotCurrentActivity("after splash")

    }

    @Test
    fun passWelcomeScreen() {
        val intent = Intent()
        splashRule.launchActivity(intent)

        onView(withId(R.id.next)).perform(click())
        getScreenShotCurrentActivity(" first click")
        onView(withId(R.id.next)).perform(click())
        getScreenShotCurrentActivity(" second click")
        onView(withId(R.id.next)).perform(click())
        getScreenShotCurrentActivity(" third click")
    }

    private fun swipeBackInWelcomeMessage() {
        val welcomeMessage = getCurrentActivity()
        if (welcomeMessage is WelcomeMessage) {
            val view = withId(R.id.view_pager)
            onView(view).perform(swipeRight())
            getScreenShotCurrentActivity(" swipe right once")
            onView(view).perform(swipeRight())
            getScreenShotCurrentActivity(" swipe right twice")
            onView(view).perform(swipeRight())
            getScreenShotCurrentActivity(" swipe right thrice")
            onView(withId(R.id.skip)).perform(click())
            getScreenShotCurrentActivity(" click skip")
        } else {
            throw Exception("Wrong activity on screen")
        }
    }

    private fun acceptPermissions(){

    }

    private fun addFirstAccountAuto(){

    }
}