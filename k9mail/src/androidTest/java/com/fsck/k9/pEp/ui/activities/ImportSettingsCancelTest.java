package com.fsck.k9.pEp.ui.activities;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static org.hamcrest.Matchers.anything;


public class ImportSettingsCancelTest {

    private TestUtils testUtils;
    private UiDevice device;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void importSettings() {
        importSettingsTest(false);
    }

    private void importSettingsTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
        testUtils.createAccount();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.import_export_action);
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.settings_import);
        device.waitForIdle();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.settings_import_selection);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.settings_import_success_header);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.settings_import_activate_account_header);
        testUtils.clickCancelButton();
        device.waitForIdle();
        assertExistsTest();
        testUtils.goBackAndRemoveAccount();
        device.waitForIdle();
        assertExistsTest();
        testUtils.goBackAndRemoveAccount();
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.skip);
        assertThereAreNoAccounts();
    }

    private void assertExistsTest() {
        onData(anything())
                .inAdapterView(withId(R.id.accounts_list))
                .atPosition(0)
                .onChildView(withId(R.id.description))
                .check(matches(withText(testUtils.getAccountDescription())));
    }

    private void assertThereAreNoAccounts() {
        onView(withId(R.id.skip))
                .check(matches(isDisplayed()));
    }
}