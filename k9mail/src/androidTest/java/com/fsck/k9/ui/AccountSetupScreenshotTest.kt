package com.fsck.k9.ui

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AccountSetupScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    var permissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)


    companion object {
        const val BOT_1_NAME = "account1"
        const val BOT_2_NAME = "account2"
        const val BOT_3_NAME = "account2"
    }

    @Test
    fun automaticAccountSetup() {
        setTestSet("A")
        accountSetup(true)
    }

    @Test
    fun manualAccountSetup() {
        setTestSet("B")
        accountSetup(false)
    }

    @Test
    fun importAccountSetup() {
        setTestSet("L")
        openFirstScreen()
        click(R.id.skip)
        sleep(500)
        permissions()

        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("import account menu")
        sleep(500)

        startFileManagerStub("stubAccount", "k9s")
        click(getString(R.string.settings_import))
        sleep(500)

        getScreenShotCurrentActivity("import account step 1")
        click(getString(R.string.okay_action))
        sleep(500)

        getScreenShotCurrentActivity("import account step 2")
        click(getString(R.string.okay_action))
        sleep(2000)

        getScreenShotCurrentActivity("import account step 3")
        addTextTo(R.id.incoming_server_password, BuildConfig.PEP_TEST_EMAIL_PASSWORD)
        getScreenShotCurrentActivity("import account step 3 filled")
    }

    @Test
    fun addMessagesToAccount() {
        openFirstScreen()
        waitListView()
        getMessageListSize()

        sendNewMessageToSelf()
        waitNewMessage()
        getMessageListSize()

        replyToSelfMessage()
        getMessageListSize()

        sendMessageToBot(BOT_1_NAME)
        waitNewMessage()
        getMessageListSize()

        sendMessageToBot(BOT_2_NAME)
        waitNewMessage()
        getMessageListSize()

        sendMessageToBot(BOT_3_NAME)
        waitNewMessage()
    }

    private fun permissions() {
        getScreenShotCurrentActivity("permissions")
        sleep(1000)
        click(R.id.action_continue)
        allowPermissions()
    }

    private fun accountSetup(automaticLogin: Boolean) {
        openFirstScreen()
        passWelcomeScreen()
        permissions()
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


}