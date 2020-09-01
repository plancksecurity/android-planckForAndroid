package com.fsck.k9.ui

import android.os.Build
import androidx.test.espresso.Espresso
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import kotlinx.coroutines.runBlocking
import org.junit.Test
import security.pEp.ui.permissions.PermissionsActivity

class AccountSetupScreenshotTest : BaseScreenshotTest() {

    /**
     * NEEDS PEP_TEST_EMAIL_ADDRESS and PEP_TEST_EMAIL_PASSWORD system variables
     */

    @Test
    fun automaticAccountSetup() {
        accountSetup(true)
    }

    @Test
    fun manualAccountSetup() {
        accountSetup(false)
    }

    private fun acceptPermissions() {
        getScreenShotCurrentActivity("accept permissions")
        TODO("Not sure if possible, since no screenshots if there is no permissions")
        // addFirstAccountAuto()
    }

    private fun accountSetup(automaticLogin: Boolean) {
        openFirstScreen()
        passWelcomeScreen()
        when {
            showPermissionsScreen() -> acceptPermissions()
            else -> {
                if (automaticLogin) addFirstAccountAutomatic()
                else addFirstAccountManual()
            }
        }
    }

    private fun showPermissionsScreen(): Boolean {
        getScreenShotCurrentActivity("show permissions")
        if (getCurrentActivity() is PermissionsActivity)
            click(R.id.action_continue)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
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

    private fun addFirstAccountManual() {
        // email password
        getScreenShotAccountSetup("without values")
        addTextTo(R.id.account_email, BuildConfig.PEP_TEST_EMAIL_ADDRESS)
        addTextTo(R.id.account_password, BuildConfig.PEP_TEST_EMAIL_PASSWORD)
        getScreenShotAccountSetup("with values")
        Espresso.closeSoftKeyboard()
        sleep(1000)
        click(R.id.manual_setup)
        sleep(1000)

        // setup income
        getScreenShotAccountSetup("without values")
        setTextTo(R.id.account_server, BuildConfig.PEP_TEST_EMAIL_SERVER)
        getScreenShotAccountSetup("with values")
        Espresso.closeSoftKeyboard()
        sleep(1000)
        click(R.id.next)
        sleep(1000)

        // setup outcome
        getScreenShotAccountSetup("without values")
        setTextTo(R.id.account_server, BuildConfig.PEP_TEST_EMAIL_SERVER)
        getScreenShotAccountSetup("with values")
        Espresso.closeSoftKeyboard()
        sleep(1000)
        click(R.id.next)
        sleep(1000)

        getScreenShotAccountSetup("")
        click(R.id.next)
        sleep(1000)

        getScreenShotCurrentActivity("without values")
        addTextTo(R.id.account_name, "John Smith")
        getScreenShotCurrentActivity("with values")
        click(R.id.done)
    }

}