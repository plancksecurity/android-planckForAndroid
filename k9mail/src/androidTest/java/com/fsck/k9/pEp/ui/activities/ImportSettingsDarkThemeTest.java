package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class ImportSettingsDarkThemeTest {

    private TestUtils testUtils;
    private UiDevice device;

    @Before
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @Test
    public void importSettingDarkTheme(){
        testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
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
        BySelector selector = By.clazz("android.widget.Checkbox");
        for (UiObject2 checkBox : device.findObjects(selector))
            {
            if (checkBox.getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(resourceOn))){
                if (!checkBox.isChecked()){
                    checkBox.click();
                }
            }else{
                if (checkBox.isChecked()){
                    checkBox.click();
                }
            }
        }
    }
}
