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
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ImportSettingsCancelTest {

    private TestUtils testUtils;

    @Before
    public void startMainActivityFromHomeScreen() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
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
        testUtils.openOptionsMenu();
        testUtils.doWait();
        testUtils.selectFromMenu(R.string.settings_import);
        testUtils.getActivityInstance();
        testUtils.doWait();
    }
}
