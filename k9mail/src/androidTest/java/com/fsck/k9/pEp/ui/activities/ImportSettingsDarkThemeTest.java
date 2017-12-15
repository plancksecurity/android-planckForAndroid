package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;

import com.fsck.k9.K9;
import com.fsck.k9.R;

import junit.framework.Assert;

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
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.externalAppRespondWithFile(R.raw.settingsthemelight);
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
        device.waitForIdle();
        testUtils.selectAcceptButton();
        device.waitForIdle();
        onView(withId(R.id.accounts_list)).perform(ViewActions.click());
        device.waitForIdle();
        Assert.assertEquals(K9.Theme.DARK, K9.getK9Theme());
        /*testUtils.pressBack();
        device.waitForIdle();*/
    /*    try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        /*UiObject2 uo = selectLayout();
        uo.getText();*/
        //onView(withId(R.id.container)).check(matches(withBackgroundColor(android.R.color.background_dark)));
        //onView(withId(R.id.root_view)).check(matches(withBackgroundColor(android.R.color.background_light)));
        //onView(selectLayout()).check(matches(withBackgroundColor(R.color.white)));

      /*  UiCollection list = new UiCollection( new UiSelector().className("android.widget.LinearLayout"));
        list.getChild(new UiSelector().index(5)).*/

    }
    private UiObject2 selectLayout(){
        BySelector selector = By.clazz("android.widget.LinearLayout");
        return device.findObjects(selector).get(14);
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
