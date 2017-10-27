package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiCollection;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.util.Log;
import android.view.KeyEvent;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import java.util.regex.Pattern;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.pEp.jniadapter.AndroidHelper.TAG;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class GreyStatusEmailTestUIAutomator {

    private static final String PACKAGE = "pep.android.k9";
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final String EMAIL = "newemail@mail.es";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final long TIME = 6000;
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
    public void greyStatusEmailTest() {
        accountConfiguration();
        accountDescription(DESCRIPTION, USER_NAME);
        accountListSelect(DESCRIPTION);
        composseMessageButton();
        testStatusEmpty();
        testStatusMail(EMAIL, "Subject", "Message", Rating.pEpRatingUnencrypted.value);
        testStatusMail("", "", "", Rating.pEpRatingUndefined.value);
        testStatusMail(EMAIL, "Subject", "Message", Rating.pEpRatingUnencrypted.value);
        sendEmail();
        removeAccount();
    }

    private void accountConfiguration(){
        doWait("skip");
        mDevice.findObject(By.res(PACKAGE, "skip")).click();
        newEmailAccount();
    }

    private void newEmailAccount(){
        doWait("account_email");
        mDevice.findObject(By.res(PACKAGE, "account_email")).setText(getEmail());
        mDevice.findObject(By.res(PACKAGE, "account_password")).setText(getPassword());
        mDevice.findObject(By.res(PACKAGE, "manual_setup")).click();
        fillImapData();
        mDevice.findObject(By.res(PACKAGE, "next")).click();
        //waitForNotExists("next");
        fillSmptData();
        doWait("next");
        mDevice.findObject(By.res(PACKAGE, "next")).click();
        doWait("next");
        mDevice.findObject(By.res(PACKAGE, "next")).click();
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
        mDevice.findObject(By.res(PACKAGE, "account_server")).setText(getEmailServer());
        mDevice.findObject(By.res(PACKAGE, "account_username")).setText(getEmail());
        mDevice.waitForIdle();
    }

    private void waitForSetText(String text, String id){
        do {
            mDevice.findObject(By.res(PACKAGE, "account_server")).setText(getEmailServer());
        }while(mDevice.findObject(By.res(PACKAGE, id)).getText() != text);
    }

    private void accountDescription(String description, String userName) {
        doWait("account_description");
        mDevice.findObject(By.res(PACKAGE, "account_description")).setText(description);
        mDevice.findObject(By.res(PACKAGE, "account_name")).setText(userName);
        mDevice.findObject(By.res(PACKAGE, "done")).click();
    }

    private void accountListSelect(String description){
        doWait("accounts_list");
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
        doWait("pEp_indicator");
        mDevice.findObject(By.res(PACKAGE, "pEp_indicator")).click();
        doWait("pEpTitle");
        assertEquals(mDevice.findObject(By.res(PACKAGE, "pEpTitle")).getText(), getResourceString(R.array.pep_title, status));
        mDevice.pressBack();
    }

    private void testStatusEmpty(){
        checkStatus(Rating.pEpRatingUndefined.value);
    }

    private void testStatusMail(String to, String subject, String message, int status){
        fillEmail(to, subject, message);
        checkStatus(status);
    }

    private void fillEmail(String to, String subject, String message){
        doWait("to");
        mDevice.waitForIdle();
        mDevice.findObject(By.res(PACKAGE, "to")).click();
        doWait(2000);
        //onView(withId(R.id.to)).perform(click());
        //mDevice.pressDelete();
        mDevice.pressKeyCode(KeyEvent.KEYCODE_DEL);
        mDevice.waitForIdle();
        //mDevice.findObject(By.res(PACKAGE, "to")).setText(to);
        onView(withId(R.id.to)).perform(typeText(to));
        mDevice.findObject(By.res(PACKAGE, "subject")).setText(subject);
        mDevice.findObject(By.res(PACKAGE, "message_content")).setText(message);
        mDevice.findObject(By.res(PACKAGE, "message_content")).click();
        //onView(withId(R.id.subject)).perform(typeText(subject));
        //onView(withId(R.id.message_content)).perform(typeText(message));
        //onView(withId(R.id.message_content)).perform(click());
        /*
        onView(withId(R.id.to)).perform(click());
        onView(withId(R.id.to)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DEL));
        onView(withId(R.id.to)).perform(typeText(to));
        onView(withId(R.id.subject)).perform(typeText(subject));
        onView(withId(R.id.message_content)).perform(typeText(message));
        onView(withId(R.id.message_content)).perform(click());
    */
    }

    private void doWait(int timeMillis){
        try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void sendEmail(){
        doWait("send");
        mDevice.findObject(By.res(PACKAGE, "send")).click();
        doWait(35000);
        waitForNotExists("send");
    }

    private void doWait(String viewId){
        /*
        boolean finish = false;
        do {
            if (mDevice.findObject(By.res(PACKAGE, viewId)) != null){
                finish = true;
            }
        }while (!finish);
        */
        UiObject2 androidRocksTextView = mDevice
                .wait(Until.findObject(By.res(PACKAGE, viewId)),
                        150000);
        assertThat(androidRocksTextView, notNullValue());
    }

    private void waitForNotExists(String viewId){
        UiObject nextButton = mDevice.findObject(new UiSelector().resourceId(viewId));
        nextButton.waitUntilGone(TIME);
    }

    private void removeAccount(){
        mDevice.pressBack();
        doWait("accounts_list");
        mDevice.waitForIdle();
        longClick("accounts_list");
        //waitForNotExists("accounts_list");
        mDevice.waitForIdle();
        selectRemoveAccount();
        mDevice.waitForIdle();
        selectAcceptButton();
    }

    private void selectRemoveAccount(){
        BySelector selector = By.clazz("android.widget.TextView");
        //waitForNotExists("title");
        //mDevice.findObject(By.text(String.valueOf(R.string.remove_account_action))).click();
        //mDevice.findObject(By.text(String.valueOf(R.string.remove_account_action))).click();

        for (int i = 0; i < mDevice.findObjects(selector).size(); i++) {
            if (mDevice.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.remove_account_action))){
                mDevice.findObjects(selector).get(i).click();
            }
        }

        /*
        UiObject2 listView = mDevice.findObject(By.res(PACKAGE, "select_dialog_listview"));
        BySelector selectorLinearLayout = By.clazz("android.widget.LinearLayout");
        BySelector selectorRelativeLayout = By.clazz("android.widget.RelativeLayout");
        BySelector selectorTextView= By.clazz("android.widget.TextView");
        listView.findObjects(selectorLinearLayout).get(4).findObjects(selectorRelativeLayout).get(0).findObjects(selectorTextView).get(0).click();
        */
        /*
        UiCollection list = new UiCollection(
                new UiSelector().className("android.widget.ListView"));
        try {
            for (int i =0; i< 10; i++) {
                if (list.getChild(new UiSelector().index(i)).toString().equals(R.string.remove_account_action)){
                    list.getChild(new UiSelector().index(i)).click();
                }
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        */
        //listView.getChildren().get(4).getChildren().get(0).getChildren().get(0).click();
        //BySelector selector = By.clazz("android.widget.TextView");
        //listView.findObjects(selector).get(4).click();
        //mDevice.findObjects(By.res(PACKAGE, "title")).get(4).click();
    }

    private void selectAcceptButton(){
        waitForObject("android.widget.Button");
        BySelector selector = By.clazz("android.widget.Button");
        mDevice.findObjects(selector).get(1).click();
    }

    private void waitForObject(String object){
        boolean finish = false;
        do {
            if (mDevice.findObject(By.clazz(object)) != null){
                finish = true;
            }
        }while (!finish);}

    private void longClick(String view){
        UiObject2 list = mDevice.findObject(By.res(PACKAGE, view));
        Rect bounds = list.getVisibleBounds();
        mDevice.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 180);}

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
