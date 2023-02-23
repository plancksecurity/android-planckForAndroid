/*package com.fsck.k9.pEp.ui.restrictions

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import security.pEp.mdm.ManageableSettingMdmEntry


@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore("Only to be run via ./gradlew testRestrictions")
class AppRestrictionsTest : BaseDeviceAdminTest() {

    private var forcedAppConfig = false

    @After
    fun afterRestrictionTest() {
        if (forcedAppConfig)
            openEnforcerSplitScreen(
                    key = "pep_disable_privacy_protection",
                    value = pEpPrivacyJson(true),
                    generic = false
            )
        sleep(3000)
    }

    private fun pEpPrivacyJson(enabled: Boolean) =
            Json.encodeToString(ManageableSettingMdmEntry(locked = true, value = enabled))

    @Test
    fun automaticStartUp() {
        passWelcomeScreen()
        allowPermissions()
        addFirstAccount()
    }

    @Test
    fun sendMessageToSelf() {
        waitListView()
        getMessageListSize()
        sendNewMessageToSelf()
        waitNewMessage()
    }

    @Test
    fun messageListAppRestrictions() {
        waitListView()

        openEnforcerSplitScreen(
                key = "pep_disable_privacy_protection",
                value = pEpPrivacyJson(false),
                generic = false
        )

        onData(anything())
                .inAdapterView(withId(R.id.message_list))
                .atPosition(0)
                .onChildView(allOf(withId(R.id.privacyBadge), isDescendantOfA(withId(R.id.message_read_container))))
                .check(matches(not(isDisplayed())))
    }

    @Test
    fun messageViewAppRestrictions() {
        waitListView()
        clickListItem(R.id.message_list, 0)

        waitMessageView()

        openEnforcerSplitScreen(
                key = "pep_disable_privacy_protection",
                value = pEpPrivacyJson(false),
                generic = false
        )

        onView(withId(R.id.actionbar_message_view)).check(matches(not(isDisplayed())))
    }

    @Test
    fun messageComposeAppRestrictions() {
        waitListView()
        clickListItem(R.id.message_list, 0)

        waitMessageView()

        onView(withId(R.id.openCloseButton)).perform(click())

        openEnforcerSplitScreen(
                key = "pep_disable_privacy_protection",
                value = pEpPrivacyJson(false),
                generic = false
        )

        onView(withId(R.id.actionbar_message_view)).check(matches(not(isDisplayed())))
    }

    @Test
    fun accountSettingsAppRestrictions() {
        waitListView()

        onView(withContentDescription(R.string.navigation_drawer_open)).perform(click())

        click(R.id.menu_header)
        click(R.id.configure_account_container)

        runBlocking { waitForIdle() }
        clickListItem(R.id.accounts_list, 0)

        sleep(500)
        clickSetting(R.string.privacy_preferences)

        openEnforcerSplitScreen(
                key = "pep_disable_privacy_protection",
                value = pEpPrivacyJson(false),
                generic = false
        )

        runBlocking { waitForIdle() }
        sleep(2000)

        checkSwitchPreferenceStatus(R.string.pep_enable_privacy_protection)
    }

    @Test
    fun receiveMalformedJson(){
        waitListView()

        openEnforcerSplitScreen(
                key = "pep_disable_privacy_protection",
                value = "{hello:hello, john:john}",
                generic = false
        )

        runBlocking { waitForIdle() }
        sleep(2000)
    }

    private fun passWelcomeScreen() {
        click(R.id.skip)
        click(R.id.action_continue)
        runBlocking { waitForIdle() }
    }

    private fun addFirstAccount() {
        click()
        addTextTo(R.id.account_email, BuildConfig.PEP_TEST_EMAIL_ADDRESS)
        addTextTo(R.id.account_password, BuildConfig.PEP_TEST_EMAIL_PASSWORD)
        closeKeyboardWithDelay()

        click(R.id.next)
        sleep(2000)

        addTextTo(R.id.account_name, "account name")
        closeKeyboardWithDelay()

        click(R.id.done)
    }

    private fun openEnforcerSplitScreen(key: String, value: String, generic: Boolean) =
        runBlocking(Dispatchers.Main) {
            val intent: Intent =
                context.packageManager.getLaunchIntentForPackage(ENFORCER_PACKAGE_NAME)
                    ?: return@runBlocking
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.putExtra("inPIP", true)
            intent.putExtra("generic", generic)
            intent.putExtra("key", key)
            intent.putExtra("value", value)
            val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry
                .getInstance().getActivitiesInStage(Stage.RESUMED)
            Iterables.getOnlyElement(activities)?.startActivity(intent)
            forcedAppConfig = true
        }

}
*/