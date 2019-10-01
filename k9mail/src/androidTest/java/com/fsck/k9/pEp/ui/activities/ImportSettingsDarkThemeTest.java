package com.fsck.k9.pEp.ui.activities;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;


public class ImportSettingsDarkThemeTest {

    private TestUtils testUtils;
    private UiDevice device;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;


    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        testUtils.increaseTimeoutWait();
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
        testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(espressoTestingIdlingResource.getIdlingResource());
    }

    @Test (timeout = TIMEOUT_TEST)
    public void importSettingDarkTheme() {
        testUtils.createAccount();
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
