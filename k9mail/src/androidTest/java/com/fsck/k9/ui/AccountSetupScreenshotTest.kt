package com.fsck.k9.ui

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 *  This 2 test need to be run in an clean app, before run it clean the app data
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AccountSetupScreenshotTest : BaseScreenshotTest() {


    /**
     * NEEDS PEP_TEST_EMAIL_ADDRESS and PEP_TEST_EMAIL_PASSWORD system variables
     */

    @Test
    fun automaticAccountSetup() {
        setTestSet("A")
        grantPermissions()
        accountSetup(true)
    }

    @Test
    fun manualAccountSetup() {
        setTestSet("B")
        grantPermissions()
        accountSetup(false)
    }

    private fun accountSetup(automaticLogin: Boolean) {
        openFirstScreen()
        passWelcomeScreen()
        if (automaticLogin) addFirstAccountAutomatic()
        else addFirstAccountManual()
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

    private fun addFirstAccountAutomatic() {
        getScreenShotCurrentActivity("without values")
        addTextTo(R.id.account_email, BuildConfig.PEP_TEST_EMAIL_ADDRESS)
        addTextTo(R.id.account_password, BuildConfig.PEP_TEST_EMAIL_PASSWORD)
        closeKeyboardWithDelay()
        getScreenShotCurrentActivity("with values")
        sleep(2000)

        click(R.id.next)
        sleep(2000)

        getScreenShotCurrentActivity("without values")
        addTextTo(R.id.account_name, "account name")
        closeKeyboardWithDelay()
        getScreenShotCurrentActivity("with values")

        click(R.id.done)
    }

    private fun addFirstAccountManual() {
        // email password
        getScreenShotAccountSetup("without values")
        addTextTo(R.id.account_email, BuildConfig.PEP_TEST_EMAIL_ADDRESS)
        addTextTo(R.id.account_password, BuildConfig.PEP_TEST_EMAIL_PASSWORD)
        closeKeyboardWithDelay()
        getScreenShotAccountSetup("with values")
        click(R.id.manual_setup)
        sleep(1500)

        // setup income
        getScreenShotAccountSetup("without values")
        setTextTo(R.id.account_server, BuildConfig.PEP_TEST_EMAIL_SERVER)
        closeKeyboardWithDelay()
        getScreenShotAccountSetup("with values")
        click(R.id.next)
        sleep(1500)

        // setup outcome
        getScreenShotAccountSetup("without values")
        setTextTo(R.id.account_server, BuildConfig.PEP_TEST_EMAIL_SERVER)
        closeKeyboardWithDelay()
        getScreenShotAccountSetup("with values")
        click(R.id.next)
        sleep(1500)

        getScreenShotAccountSetup("")
        click(R.id.next)
        sleep(1500)

        getScreenShotCurrentActivity("without values")
        addTextTo(R.id.account_name, "John Smith")
        closeKeyboardWithDelay()
        getScreenShotCurrentActivity("with values")
        click(R.id.done)
    }

    private fun closeKeyboardWithDelay() {
        Espresso.closeSoftKeyboard()
        sleep(1000)
    }
}