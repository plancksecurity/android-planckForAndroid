package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ImportSettingsCancelTest {

    private static final int TIMEOUT = 15000;
    private static final String PACKAGE = "pep.android.k9";

    private UiDevice mDevice;

    @Before
    public void startMainActivityFromHomeScreen() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), TIMEOUT);
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        mDevice.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), TIMEOUT);
    }

    @Test
    public void importSettings(){
        waitForSkipButton();
        mDevice.waitForIdle();
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        mDevice.waitForIdle();
        selectImportSettings();
        getActivityInstance();
        mDevice.waitForIdle();
    }

    private void waitForSkipButton(){
        doWait("skip");
        mDevice.findObject(By.res(PACKAGE, "skip")).click();
    }

    private void doWait(String viewId){
        mDevice.wait(Until.findObject(By.res(PACKAGE, viewId)),TIMEOUT);
    }

        private void selectImportSettings(){
            BySelector selector = By.clazz("android.widget.TextView");
            mDevice.findObjects(selector).get(2).click();
        }

    private String getLauncherPackageName() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }
    public void getActivityInstance(){
            do {
                mDevice.waitForIdle();
            }while (PACKAGE.equals(mDevice.getCurrentPackageName()));

            do {
                mDevice.pressBack();
            }
            while (!PACKAGE.equals(mDevice.getCurrentPackageName()));
    }
}
