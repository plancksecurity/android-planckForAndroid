package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class ImportSettingsCancelTest {

    private TestUtils testUtils;
    private UiDevice device;
    private static final String APP_ID = "pep.android.k9";

    @Before
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.startActivity();
    }

    @Test
    public void importSettings(){
        importSettingsTest();
    }

    public void importSettingsTest(){
        testUtils.increaseTimeoutWait();
        onView(withId(R.id.skip)).perform(click());
        testUtils.doWait();
        assertPackageNameIsCurrentPackageName();
        testUtils.openOptionsMenu();
        testUtils.doWait();
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.getActivityInstance();
        testUtils.doWait();
        assertPackageNameIsCurrentPackageName();
    }

    private void assertPackageNameIsCurrentPackageName(){
        assertThat(APP_ID, is((device.getCurrentPackageName())));}
}
