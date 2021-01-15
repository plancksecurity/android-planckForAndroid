package com.fsck.k9.pEp.ui.navigationdrawer

import android.app.Activity
import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.MessageList
import com.fsck.k9.pEp.ui.activities.TestUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber

open class SetupDevTestAccounts {

    val ANDROID_DEV_TEST_1_ADDRESS = "android_devtest_01@peptest.ch"
    val ANDROID_DEV_TEST_2_ADDRESS = "android_devtest_02@peptest.ch"
    val ANDROID_DEV_TEST_3_ADDRESS = "android_devtest_03@peptest.ch"

    lateinit var uiDevice: UiDevice
    lateinit var testUtils: TestUtils

    fun clearAccounts() {
        val activity = getCurrentActivity()
        Timber.e("class is " + activity!!::class.java)

        if (activity is MessageList) {
            Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
            val settingText = activity.resources?.getString(R.string.action_settings) ?: ""
            onView(withText(settingText)).check(matches(isDisplayed())).perform(click())
            val accountsSize = Preferences.getPreferences(activity).accounts.size
            repeat(accountsSize) {
                testUtils.goBackAndRemoveAccount()
            }
        }
    }

    fun setupAccounts() {
        grantPermissions()
        passWelcomeScreen()
        addAccount(ANDROID_DEV_TEST_1_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "1")
        Thread.sleep(2000)
        clickAddAccountButton()
        addAccount(ANDROID_DEV_TEST_2_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "2")
        clickAddAccountButton()
        addAccount(ANDROID_DEV_TEST_3_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "3")
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

    private fun addAccount(emailAddress: String, password: String, accountName: String) {
        uiDevice.waitForIdle()
        onView(withId(R.id.account_email)).check(matches(isDisplayed())).perform(typeText(emailAddress))
        onView(withId(R.id.account_password)).check(matches(isDisplayed())).perform(typeText(password))
        Espresso.closeSoftKeyboard()
        uiDevice.waitForIdle()
        onView(withId(R.id.next)).check(matches(isDisplayed())).perform(click())

        uiDevice.waitForIdle()
        Thread.sleep(1000)
        onView(withId(R.id.account_name)).check(matches(isDisplayed())).perform(clearText(), typeText(accountName))
        Espresso.closeSoftKeyboard()
        uiDevice.waitForIdle()
        Thread.sleep(1000)
        onView(withId(R.id.done)).check(matches(isDisplayed())).perform(click())
    }

    private fun clickAddAccountButton() {
        uiDevice.waitForIdle()
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.add_account_container)).perform(click())
        uiDevice.waitForIdle()
    }

    private fun getCurrentActivity(): Activity? = runBlocking(Dispatchers.Main) {
        uiDevice.waitForIdle()
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        return@runBlocking Iterables.getOnlyElement(activities)
    }
}