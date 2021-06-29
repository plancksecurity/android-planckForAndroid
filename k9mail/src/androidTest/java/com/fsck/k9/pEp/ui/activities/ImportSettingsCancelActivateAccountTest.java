package com.fsck.k9.pEp.ui.activities;

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

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.saveSizeInInt;
import static junit.framework.TestCase.assertEquals;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ImportSettingsCancelActivateAccountTest {

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

        testUtils.skipTutorialAndAllowPermissionsIfNeeded();
        testUtils.externalAppRespondWithFile(R.raw.test);
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    // DO NOT RUN INDIVIDUALLY, RUN WHOLE CLASS.
    @Test
    public void importSettingsWithAccountFromSettingsCancelTest() {
        testUtils.doWaitForAlertDialog(R.string.settings_import_activate_account_header);
        testUtils.clickCancelButton();

        onView(withId(R.id.accounts_list)).perform(saveSizeInInt(accountListSize, 0));
        testUtils.selectFromMenu(R.string.import_export_action);
        testUtils.selectFromScreen(R.string.settings_import);
        testUtils.doWaitForAlertDialog(R.string.settings_import_selection);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_success_header);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_activate_account_header);
        onView(withId(R.id.incoming_server_password)).perform(replaceText(BuildConfig.PEP_TEST_EMAIL_PASSWORD));
        testUtils.clickCancelButton();
        device.waitForIdle();
        onView(withId(R.id.accounts_list)).perform(saveSizeInInt(accountListSize, 1));
        testUtils.removeAllAccounts();
        assertEquals(accountListSize[0] + 1, accountListSize[1]);
    }

    // DO NOT RUN INDIVIDUALLY, RUN WHOLE CLASS.
    @Test
    public void importSettingsWithAccountFromAccountSetupCancelTest() {
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.doWaitForAlertDialog(R.string.settings_import_selection);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_success_header);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_activate_account_header);
        onView(withId(R.id.incoming_server_password)).perform(replaceText(BuildConfig.PEP_TEST_EMAIL_PASSWORD));
        testUtils.clickCancelButton();
        device.waitForIdle();
        List<Account> accounts =
                Preferences.getPreferences(ApplicationProvider.getApplicationContext()).getAccounts();
        assertEquals(1, accounts.size());
    }
}