package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
public class ImportSettingsTest {

    private static final String PACKAGE = "pep.android.k9";
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";

    private UiDevice device;
    private TestUtils testUtils;

    @Before
    public void startMainActivityFromHomeScreen() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.startActivity();
    }

    @Test
    public void importSettings(){
        importSettingsTest(false);
    }

    public void importSettingsTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        onView(withId(R.id.skip)).perform(click());
        if (isGmail) {
            testUtils.gmailAccount();
        } else {
            testUtils.newEmailAccount();
        }
        testUtils.accountDescription(DESCRIPTION, USER_NAME);
        device.waitForIdle();
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        device.waitForIdle();
        selectSettingsImportExport();
        device.waitForIdle();
        waitForMenu();
        selectSettingsImport();
        getActivityInstance();
        testUtils.removeAccount("accounts_list");
        device.waitForIdle();
    }

    private void waitForMenu(){
        device.wait(Until.hasObject(By.desc(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.settings_import))), 1);
    }

    private void selectSettingsImportExport(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        for (int i = 0; i < size; i++) {
            if (device.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.import_export_action))){
                device.findObjects(selector).get(i).click();
                break;
            }
        }
    }

    private void selectSettingsImport(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        for (int i = 0; i < size; i++) {
            if (device.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.settings_import))){
                device.findObjects(selector).get(i).click();
                break;
            }
        }}


    private void getActivityInstance(){
        waitForExternalApp();
        goBackToOriginalApp();
    }

    private void waitForExternalApp(){
        while (PACKAGE.equals(device.getCurrentPackageName())){
            device.waitForIdle();
        }
    }

    private void goBackToOriginalApp(){
        while (!PACKAGE.equals(device.getCurrentPackageName())){
            device.pressBack();
        }
    }
}