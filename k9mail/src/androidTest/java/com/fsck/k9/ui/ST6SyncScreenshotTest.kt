package com.fsck.k9.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.K9
import com.fsck.k9.R
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class ST6SyncScreenshotTest : BaseScreenshotTest() {

    @Test
    fun syncTest() {
        openFirstScreen()
        setTestSet("K")
        restartSync()
        goToSettings()
        triggerSync()
        waitOtherDevice()
        clickNext()
        showLongTrustwords()
        showLanguageList()
        selectLanguage()
        acceptSync()
        waitSyncFinish()
    }

    private fun restartSync() {
        ApplicationProvider.getApplicationContext<K9>().setPlanckSyncEnabled(false)
        ApplicationProvider.getApplicationContext<K9>().setPlanckSyncEnabled(true)
    }

    private fun goToSettings() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        click(getString(R.string.action_settings))
    }

    private fun triggerSync() {
        clickSetting(R.string.privacy_preferences)
        expandSetting(R.string.sync_title)
        testUtils.pressOKButtonInDialog()
    }

    private fun waitOtherDevice() {
        if (viewIsDisplayed(R.id.waiting_for_sync)) {
            getScreenShotCurrentActivity("awaiting the other device for sync")
        }
        while (viewIsDisplayed(R.id.waiting_for_sync)) {
            sleep(2000)
        }
        getScreenShotCurrentActivity("awaiting user to start sync process")
    }

    private fun acceptSync() {
        click(R.id.afirmativeActionButton)
        sleep(500)
        getScreenShotCurrentActivity("sync accepted")
    }

    private fun selectLanguage() {
        click("Deutsch")
        sleep(500)
        getScreenShotCurrentActivity("language changed")
    }

    private fun showLanguageList() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        sleep(500)
        getScreenShotCurrentActivity("language list")
    }

    private fun showLongTrustwords() {
        longClick(R.id.trustwords)
        sleep(500)
        getScreenShotCurrentActivity("long trustwords")
    }

    private fun clickNext() {
        click(R.id.afirmativeActionButton)
        sleep(500)
        getScreenShotCurrentActivity("second screen")
    }

    private fun waitSyncFinish() {
        sleep(2000)
        getScreenShotCurrentActivity("sync loading")
        while (viewIsDisplayed(R.id.loading)) {
            sleep(500)
        }
        getScreenShotCurrentActivity("sync done")
    }

}