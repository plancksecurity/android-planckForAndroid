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
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;


public class ImportSettingsTest {

    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";

    private TestUtils testUtils;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivityFromHomeScreen() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.startActivity();
    }

    @Test
    public void importSettings() {
        importSettingsTest(false);
    }

    private void importSettingsTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.externalAppRespondWithFile(R.raw.settings);
        onView(withId(R.id.skip)).perform(click());
        if (isGmail) {
            testUtils.gmailAccount();
        } else {
            testUtils.newEmailAccount();
        }
        testUtils.accountDescription(DESCRIPTION, USER_NAME);
        testUtils.pressBack();
        testUtils.doWait();
        testUtils.openOptionsMenu();
        testUtils.doWait();
        testUtils.selectFromMenu(R.string.import_export_action);
        testUtils.doWait();
        testUtils.doWaitForResource(R.string.settings_import);
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.doWait();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.settings_import_selection);
        testUtils.selectAcceptButton();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.settings_import_success_header);
        testUtils.selectAcceptButton();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.settings_import_activate_account_header);
        testUtils.selectCancelButton();
        testUtils.doWait();
        assertExistsTest();
        testUtils.removeLastAccount();
        testUtils.doWait();
        assertExistsTest();
        testUtils.removeLastAccount();
        testUtils.doWait();
        testUtils.doWaitForResource(R.id.skip);
        assertThereAreNoAccounts();
    }

    private void assertExistsTest(){
        onData(anything())
                .inAdapterView(withId(R.id.accounts_list))
                .atPosition(0)
                .onChildView(withId(R.id.description))
                .check(matches(withText(DESCRIPTION)));
    }

    private void assertThereAreNoAccounts(){
        onView(withId(R.id.skip))
                .check(matches(isDisplayed()));
    }
}