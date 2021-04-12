package com.fsck.k9.pEp.ui.activities;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.ui.tools.Theme;
import com.fsck.k9.pEp.ui.tools.ThemeManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class ImportSettingsDarkThemeTest {

    private TestUtils testUtils;
    private UiDevice device;


    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.skipTutorialAndAllowPermissionsIfNeeded();
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
        testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void importSettingDarkTheme() {
        // FIXME: 12/04/2021 This test is fixed by P4A-1331. Please remove this fixme after P4A-1331 is merged.
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.doWaitForAlertDialog(R.string.settings_import_selection);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_success_header);
        testUtils.clickAcceptButton();
        device.waitForIdle();
        assertEquals(Theme.DARK, ThemeManager.getLegacyTheme());
    }
}
