package com.fsck.k9.activity.setup

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.fsck.k9.BuildConfig
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.pEp.AccountRemover
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File

@RunWith(AndroidJUnit4::class)
@LargeTest
class AccountSetupBasicsTest {
    private var currentUuid: String? = null

    @get:Rule
    var mActivityRule = ActivityTestRule(AccountSetupBasics::class.java)

    @Before
    @Throws(Exception::class)
    fun setUp() {
    }

    @Test
    fun testManualSetup() {
        startActivity()
        setupEmailAndPassword()
        onView(withId(R.id.manual_setup)).perform(click())
        doWait()
        setupIncomingSettings()
        doWait()
        setupOutgoingSettings()
        doWait()
        accountSetupPEpOptions()
        doWait()
        accountSetupName()
    }

    @Test
    fun testAutomaticSetup() {
        startActivity()
        setupEmailAndPassword()
        onView(withId(R.id.next)).perform(click())
        doWait()
        accountSetupName()
    }

    @Test
    fun deletedAccountFilesAreNotLeftInAutomaticSetup() {
        startActivity()
        setupEmailAndPassword()
        for (i in 0..2) {
            onView(withId(R.id.next)).perform(click())
            doWait(5000L)
            checkWeArrivedToSetupNames()
            val activity = activityInstance
            val setupNames = activity as AccountSetupNames?
            currentUuid = setupNames!!.intent.extras!!.getString("account")
            Timber.d("==== account uuid is $currentUuid")
            checkAccountIsCreated()
            goBackFromNamesToBasics()
            checkAccountDbIsDeleted()

        }
    }

    @Test
    fun deletedAccountFilesAreNotLeftInManualSetup() {
        startActivity()
        setupEmailAndPassword()
        onView(withText(R.string.account_setup_basics_manual_setup_action)).perform(click())
        doWait(3000)
        checkWeArrivedToIncomingSettings()
        changeImapServerText()
        onView(withId(R.id.next)).perform(click())
        doWait(4000)
    }

    private fun checkWeArrivedToIncomingSettings() {
        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches(withText(R.string.account_setup_incoming_title)))
    }

    private fun changeImapServerText() {
        onView(withId(R.id.account_server)).perform(replaceText("peptest.ch"))
    }

    private fun performAutomaticSetupNormally() {
        startActivity()
        setupEmailAndPassword()
        onView(withId(R.id.next)).perform(click())
        doWait(5000L)
        checkWeArrivedToSetupNames()
        val activity = activityInstance
        val setupNames = activity as AccountSetupNames?
        currentUuid = setupNames!!.intent.extras!!.getString("account")
        Timber.d("==== account uuid is $currentUuid")
    }

    private fun checkAccountIsCreated() {
        val route = "/data/user/0/security.pEp.debug/databases/$currentUuid.db"
        val file = File(route)
        Timber.d("==== db path: $route")
        assertTrue(file.exists())
        val account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).getAccount(currentUuid)
        assertNotNull(account)
    }

    private fun checkAccountDbIsDeleted() {
        val route = "/data/user/0/security.pEp.debug/databases/$currentUuid.db"
        val file = File(route)
        Timber.d("==== db path: $route")
        assertFalse(file.exists())
    }

    @Test
    fun accountIsSetUpInAutomaticMode() {
        // lets check relevant things: account is in preferences, we have db in storage, lets look at the pushers in MessagingController as well?
        performAutomaticSetupNormally()
        checkAccountIsCreated()

        onView(withText("Done")).perform(click())
        doWait(3000L)

        //checkWeArriveToMessageList()
        val account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).getAccount(currentUuid)
        assertNotNull(account)
        val controller = MessagingController.getInstance(activityInstance)
        assertNotNull(controller.getPusher(account))
        Timber.d("==== pushers in controller are " + controller.pushers.map { it })
    }

    private fun checkWeArriveToMessageList() {
        checkAccountIsCreated()

        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches(withText(
                        "Inbox"
                )))
    }

    private val activityInstance: Activity?
        private get() {
            val activity = arrayOf<Activity?>(null)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                activity[0] = Iterables.getOnlyElement(activities)
            }
            return activity[0]
        }

    private fun goBackFromNamesToBasics() {
        onView(isRoot()).perform(pressBack())
        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches(withText(R.string.account_setup_basics_title)))
    }

    @Test
    fun testImportAccount() {
        startActivity()
        //openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        //onView(withId(R.id.import_settings)).perform(click());
        //onView(withText(R.string.action_settings)).perform(click());
        //sonView(allOf(withId(R.id.import_settings), withParent(withId(R.id.toolbar)))).perform(click());
        //openActionBarOverflowOrOptionsMenu(getInstrumentation().getContext());
        //onView(withText(R.string.action_settings)).perform(click());
        Espresso.openActionBarOverflowOrOptionsMenu(androidx.test.InstrumentationRegistry.getInstrumentation().targetContext)
        val appCompatTextView = Espresso.onView(
                Matchers.allOf(withId(R.id.title), withText("Import settings"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(Matchers.`is`("android.support.v7.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()))
        appCompatTextView.perform(click())
    }

    private fun doWait() {
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun doWait(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun checkWeArrivedToSetupNames() {
        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches(withText(R.string.account_setup_names_title)))
    }

    private fun accountSetupName() {
        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches(withText(R.string.account_setup_names_title)))
        Espresso.onView(withId(R.id.account_name)).perform(replaceText("test"))
    }

    private fun accountSetupPEpOptions() {
        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches(withText(R.string.account_settings_title_fmt)))
        onView(withId(R.id.next)).perform(click())
    }

    private fun setupOutgoingSettings() {
        setupSettings(matches(withText(R.string.account_setup_outgoing_title)))
    }

    private fun setupSettings(matches: ViewAssertion) {
        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches)
        val server = BuildConfig.PEP_TEST_EMAIL_SERVER
        Espresso.onView(withId(R.id.account_server)).perform(replaceText(server))
        onView(withId(R.id.next)).perform(click())
    }

    private fun setupIncomingSettings() {
        setupSettings(matches(withText(R.string.account_setup_incoming_title)))
    }

    private fun setupEmailAndPassword() {
        onView(Matchers.allOf(withId(R.id.next), withText(R.string.next_action))).check(matches(isDisplayed()))
        onView(Matchers.allOf(isAssignableFrom(TextView::class.java),
                withParent(isAssignableFrom(Toolbar::class.java))))
                .check(matches(withText(R.string.account_setup_basics_title)))
        val email = BuildConfig.PEP_TEST_EMAIL_ADDRESS
        val pass = BuildConfig.PEP_TEST_EMAIL_PASSWORD
        onView(withId(R.id.account_email)).perform(replaceText(email))
        onView(withId(R.id.account_password)).perform(replaceText(pass))
    }

    private fun startActivity() {
        val intent = Intent()
        mActivityRule.launchActivity(intent)
    }

    companion object {
        private fun childAtPosition(
                parentMatcher: Matcher<View>, position: Int): Matcher<View> {
            return object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description) {
                    description.appendText("Child at position $position in parent ")
                    parentMatcher.describeTo(description)
                }

                public override fun matchesSafely(view: View): Boolean {
                    val parent = view.parent
                    return (parent is ViewGroup && parentMatcher.matches(parent)
                            && view == parent.getChildAt(position))
                }
            }
        }
    }

    @After
    fun tearDown() {
        val account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).getAccount(currentUuid)
        AccountRemover.launchRemoveAccount(account, ApplicationProvider.getApplicationContext())
    }
}