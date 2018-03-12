package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

@RunWith(AndroidJUnit4.class)
public class ImportSettingsCancelFromAccountTest {

    private TestUtils testUtils;
    private UiDevice device;

    @Before
    public void startpEpApp() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @Test (timeout = TIMEOUT_TEST)
    public void importSettingsCancel() {
        importSettingsCancelTest(false);
    }

    public void importSettingsCancelTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(isGmail);
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.import_export_action);
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.settings_import);
        testUtils.getActivityInstance();
        testUtils.goBackAndRemoveAccount();
        device.waitForIdle();
        assertThereAreNoAccounts();
    }

    private void assertThereAreNoAccounts(){
        onView(withId(R.id.skip))
                .check(matches(isDisplayed()));
    }
}