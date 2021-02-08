package com.fsck.k9.helper

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import com.fsck.k9.pEp.ui.activities.UtilsPackage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

@Suppress("SameParameterValue")
class SetupAccountsTask {

    private val ANDROID_DEV_TEST_1_ADDRESS = "android_devtest_01@peptest.ch"

    lateinit var uiDevice: UiDevice
    lateinit var testUtils: TestUtils

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun setupAccounts() {
        uiDevice.waitForIdle()
        passWelcomeScreen()
        Thread.sleep(1000)
        allowPermissions()
        Thread.sleep(1000)
        if (UtilsPackage.viewIsDisplayed(R.id.action_continue)) {
            onView(withId(R.id.action_continue)).perform(click())
        }
        addAccount(ANDROID_DEV_TEST_1_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "1")
    }

    private fun passWelcomeScreen() {
        uiDevice.waitForIdle()
        onView(withId(R.id.skip)).perform(click())
        uiDevice.waitForIdle()
    }

    private fun addAccount(emailAddress: String, password: String, accountName: String) {
        uiDevice.waitForIdle()
        onView(withId(R.id.account_email)).perform(typeText(emailAddress))
        onView(withId(R.id.account_password)).perform(typeText(password))
        Espresso.closeSoftKeyboard()
        uiDevice.waitForIdle()
        onView(withId(R.id.next)).perform(click())

        uiDevice.waitForIdle()
        Thread.sleep(1000)
        onView(withId(R.id.account_name)).perform(clearText(), typeText(accountName))
        Espresso.closeSoftKeyboard()
        uiDevice.waitForIdle()
        Thread.sleep(1000)
        onView(withId(R.id.done)).perform(click())
    }

    private fun allowPermissions() {
        uiDevice.waitForIdle()
        try {
            val popUpMessage = By.clazz("android.widget.Button")
            var buttonExists = true
            Timber.e("while")
            while (buttonExists) {
                buttonExists = false
                uiDevice.findObjects(popUpMessage).forEach { obj ->
                    if (obj.resourceName != null && obj.resourceName == "com.android.permissioncontroller:id/permission_allow_button") {
                        buttonExists = true
                        obj.click()
                    }
                }
            }
        } catch (ex: Exception) {
            Timber.e("Cannot allow permissions")
        }
        do {
            allowPermissions(2)
            allowPermissions(1)
        } while (!UtilsPackage.viewIsDisplayed(R.id.action_continue) && !UtilsPackage.viewIsDisplayed(R.id.account_email))
    }

    private fun allowPermissions(index: Int) {
        while (true) {
            try {
                uiDevice.waitForIdle()
                val allowPermissions = uiDevice.findObject(UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .index(index))
                if (allowPermissions.exists()) {
                    allowPermissions.click()
                    uiDevice.waitForIdle()
                } else {
                    Timber.e("There is no permissions dialog to interact with ")
                    return
                }
            } catch (ignoredException: Exception) {
                Timber.e(ignoredException, "Failed trying to allow permission")
            }
        }
    }

}