package com.fsck.k9.pEp.ui.navigationdrawer

import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddAccountsDrawerLayoutTest {

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    private lateinit var testUtils: TestUtils
    private lateinit var uiDevice: UiDevice

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun addAccounts() {
        grantPermissions()
        passWelcomeScreen()
        addAccount(BuildConfig.PEP_TEST_1_EMAIL_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "1")
        Thread.sleep(2000)
        clickAddAccount()
        addAccount(BuildConfig.PEP_TEST_2_EMAIL_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "2")
        clickAddAccount()
        addAccount(BuildConfig.PEP_TEST_3_EMAIL_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "3")
    }

    private fun passWelcomeScreen() {
        uiDevice.waitForIdle()
        onView(withId(R.id.skip)).check(matches(isDisplayed())).perform(click())
        uiDevice.waitForIdle()
    }

    private fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().context.packageName
                            + " android.permission.WRITE_EXTERNAL_STORAGE")
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().context.packageName
                            + " android.permission.READ_CONTACTS")
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().context.packageName
                            + " android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
            K9.setShallRequestPermissions(false)

            Thread {
                val prefs = Preferences.getPreferences(InstrumentationRegistry.getInstrumentation().context)
                val editor = prefs.storage.edit()
                K9.save(editor)
                editor.commit()
            }.start()
        }
    }

    private fun addAccount(emailAddress: String, password: String, accountname: String) {
        uiDevice.waitForIdle()
        onView(withId(R.id.account_email)).check(matches(isDisplayed())).perform(ViewActions.typeText(emailAddress))
        onView(withId(R.id.account_password)).check(matches(isDisplayed())).perform(ViewActions.typeText(password))
        Espresso.closeSoftKeyboard()
        uiDevice.waitForIdle()
        onView(withId(R.id.next)).check(matches(isDisplayed())).perform(click())
        uiDevice.waitForIdle()
        onView(withId(R.id.account_name)).check(matches(isDisplayed())).perform(ViewActions.typeText(accountname))
        Espresso.closeSoftKeyboard()
        uiDevice.waitForIdle()
        onView(withId(R.id.done)).check(matches(isDisplayed())).perform(click())
    }

    private fun clickAddAccount() {
        uiDevice.waitForIdle()
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.add_account_container)).perform(click())
        uiDevice.waitForIdle()
    }

}