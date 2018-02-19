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

@RunWith(AndroidJUnit4.class)
public class ImportSettingsCancelFromAccountTest {

    private TestUtils testUtils;

    @Before
    public void startpEpApp() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.startActivity();
    }

    @Test
    public void importSettingsCancel() {
        importSettingsCancelTest(false);
    }

    public void importSettingsCancelTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(isGmail);
        testUtils.doWait();
        testUtils.pressBack();
        testUtils.doWait();
        testUtils.openOptionsMenu();
        testUtils.doWait();
        testUtils.selectFromScreen(R.string.import_export_action);
        testUtils.doWait();
        testUtils.doWaitForResource(R.string.settings_import);
        testUtils.selectFromScreen(R.string.settings_import);
        testUtils.getActivityInstance();
        testUtils.goBackAndRemoveAccount();
        testUtils.doWait();
        assertThereAreNoAccounts();
    }

    private void assertThereAreNoAccounts(){
        onView(withId(R.id.skip))
                .check(matches(isDisplayed()));
    }
}