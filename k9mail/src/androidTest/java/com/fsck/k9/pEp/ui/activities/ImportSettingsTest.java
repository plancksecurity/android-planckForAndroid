package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.collect.Iterables;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.util.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collection;

import timber.log.Timber;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.pEp.jniadapter.AndroidHelper.TAG;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class ImportSettingsTest {

    private static final String PACKAGE = "pep.android.k9";
    private static final int LAUNCH_TIMEOUT = 5000;
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
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        doWait();
        BySelector selector = By.clazz("android.widget.TextView");
        mDevice.findObjects(selector).get(2).click();
        //doWait("icon1");
        //mDevice.findObject(By.res(PACKAGE, "icon1")).click();
        //BySelector selector2 = By.clazz("android.widget.ImageView");
        //mDevice.findObjects(selector2).get(0).click();
        //mDevice.pressHome();
        //mDevice.pressBack();
        //UiObject2 icon2 = mDevice.findObject(By.res(PACKAGE, "icon1"));
        //Rect bounds = icon.getVisibleBounds();
        //mDevice.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 20);
        //mDevice.swipe(40, 100, 40, 100, 50);
        doWait();
        getActivityInstance();
        //mDevice.click(mDevice.getDisplayWidth()/20, mDevice.getDisplayHeight()/12);
        doWait();

    }

    private void doWait(String viewId){
        mDevice.wait(Until.findObject(By.res(PACKAGE, viewId)),15000);
    }

    private void doWait(){
        //getInstrumentation().waitForIdleSync();

        try {
            Thread.sleep(3000);
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
        BySelector selector = By.clazz("android.widget.TextView");
        do {
            Log.e(TAG, "Nombre original: " + originalText);
            mDevice.pressBack();
        }while (originalText != mDevice.findObjects(selector).get(0).getText());
    }
}
