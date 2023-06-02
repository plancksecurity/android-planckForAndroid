package com.fsck.k9.planck.ui.activities;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.saveSizeInInt;
import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ImportSettingsCancelTest extends BaseAndroidTest {

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void importSettingsFromAccountSetupCancelTest() {
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.getActivityInstance();
        onView(withText(R.string.account_setup_basics_title)).check(matches(isDisplayed()));
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
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