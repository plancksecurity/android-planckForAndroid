package com.fsck.k9.pEp.ui.activities;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

@RunWith(AndroidJUnit4.class)
public class ImportSettingsCancelFromAccountTest {

    private TestUtils testUtils;
    private UiDevice device;

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

    @Test (timeout = TIMEOUT_TEST)
    public void importSettingsCancel() {
        importSettingsCancelTest(false);
    }

    public void importSettingsCancelTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount();
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