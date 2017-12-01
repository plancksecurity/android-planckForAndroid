package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.text.format.DateUtils;
import android.view.KeyEvent;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;

import org.pEp.jniadapter.Rating;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


class TestUtils {

    private static final String APP_ID = "pep.android.k9";
    private static final int LAUNCH_TIMEOUT = 5000;

    private UiDevice device;

    TestUtils(UiDevice device) {
        this.device = device;
    }

    void increaseTimeoutWait(){
        long waitingTime = DateUtils.SECOND_IN_MILLIS * 200;
        IdlingPolicies.setMasterPolicyTimeout(waitingTime, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTime, TimeUnit.MILLISECONDS);
    }

    private String getLauncherPackageName() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    void newEmailAccount(){
        onView(withId(R.id.account_email)).perform(typeText(getEmail()));
        onView(withId(R.id.account_password)).perform(typeText(getPassword()), closeSoftKeyboard());
        onView(withId(R.id.manual_setup)).perform(click());
        fillImapData();
        onView(withId(R.id.next)).perform(click());
        doWait();
        fillSmptData();
        doWait();
        onView(withId(R.id.next)).perform(click());
        doWait();
        onView(withId(R.id.next)).perform(click());
    }

    void gmailAccount(){
        onView(withId(R.id.account_oauth2)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.next)).perform(click());
        doWait();
        onView(withId(R.id.next)).perform(click());
        doWait();
        onView(withId(R.id.next)).perform(click());
    }

    private void fillSmptData() {
        fillServerData();
    }

    private void fillImapData() {
        fillServerData();
    }

    private void fillServerData() {
        onView(withId(R.id.account_server)).perform(replaceText(getEmailServer()));
        onView(withId(R.id.account_username)).perform(replaceText(getEmail()));
    }

    void accountDescription(String description, String userName) {
        onView(withId(R.id.account_description)).perform(typeText(description));
        onView(withId(R.id.account_name)).perform(typeText(userName));
        doWait();
        onView(withId(R.id.done)).perform(click());
    }

    void composseMessageButton(){
        onView(withId(R.id.fab_button_compose_message)).perform(click());
    }

    void fillEmail(String to, String subject, String message, boolean attachFilesToEmail){
        doWait("to");
        doWait();
        device.findObject(By.res(APP_ID, "to")).longClick();
        doWait();
        device.pressKeyCode(KeyEvent.KEYCODE_DEL);
        doWait();
        onView(withId(R.id.to)).perform(typeText(to), closeSoftKeyboard());
        device.findObject(By.res(APP_ID, "subject")).setText(subject);
        device.findObject(By.res(APP_ID, "message_content")).setText(message);
        device.findObject(By.res(APP_ID, "message_content")).click();
        Espresso.closeSoftKeyboard();
        if (attachFilesToEmail) {
            String fileName = "ic_test";
            String extension = ".png";
            attachFiles(fileName, extension);
        }
    }

    private void attachFiles(String fileName, String extension){
        for (int fileNumber = 0; fileNumber<3; fileNumber++){
            intending(not(isInternal())).respondWith(createFileForActivityResultStub(fileName+fileNumber+".png"));
            doWait();
            onView(withId(R.id.add_attachment)).perform(click());
            doWait();
            onView(withId(R.id.attachments)).check(matches(hasDescendant(withText(fileName+fileNumber+extension))));
        }
    }

    void externalAppRespondWithFile(int id){
        intending(not(isInternal()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, insertFileIntoIntentAsData(id)));
    }

    private Instrumentation.ActivityResult createFileForActivityResultStub(String fileName) {
        convertResourceToBitmapFile(R.mipmap.icon, fileName);
        return new Instrumentation.ActivityResult(Activity.RESULT_OK, insertFileIntoIntentAsData(fileName));
    }

    private void convertResourceToBitmapFile(int resource, String fileName) {
        Bitmap bm = BitmapFactory.decodeResource(InstrumentationRegistry.getTargetContext().getResources(), resource);
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        File fileImage = new File(extStorageDirectory, fileName);
        try {
            FileOutputStream outStream;
            outStream = new FileOutputStream(fileImage);
            bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Intent insertFileIntoIntentAsData(int id){
        Resources resources = InstrumentationRegistry.getTargetContext().getResources();
        Uri fileUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                resources.getResourcePackageName(id) +"/" +
                resources.getResourceTypeName(id) + "/" +
                resources.getResourceEntryName(id));
        Intent resultData = new Intent();
        resultData.setData(fileUri);
        return  resultData;
    }

    private Intent insertFileIntoIntentAsData(String fileName){
        Intent resultData = new Intent();
        File filelocation = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath(), fileName);
        resultData.setData(Uri.parse( "file://"+filelocation));
        return  resultData;
    }

    void sendEmail(){
        onView(withId(R.id.send)).perform(click());
    }

    void pressBack(){
        device.pressBack();
    }

    void removeLastAccount(){
        doWait();
        doWait("accounts_list");
        doWait();
        longClick("accounts_list");
        doWait();
        selectRemoveAccount();
        doWait();
        selectAcceptButton();
    }

    void selectAcceptButton(){
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.okay_action)).perform(click());
    }

    void selectCancelButton(){
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.cancel_action)).perform(click());
    }

    private void doWaitForObject(String object){
        boolean finish = false;
        do {
            if (device.findObject(By.clazz(object)) != null){
                finish = true;
            }
        }while (!finish);
    }

    private void selectRemoveAccount(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size;
        do {
            doWait();
            size = device.findObjects(selector).size();
        }while (size == 0);
        for (int i = 0; i < size; i++) {
            if (device.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.remove_account_action))){
                device.findObjects(selector).get(i).click();
                i = size;
            }
        }
    }

    void longClick(String view){
        UiObject2 list = device.findObject(By.res(APP_ID, view));
        Rect bounds = list.getVisibleBounds();
        device.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 180);
    }

    void testStatusEmpty(){
        checkStatus(Rating.pEpRatingUndefined.value);
        Espresso.pressBack();
    }

    void testStatusMail(String to, String subject, String message, int status){
        fillEmail(to, subject, message, false);
        doWait();
        checkStatus(status);
        Espresso.pressBack();
    }

    void testStatusMailAndListMail(String to, String subject, String message, int status, String email){
        fillEmail(to, subject, message, false);
        doWait();
        checkStatus(status);
        onView(withText(email)).check(doesNotExist());
        Espresso.pressBack();
    }

    private void checkStatus(int status){
        onView(withId(R.id.pEp_indicator)).perform(click());
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, status))));
    }

    public String getTextFromTextviewThatContainsText(String text){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        for (int i = 0; i < size; i++) {
            device.findObjects(selector).get(i);
            if (device.findObjects(selector).get(i).getText() != null && device.findObjects(selector).get(i).getText().contains(text)){
                return device.findObjects(selector).get(i).getText();
            }
        }
        return "not found";
    }

    void getActivityInstance(){
        waitForExternalApp();
        goBackToOriginalApp();
    }

    private void waitForExternalApp(){
        while (APP_ID.equals(device.getCurrentPackageName())){
            device.waitForIdle();
        }
    }

    private void goBackToOriginalApp(){
        while (!APP_ID.equals(device.getCurrentPackageName())){
            device.pressBack();
        }
    }

    void openOptionsMenu(){
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    }

    void selectFromMenu(int resource){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        for (int i = 0; i < size; i++) {
            if (device.findObjects(selector).get(i).getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(resource))){
                device.findObjects(selector).get(i).click();
                break;
            }
        }
    }

    void doWait(){
        device.waitForIdle();
    }

    void doWait(String viewId){
        UiObject2 waitForView = device
                .wait(Until.findObject(By.res(APP_ID, viewId)),
                        150000);
        assertThat(waitForView, notNullValue());
    }

    void doWaitForResource(int resource){
        device.wait(Until.hasObject(By.desc(InstrumentationRegistry.getTargetContext().getResources().getString(resource))), 1);
    }

    void doWaitForAlertDialog(IntentsTestRule<SplashActivity> intent, int displayText){
        onView(withId(intent.getActivity().getResources()
                .getIdentifier( "alertTitle", "id", "android" )))
                .inRoot(isDialog())
                .check(matches(withText(displayText)))
                .check(matches(isDisplayed()));
    }

    private String getResourceString(int id, int n) {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        return targetContext.getResources().getStringArray(id)[n];
    }

    void startActivity(){
        device.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(APP_ID);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        context.startActivity(intent);
        device.wait(Until.hasObject(By.pkg(APP_ID).depth(0)), LAUNCH_TIMEOUT);
    }

    @NonNull
    private String getEmail() {return BuildConfig.PEP_TEST_EMAIL_ADDRESS;}

    @NonNull
    private String getEmailServer() {return BuildConfig.PEP_TEST_EMAIL_SERVER;}

    @NonNull
    private String getPassword(){return  BuildConfig.PEP_TEST_EMAIL_PASSWORD;}
}
