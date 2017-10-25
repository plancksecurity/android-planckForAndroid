package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
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
@SdkSuppress(minSdkVersion = 18)
public class ImportSettingsTest {

    private static final String PACKAGE = "pep.android.k9";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final int TIME = 1000;
    private String originalText = "";
    private UiDevice mDevice;
    @Before
    public void startMainActivityFromHomeScreen() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        mDevice.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), LAUNCH_TIMEOUT);
    }

    @Test
    public void importCrash(){
        doWait("skip");
        mDevice.findObject(By.res(PACKAGE, "skip")).click();
        doWait();
        originalText = mDevice.getCurrentPackageName();
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        doWait();
        BySelector selector = By.clazz("android.widget.TextView");
        mDevice.findObjects(selector).get(2).click();
        getActivityInstance();
        doWait();

    }

    private void doWait(String viewId){
        mDevice.wait(Until.findObject(By.res(PACKAGE, viewId)),15000);
    }

    private void doWait(){
        try {
            Thread.sleep(TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                mDevice.pressBack();
                doWait();
            }
            while (originalText != mDevice.getCurrentPackageName());
    }
}
