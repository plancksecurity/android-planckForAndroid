package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.text.format.DateUtils;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


@RunWith(AndroidJUnit4.class)
public class ImportSettingsTest {

    private static final String PACKAGE = "pep.android.k9";
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final int LAUNCH_TIMEOUT = 15000;

    private UiDevice device;

    @Before
    public void startMainActivityFromHomeScreen() {
        increaseTimeoutWait();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), LAUNCH_TIMEOUT);
    }

    @Test
    public void ImportSettingsTest() {
        accountConfiguration();
        accountDescription(DESCRIPTION, USER_NAME);
        device.waitForIdle();
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        device.waitForIdle();
        selectSettingsImportExport();
        device.waitForIdle();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //waitForMenu();
        selectSettingsImport();
        getActivityInstance();
        removeAccount();
        device.waitForIdle();
    }

    private void accountConfiguration(){
        doWait("skip");
        device.findObject(By.res(PACKAGE, "skip")).click();
        newEmailAccount();
    }

    private void newEmailAccount(){
        doWait("account_email");
        device.findObject(By.res(PACKAGE, "account_email")).setText(getEmail());
        device.findObject(By.res(PACKAGE, "account_password")).setText(getPassword());
        device.findObject(By.res(PACKAGE, "manual_setup")).click();
        fillImapData();
        device.findObject(By.res(PACKAGE, "next")).click();
        fillSmptData();
        doWait("next");
        device.findObject(By.res(PACKAGE, "next")).click();
        doWait("next");
        device.findObject(By.res(PACKAGE, "next")).click();
    }

    private void fillSmptData() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fillServerData();
    }

    private void fillImapData() {
        fillServerData();
    }

    private void fillServerData() {
        doWait("account_server");
        device.findObject(By.res(PACKAGE, "account_server")).setText(getEmailServer());
        device.findObject(By.res(PACKAGE, "account_username")).setText(getEmail());
        device.waitForIdle();
    }

    private void accountDescription(String description, String userName) {
        doWait("account_description");
        device.findObject(By.res(PACKAGE, "account_description")).setText(description);
        device.findObject(By.res(PACKAGE, "account_name")).setText(userName);
        device.findObject(By.res(PACKAGE, "done")).click();
    }

    private void waitForMenu(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        String originalText = device.findObjects(selector).get(size-1).getText();
        while ((size == device.findObjects(selector).size())){
            device.waitForIdle();
        }
    }

    private void selectSettingsImportExport(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        for (int i = 0; i < size; i++) {
            if (device.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.import_export_action))){
                device.findObjects(selector).get(i).click();
                i = size;
            }
        }
    }

    private void selectSettingsImport(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        for (int i = 0; i < size; i++) {
            if (device.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.settings_import))){
                device.findObjects(selector).get(i).click();
                i = size;
            }
        }}

    private void doWait(String viewId){
        UiObject2 androidRocksTextView = device
                .wait(Until.findObject(By.res(PACKAGE, viewId)),
                        150000);
        assertThat(androidRocksTextView, notNullValue());
    }

    private void removeAccount(){
        doWait("accounts_list");
        device.waitForIdle();
        longClick("accounts_list");
        device.waitForIdle();
        selectRemoveAccount();
        device.waitForIdle();
        selectAcceptButton();
    }

    private void selectRemoveAccount(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        for (int i = 0; i < size; i++) {
            if (device.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.remove_account_action))){
                device.findObjects(selector).get(i).click();
                i = size;
            }
        }
    }

    private void selectAcceptButton(){
        waitForObject("android.widget.Button");
        BySelector selector = By.clazz("android.widget.Button");
        device.findObjects(selector).get(1).click();
    }

    private void waitForObject(String object){
        boolean finish = false;
        do {
            if (device.findObject(By.clazz(object)) != null){
                finish = true;
            }
        }while (!finish);}

    private void longClick(String view){
        UiObject2 list = device.findObject(By.res(PACKAGE, view));
        Rect bounds = list.getVisibleBounds();
        device.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 180);}

    private String getLauncherPackageName() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

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

    private void increaseTimeoutWait(){
        long waitingTime = DateUtils.SECOND_IN_MILLIS * 150;
        IdlingPolicies.setMasterPolicyTimeout(waitingTime, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTime, TimeUnit.MILLISECONDS);
    }

    @NonNull
    private String getEmail() {return BuildConfig.PEP_TEST_EMAIL_ADDRESS;}

    @NonNull
    private String getEmailServer() {return BuildConfig.PEP_TEST_EMAIL_SERVER;}

    @NonNull
    private String getPassword(){return  BuildConfig.PEP_TEST_EMAIL_PASSWORD;}
}