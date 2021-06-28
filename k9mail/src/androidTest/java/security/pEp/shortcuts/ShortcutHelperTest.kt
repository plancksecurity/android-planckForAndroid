package security.pEp.shortcuts

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import com.fsck.k9.pEp.ui.activities.UtilsPackage
import com.fsck.k9.pEp.ui.navigationdrawer.SetupDevTestAccounts
import junit.framework.TestCase
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class ShortcutHelperTest: SetupDevTestAccounts() {
    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    private val preferences: Preferences =
        Preferences.getPreferences(ApplicationProvider.getApplicationContext())

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun stage1_clearAccounts() {
        uiDevice.waitForIdle()
        clearAccounts()
    }

    @Test
    fun stage2_setupAccount() {
        uiDevice.waitForIdle()
        setupAccounts()
    }

    @Test
    fun stage3_onDefaultAccountDeletionThenShortcutChangesToNewDefaultAccount() {
        val firstEmail = preferences.accounts.first().email
        assertCurrentShortcutEmail(firstEmail)


        testUtils.selectFromMenu(R.string.action_settings)
        testUtils.removeAccountAtPosition(0)


        Assert.assertNotEquals(firstEmail, preferences.accounts.first().email)
        assertCurrentShortcutEmail(preferences.accounts.first().email)
    }

    @Test
    fun stage4_onChangingDefaultAccountThenShortcutChangesToNewDefaultAccount() {
        TestCase.assertEquals(ANDROID_DEV_TEST_2_ADDRESS, preferences.accounts.first().email)
        assertCurrentShortcutEmail(preferences.accounts.first().email)


        setLastAccountAsDefaultInSettings()


        assertCurrentShortcutEmail(preferences.accounts.last().email)
    }

    private fun setLastAccountAsDefaultInSettings() {
        testUtils.selectFromMenu(R.string.action_settings)
        Espresso.onView(
            UtilsPackage.withRecyclerView(R.id.accounts_list)
                .atPosition(preferences.accounts.size - 1)
        )
            .perform(ViewActions.scrollTo(), ViewActions.click())
        testUtils.selectFromScreen(R.string.general_settings_title)
        TestUtils.waitForIdle()
        Espresso.onView(ViewMatchers.withText(R.string.account_settings_default_label))
            .perform(ViewActions.click())
    }

    private fun assertCurrentShortcutEmail(expectedEmail: String) {

        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstShortcutInfo = ShortcutManagerCompat.getDynamicShortcuts(context).first()
        val currentUuid = firstShortcutInfo.intent.extras?.getString(MessageCompose.EXTRA_ACCOUNT)
        val currentEmail = preferences.getAccount(currentUuid).email

        TestCase.assertEquals(expectedEmail, currentEmail)
    }
}