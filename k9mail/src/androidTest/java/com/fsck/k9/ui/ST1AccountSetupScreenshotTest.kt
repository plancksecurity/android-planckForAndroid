package com.fsck.k9.ui

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.planck.ui.activities.UtilsPackage
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ST1AccountSetupScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    )


    companion object {
        private val millis = System.currentTimeMillis()
        private val BOT_1_NAME = millis.toString()
        private val BOT_2_NAME = (millis + 1).toString()
        private val BOT_3_NAME = (millis + 2).toString()
    }

    @Test
    @Ignore("normally we can see all screens just by using the manual setup")
    fun step1_automaticAccountSetup() {
        setTestSet("A")
        accountSetup(true)
    }

    @Test
    fun step2_manualAccountSetup() {
        setTestSet("B")
        accountSetup(false)
    }

    @Test
    @Ignore
    fun importAccountSetup() {
        setTestSet("L")
        openFirstScreen()
        click(R.id.skip)
        sleep(500)
        runBlocking { waitForIdle() }
        permissions()

        runBlocking { waitForIdle() }
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        getScreenShotCurrentActivity("import account menu")
        sleep(500)

        testUtils.externalAppRespondWithFile(R.raw.stubaccount)

        click(getString(R.string.settings_import))
        sleep(500)

        getScreenShotCurrentActivity("import account step 1")
        click(getString(R.string.okay_action))
        sleep(500)

        getScreenShotCurrentActivity("import account step 2")
        click(getString(R.string.okay_action))
        sleep(2000)

        getScreenShotCurrentActivity("import account step 3")
        addTextTo(R.id.incoming_server_password, BuildConfig.PLANCK_TEST_EMAIL_PASSWORD)
        getScreenShotCurrentActivity("import account step 3 filled")
    }

    @Test
    fun step3_addMessagesToAccount() {
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
        if (UtilsPackage.viewIsDisplayed(R.id.action_continue)) {
            getScreenShotCurrentActivity("permissions")
            sleep(1000)
            click(R.id.action_continue)
            allowPermissions()
        }
    }

    private fun accountSetup(automaticLogin: Boolean) {
        openFirstScreen()
        permissions()
        getScreenShotCurrentActivity("select auth method")
        click(R.id.other_method_sign_in_button)
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
        addTextTo(R.id.account_email, BuildConfig.PLANCK_TEST_EMAIL_ADDRESS)
        addTextTo(R.id.account_password, BuildConfig.PLANCK_TEST_EMAIL_PASSWORD)
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
        addTextTo(R.id.account_email, BuildConfig.PLANCK_TEST_EMAIL_ADDRESS)
        addTextTo(R.id.account_password, BuildConfig.PLANCK_TEST_EMAIL_PASSWORD)
        closeKeyboardWithDelay()
        getScreenShotAccountSetup("with values")
        click(R.id.manual_setup)
        sleep(1500)

        // setup income
        getScreenShotAccountSetup("without values")
        setTextTo(R.id.account_server, BuildConfig.PLANCK_TEST_EMAIL_SERVER)
        closeKeyboardWithDelay()
        getScreenShotAccountSetup("with values")
        click(R.id.next)
        sleep(1500)

        // setup outcome
        getScreenShotAccountSetup("without values")
        setTextTo(R.id.account_server, BuildConfig.PLANCK_TEST_EMAIL_SERVER)
        closeKeyboardWithDelay()
        getScreenShotAccountSetup("with values")
        click(R.id.next)
        sleep(1500)

        //getScreenShotAccountSetup("") // only for endUser
        //click(R.id.next)
        //sleep(1500)

        getScreenShotCurrentActivity("without values")
        addTextTo(R.id.account_name, "John Smith")
        closeKeyboardWithDelay()
        getScreenShotCurrentActivity("with values")
        click(R.id.done)
    }


}