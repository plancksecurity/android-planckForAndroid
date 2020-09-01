package com.fsck.k9.ui

import android.content.Intent
import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import security.pEp.ui.intro.WelcomeMessage
import security.pEp.ui.permissions.PermissionsActivity


@LargeTest
@RunWith(AndroidJUnit4::class)
class AppScreenshotsTest : BaseScreenshotTest() {

    @get:Rule
    var splashRule =
            ActivityTestRule(SplashActivity::class.java, false, false)

    /**
     * NEEDS PEP_TEST_EMAIL_ADDRESS and PEP_TEST_EMAIL_PASSWORD system variables
     */
    @Test
    fun allTest() {
        openFirstScreen()
        passWelcomeScreen()
        when {
            showPermissionsScreen() -> acceptPermissions()
            else -> addFirstAccountAutomatic()
        }
    }

    private fun openFirstScreen() {
        val intent = Intent()
        splashRule.launchActivity(intent)
        getScreenShotCurrentActivity("splash")
        runBlocking { waitForIdle() }
    }

    private fun passWelcomeScreen() {
        getScreenShotCurrentActivity("welcome screen")
        click(R.id.next)
        getScreenShotCurrentActivity(" first click")
        click(R.id.next)
        getScreenShotCurrentActivity(" second click")
        click(R.id.next)
        getScreenShotCurrentActivity(" third click")
        click(R.id.done)
        runBlocking { waitForIdle() }
    }

    private fun swipeBackInWelcomeMessage() {
        val welcomeMessage = getCurrentActivity()
        if (welcomeMessage is WelcomeMessage) {
            val view = withId(R.id.view_pager)
            swipeRight(view)
            getScreenShotCurrentActivity(" swipe right once")
            swipeRight(view)
            getScreenShotCurrentActivity(" swipe right twice")
            swipeRight(view)
            getScreenShotCurrentActivity(" swipe right thrice")
            click(R.id.skip)
        } else {
            throw Exception("Wrong activity on screen")
        }
    }

    private fun showPermissionsScreen(): Boolean {
        getScreenShotCurrentActivity("show permissions")
        if (getCurrentActivity() is PermissionsActivity)
            click(R.id.action_continue)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun acceptPermissions() {
        getScreenShotCurrentActivity("accept permissions")
        TODO("Not sure if possible, since no screenshots if there is no permissions")
        // addFirstAccountAuto()
    }

    private fun addFirstAccountAutomatic() {
        getScreenShotCurrentActivity("without values")
        addTextTo(R.id.account_email, BuildConfig.PEP_TEST_EMAIL_ADDRESS)
        addTextTo(R.id.account_password, BuildConfig.PEP_TEST_EMAIL_PASSWORD)
        getScreenShotCurrentActivity("with values")
        Espresso.closeSoftKeyboard()
        sleep(2000)
        click(R.id.next)
        sleep(2000)
        getScreenShotCurrentActivity("without values")
        addTextTo(R.id.account_name, "account name")
        getScreenShotCurrentActivity("with values")
        click(R.id.done)
    }


}