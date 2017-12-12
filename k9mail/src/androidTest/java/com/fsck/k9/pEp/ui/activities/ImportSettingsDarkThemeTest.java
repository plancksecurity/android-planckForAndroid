package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class ImportSettingsDarkThemeTest {

    private TestUtils testUtils;
    private UiDevice device;


    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
        testUtils.startActivity();
    }

    @Test
    public void importSettingDarkTheme(){
        testUtils.pressBack();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.import_export_action);
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.settings_import);
        turnOnCheckBoxAndOffTheOther(R.string.settings_import_global_settings);
        testUtils.selectAcceptButton();
    }

    private void turnOnCheckBoxAndOffTheOther(int resourceOn){
        BySelector selector = By.clazz("android.widget.CheckedTextView");
        device.waitForIdle();
        for (UiObject2 checkBox : device.findObjects(selector))
            {device.waitForIdle();
            if (checkBox.getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(resourceOn))){
                if (!checkBox.isChecked()){
                    checkBox.click();
                }
            }else{
                if (checkBox.isChecked()){
                    checkBox.longClick();
                }
            }
        }
        device.waitForIdle();
    }
}
