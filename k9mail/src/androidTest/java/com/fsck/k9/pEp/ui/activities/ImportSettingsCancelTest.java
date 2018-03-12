package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static org.hamcrest.Matchers.anything;


public class ImportSettingsCancelTest {

    private TestUtils testUtils;
    private UiDevice device;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @Test (timeout = TIMEOUT_TEST)
    public void importSettings() {
        importSettingsTest(false);
    }

    private void importSettingsTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.externalAppRespondWithFile(R.raw.settings);
        testUtils.createAccount(isGmail);
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.import_export_action);
        device.waitForIdle();
        testUtils.doWaitForResource(R.string.settings_import);
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

    private void assertExistsTest(){
        onData(anything())
                .inAdapterView(withId(R.id.accounts_list))
                .atPosition(0)
                .onChildView(withId(R.id.description))
                .check(matches(withText(testUtils.getAccountDescription())));
    }

    private void assertThereAreNoAccounts(){
        onView(withId(R.id.skip))
                .check(matches(isDisplayed()));
    }
}