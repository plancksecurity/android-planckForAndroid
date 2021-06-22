package com.fsck.k9.activity.setup;

import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.ui.activities.SplashActivity;
import com.fsck.k9.pEp.ui.activities.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withListSize;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccountSetupBasicsTest {

    private TestUtils testUtils;
    private UiDevice device;
    private Preferences preferences;
    private List<Account> previousAccounts;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void setUp() throws Exception {
        preferences = Preferences.getPreferences(ApplicationProvider.getApplicationContext());
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.skipTutorialAndAllowPermissionsIfNeeded();
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void testManualSetup() {
        previousAccounts = preferences.getAccounts();
        setupEmailAndPassword();
        onView(withId(R.id.manual_setup)).perform(click());
        setupIncomingSettings();
        setupOutgoingSettings();
        accountSetupPEpOptions();
        accountSetupName(false);
        checkLastAccountInSettings();
    }

    @Test
    public void testAutomaticSetup() {
        previousAccounts = preferences.getAccounts();
        setupEmailAndPassword();
        onView(withId(R.id.next)).perform(click());
        accountSetupName(false);
        checkLastAccountInSettings();
    }

    @Test
    public void testImportAccount() {
        testUtils.externalAppRespondWithFile(R.raw.test);
        previousAccounts = preferences.getAccounts();

        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.doWaitForAlertDialog(R.string.settings_import_selection);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_success_header);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_activate_account_header);
        onView(withId(R.id.incoming_server_password)).perform(replaceText(BuildConfig.PEP_TEST_EMAIL_PASSWORD));
        testUtils.clickAcceptButton();
        device.waitForIdle();
        checkLastAccountInSettings();
    }

    private void checkLastAccountInSettings() {
        testUtils.selectFromMenu(R.string.action_settings);

        List<Account> newAccounts = preferences.getAccounts();
        assertEquals(previousAccounts.size() + 1, newAccounts.size());

        testUtils.doWaitForResource(R.id.accounts_list);

        onView(withId(R.id.accounts_list)).check(matches(withListSize(newAccounts.size())));

        int lastIndex = newAccounts.size() - 1;

        onView(withRecyclerView(R.id.accounts_list).atPositionOnView(lastIndex, R.id.description))
                .check(matches(withText(newAccounts.get(lastIndex).getEmail())));
    }

    private void accountSetupName(boolean withSync) {
        testUtils.waitUntilViewDisplayed(R.id.account_name);
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches(withText(R.string.account_setup_names_title)));

        onView(withId(R.id.account_name)).perform(replaceText("test"));
        if (!withSync) {
            onView(withId(R.id.pep_enable_sync_account)).perform(click());
        }
        onView(withId(R.id.done)).perform(click());
    }

    private void accountSetupPEpOptions() {
        testUtils.waitUntilViewDisplayed(R.id.next);
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches(withText(R.string.account_settings_title_fmt)));

        onView(withId(R.id.next)).perform(click());
    }

    private void setupOutgoingSettings() {
        setupSettings(R.string.account_setup_outgoing_title);
    }

    private void setupSettings(int stringResource) {
        testUtils.waitUntilViewDisplayed(onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class)), withText(stringResource))));

        String server = BuildConfig.PEP_TEST_EMAIL_SERVER;

        onView(withId(R.id.account_server)).perform(replaceText(server));
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
    }

    private void setupIncomingSettings() {
        setupSettings(R.string.account_setup_incoming_title);
    }

    private void setupEmailAndPassword() {
        onView(allOf(withId(R.id.next), withText(R.string.next_action))).check(matches(isDisplayed()));
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches(withText(R.string.account_setup_basics_title)));

        String email = testUtils.getAccountEmailForDevice();
        String pass = BuildConfig.PEP_TEST_EMAIL_PASSWORD;
        onView(withId(R.id.account_email)).perform(replaceText(email));
        onView(withId(R.id.account_password)).perform(replaceText(pass));
    }
}