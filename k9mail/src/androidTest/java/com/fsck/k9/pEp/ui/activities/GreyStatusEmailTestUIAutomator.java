package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.view.KeyEvent;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static java.lang.Thread.sleep;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.JMock1Matchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by juan on 11/10/17.
 */

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class GreyStatusEmailTestUIAutomator {

    private static final String PACKAGE = "pep.android.k9";
    private static final int TIME = 2000;
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final String EMAIL = "newemail@mail.es";
    private static final int LAUNCH_TIMEOUT = 5000;
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
/*
    @Test
    public void checkPreconditions() {
        assertThat(mDevice, notNullValue());
    }
*/
    @Test
    public void greyStatusEmailTest() {
        accountConfiguration();
        accountDescription(DESCRIPTION, USER_NAME);
        accountListSelect(DESCRIPTION);
        composseMessageButton();
        testStatusEmpty();
        testStatusMail(EMAIL, "Subject", "Message", Rating.pEpRatingUnencrypted.value);
        testStatusMail("", "", "", Rating.pEpRatingUndefined.value);
        testStatusMail(EMAIL, "", "", Rating.pEpRatingUnencrypted.value);
        sendEmail();
    }

    private void accountConfiguration(){
        mDevice.findObject(By.res(PACKAGE, "skip")).click();
        newEmailAccount();
    }

    private void newEmailAccount(){
        doWait(TIME);
        mDevice.findObject(By.res(PACKAGE, "account_email")).setText(getEmail());
        mDevice.findObject(By.res(PACKAGE, "account_password")).setText(getPassword());
        mDevice.findObject(By.res(PACKAGE, "manual_setup")).click();

        fillImapData();
        mDevice.findObject(By.res(PACKAGE, "next")).click();
        doWait(TIME);
        fillSmptData();
        doWait(TIME);
        mDevice.findObject(By.res(PACKAGE, "next")).click();
        doWait(TIME);
        mDevice.findObject(By.res(PACKAGE, "next")).click();
    }

    private void fillSmptData() {
        fillServerData();
    }

    private void fillImapData() {
        fillServerData();
    }

    private void fillServerData() {
        doWait(TIME);
        mDevice.findObject(By.res(PACKAGE, "account_server")).setText(getEmailServer());
        mDevice.findObject(By.res(PACKAGE, "account_username")).setText(getEmail());
    }

    private void accountDescription(String description, String userName) {
        doWait(TIME);
        mDevice.findObject(By.res(PACKAGE, "account_description")).setText(description);
        mDevice.findObject(By.res(PACKAGE, "account_name")).setText(userName);
        //doWait(TIME);
        mDevice.findObject(By.res(PACKAGE, "done")).click();
    }

    private void accountListSelect(String description){
        doWait(TIME);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        mDevice.findObject(By.res(PACKAGE, "accounts_list")).click();
        UiScrollable listView = new UiScrollable(new UiSelector());
        UiObject listViewItem = null;
        try {
            listViewItem = listView.getChildByText(new UiSelector().className(android.widget.TextView.class.getName()), description);
            listViewItem.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void composseMessageButton(){
        mDevice.findObject(By.res(PACKAGE, "fab_button_compose_message")).click();
    }

    private void checkStatus(int status){
        doWait(TIME);
        mDevice.findObject(By.res(PACKAGE, "pEp_indicator")).click();
        doWait(TIME);
        assertEquals(mDevice.findObject(By.res(PACKAGE, "pEpTitle")).getText(), getResourceString(R.array.pep_title, status));
        mDevice.pressBack();
    }

    private void testStatusEmpty(){
        checkStatus(Rating.pEpRatingUndefined.value);
    }

    private void testStatusMail(String to, String subject, String message, int status){
        fillEmail(to, subject, message);
        doWait(TIME);
        checkStatus(status);
    }

    private void fillEmail(String to, String subject, String message){
        onView(withId(R.id.to)).perform(click());
        doWait(TIME);
        onView(withId(R.id.to)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DEL));
        doWait(TIME);
        onView(withId(R.id.to)).perform(typeText(to));
        onView(withId(R.id.subject)).perform(typeText(subject));
        onView(withId(R.id.message_content)).perform(typeText(message));
        onView(withId(R.id.message_content)).perform(click());
    }
    private void sendEmail(){
        mDevice.findObject(By.res(PACKAGE, "send")).click();
    }

    private void doWait(int timeMillis){
        try {
            Thread.sleep(timeMillis);
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

    private String getResourceString(int id, int n) {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        return targetContext.getResources().getStringArray(id)[n];
    }

    @NonNull
    private String getEmail() {return BuildConfig.PEP_TEST_EMAIL_ADDRESS;}

    @NonNull
    private String getEmailServer() {return BuildConfig.PEP_TEST_EMAIL_SERVER;}

    @NonNull
    private String getPassword(){return  BuildConfig.PEP_TEST_EMAIL_PASSWORD;}
}
