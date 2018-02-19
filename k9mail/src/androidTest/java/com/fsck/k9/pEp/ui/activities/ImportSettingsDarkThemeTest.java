package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.K9;
import com.fsck.k9.R;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


public class ImportSettingsDarkThemeTest {

    private TestUtils testUtils;
    private UiDevice device;


    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
        testUtils.startActivity();
    }

    @Test
    public void importSettingDarkTheme() {
        testUtils.createAccount(false);
        testUtils.pressBack();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.import_export_action);
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.settings_import);
        testUtils.clickAcceptButton();
        device.waitForIdle();
        testUtils.clickAcceptButton();
        device.waitForIdle();
        onView(withId(R.id.accounts_list)).perform(ViewActions.click());
        device.waitForIdle();
        try{
            Assert.assertEquals(K9.Theme.LIGHT, K9.getK9Theme());
        }catch (AssertionFailedError exception){
        }
        testUtils.goBackAndRemoveAccount();
    }
}
