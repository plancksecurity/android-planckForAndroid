package com.fsck.k9.planck.ui.activities;

import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;
import com.fsck.k9.planck.ui.tools.Theme;
import com.fsck.k9.planck.ui.tools.ThemeManager;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class ImportSettingsDarkThemeTest extends BaseAndroidTest {

    @Before
    public void startPlanckApp() {
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
        testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void importSettingDarkTheme() {
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.doWaitForAlertDialog(R.string.settings_import_selection);
        testUtils.clickAcceptButton();
        testUtils.doWaitForAlertDialog(R.string.settings_import_success_header);
        testUtils.clickAcceptButton();
        TestUtils.waitForIdle();
        assertEquals(Theme.DARK, ThemeManager.getLegacyTheme());
    }
}
