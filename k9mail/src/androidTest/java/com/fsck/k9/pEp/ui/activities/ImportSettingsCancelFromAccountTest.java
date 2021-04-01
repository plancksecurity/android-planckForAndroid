package com.fsck.k9.pEp.ui.activities;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.saveSizeInInt;
import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ImportSettingsCancelFromAccountTest {

    private TestUtils testUtils;
    private UiDevice device;
    private final int[] accountListSize = new int[2];

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.increaseTimeoutWait();
        testUtils.skipTutorialAndAllowPermissionsIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test (timeout = TIMEOUT_TEST)
    public void importSettingsFromAccountSetupCancelTest() {
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.getActivityInstance();
        onView(withText(R.string.account_setup_basics_title)).check(matches(isDisplayed()));
    }

    @Test (timeout = TIMEOUT_TEST)
    public void importSettingsFromSettingsCancelTest() {
        testUtils.setupAccountIfNeeded();
        testUtils.selectFromMenu(R.string.action_settings);
        onView(withId(R.id.accounts_list)).perform(saveSizeInInt(accountListSize, 0));
        testUtils.selectFromMenu(R.string.import_export_action);
        testUtils.selectFromScreen(R.string.settings_import);
        testUtils.getActivityInstance();
        onView(withId(R.id.accounts_list)).perform(saveSizeInInt(accountListSize, 1));
        assertEquals(accountListSize[0], accountListSize[1]);
    }
}