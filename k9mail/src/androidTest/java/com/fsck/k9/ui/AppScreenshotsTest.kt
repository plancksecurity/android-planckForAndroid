package com.fsck.k9.ui

import android.content.Intent
import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import security.pEp.ui.intro.WelcomeMessage
import security.pEp.ui.permissions.PermissionsActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class AppScreenshotsTest : BaseScreenshotTest() {

    @Test
    fun afterAccountSetup() {
        openFirstScreen()
        openSingleInboxMessage()
    }

    private fun openSingleInboxMessage() {
        getScreenShotMessageList("inbox list")
        sleep(2000)
        clickListItem(R.id.message_list, 0)
        getScreenShotMessageList("inbox item 0")
        click(R.id.message_more_options)
        getScreenShotMessageList("click more options")
        pressBack()
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        getScreenShotMessageList("click menu options")
    }

}