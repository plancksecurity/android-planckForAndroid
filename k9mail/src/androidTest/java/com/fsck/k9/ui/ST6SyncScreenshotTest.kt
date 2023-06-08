package com.fsck.k9.ui

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.R
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class ST6SyncScreenshotTest : BaseScreenshotTest() {

    @Test
    fun syncTest() {
        openFirstScreen()
        setTestSet("K")
        waitSyncDialog()
        clickNext()
        showLongTrustwords()
        showLanguageList()
        selectLanguage()
        acceptSync()
        waitSyncFinish()
    }

    private fun waitSyncDialog() {
        while (getCurrentActivity()?.localClassName?.contains("ImportWizardFrompEp") == false) {
            sleep(2000)
        }
        getScreenShotCurrentActivity("first screen")
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