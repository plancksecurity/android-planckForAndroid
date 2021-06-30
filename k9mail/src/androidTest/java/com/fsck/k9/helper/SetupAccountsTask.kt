package com.fsck.k9.helper

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.GrantPermissionRule
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.common.BaseTest
import com.fsck.k9.pEp.ui.activities.UtilsPackage
import org.junit.Rule
import org.junit.Test

@Suppress("SameParameterValue")
class SetupAccountsTask : BaseTest() {

    @get:Rule
    var permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        )

    private val ANDROID_DEV_TEST_1_ADDRESS = "android_devtest_01@peptest.ch"

    @Test
    fun setupAccounts() {
        device.waitForIdle()
        passWelcomeScreen()
        if (UtilsPackage.viewIsDisplayed(R.id.action_continue)) {
            onView(withId(R.id.action_continue)).perform(click())
        }
        allowPermissions()
        device.waitForIdle()
        addAccount(ANDROID_DEV_TEST_1_ADDRESS, BuildConfig.PEP_TEST_EMAIL_PASSWORD, "account")
    }

    private fun passWelcomeScreen() {
        device.waitForIdle()
        onView(withId(R.id.skip)).perform(click())
        device.waitForIdle()
    }

    private fun addAccount(emailAddress: String, password: String, accountName: String) {
        device.waitForIdle()
        onView(withId(R.id.account_email)).perform(typeText(emailAddress))
        onView(withId(R.id.account_password)).perform(typeText(password))
        Espresso.closeSoftKeyboard()
        device.waitForIdle()
        onView(withId(R.id.next)).perform(click())

        device.waitForIdle()
        onView(withId(R.id.account_name)).perform(clearText(), typeText(accountName))
        device.waitForIdle()
        Espresso.closeSoftKeyboard()
        device.waitForIdle()
        onView(withId(R.id.done)).perform(click())
    }

}