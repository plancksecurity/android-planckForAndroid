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
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.privacy.status.PEpTrustwords;

import org.pEp.jniadapter.Rating;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static android.content.ContentValues.TAG;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.runner.lifecycle.Stage.RESUMED;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.saveSizeInInt;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.valuesAreEqual;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.viewIsDisplayed;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.waitUntilIdle;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.assertThat;


public class TestUtils {

    private static final String ANIMATION_PERMISSION = "android.permission.SET_ANIMATION_SCALE";
    private static final float ANIMATION_DISABLED = 0.0f;
    private static final String APP_ID = BuildConfig.APPLICATION_ID;
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final int FIVE_MINUTES = 5;
    private static final int MINUTE_IN_SECONDS = 60;
    private static final int SECOND_IN_MILIS = 1000;

    private UiDevice device;
    private Context context;
    private Resources resources;
    private Instrumentation instrumentation;
    int messageListSize[] = new int[2];

    public static final int TIMEOUT_TEST = FIVE_MINUTES * MINUTE_IN_SECONDS * SECOND_IN_MILIS;

    public TestUtils(UiDevice device, Instrumentation instrumentation) {
        this.device = device;
        this.instrumentation = instrumentation;
        context = InstrumentationRegistry.getTargetContext();
        resources = context.getResources();
    }

    public void increaseTimeoutWait() {
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

    private void newEmailAccount() {
        onView(withId(R.id.account_email)).perform(typeText(getEmail()));
        onView(withId(R.id.account_password)).perform(typeText(getPassword()), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
    }

    private void gmailAccount() {
        onView(withId(R.id.account_oauth2)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
    }

    private void accountDescription(String description, String userName) {
        doWaitForResource(R.id.account_description);
        device.waitForIdle();
        onView(withId(R.id.account_description)).perform(typeText(description));
        onView(withId(R.id.account_name)).perform(typeText(userName), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.done)).perform(click());
    }

    public void composeMessageButton() {
        clickView(R.id.fab_button_compose_message);
    }

    void goBackToMessageCompose() {
        boolean backToMessageCompose = false;
        while (!backToMessageCompose){
            pressBack();
            device.waitForIdle();
            if (exists(onView(withId(R.id.send)))){
                backToMessageCompose = true;
            }
        }
    }

    public void clickView(int viewId) {
        boolean buttonClicked = false;
        doWaitForResource(viewId);
        while (!buttonClicked) {
            if (exists(onView(withId(viewId))) && viewIsDisplayed(viewId)){
                device.waitForIdle();
                try {
                    onView(withId(viewId)).perform(click());
                    device.waitForIdle();
                    buttonClicked = true;
                    Timber.i("View found, can click it");
                } catch (Exception ex) {
                    Timber.i("View not found, cannot click it: " + ex);
                }
            }else {
                buttonClicked = true;
            }
        }
    }

    public void yellowStatusMessageTest(String messageSubject, String messageBody, String messageTo) {
        device.waitForIdle();
        fillMessage(new TestUtils.BasicMessage("", messageSubject, messageSubject, messageTo), false);
        onView(withId(R.id.pEp_indicator)).perform(click());
        onView(withId(R.id.my_recycler_view)).check(doesNotExist());
        assertCurrentActivityIsInstanceOf(PEpTrustwords.class);

    }

    public void assertCurrentActivityIsInstanceOf(Class<? extends Activity> activityClass) {
        Activity currentActivity = getCurrentActivity();
        checkNotNull(currentActivity);
        checkNotNull(activityClass);
        assertTrue(currentActivity.getClass().isAssignableFrom(activityClass));
    }

     Activity getCurrentActivity() {

         final Activity[] resumedActivity = {null};
         getInstrumentation().runOnMainSync(new Runnable() {
             public void run() {
                 Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                         .getActivitiesInStage(RESUMED);
                 if (resumedActivities.iterator().hasNext()) {
                     resumedActivity[0] = (Activity) resumedActivities.iterator().next();
                 }
             }
         });
         return resumedActivity[0];
    }

    public void createAccount(boolean isGmail) {
        createNewAccountWithPermissions(isGmail);
        removeMessagesFromList();
        getMessageListSize();
    }

    private void createNewAccountWithPermissions(boolean isGmail){
        try {
            onView(withId(R.id.next)).perform(click());
            device.waitForIdle();
            try {
                device.waitForIdle();
                onView(withId(R.id.skip)).perform(click());
                device.waitForIdle();
            } catch (Exception ignoredException) {
                Timber.i("Ignored", "Ignored exception");
            }
            try {
                device.waitForIdle();
                onView(withId(R.id.action_continue)).perform(click());
                device.waitForIdle();
            } catch (Exception ignoredException) {
                Timber.i("Ignored", "Ignored exception");
            }
            allowPermissions();
            try {
                device.waitForIdle();
                onView(withId(R.id.action_continue)).perform(click());
                device.waitForIdle();
            } catch (Exception ignoredException) {
                Timber.i("Ignored", "Ignored exception");
            }
            try {
                if (isGmail) {
                    gmailAccount();
                } else {
                    newEmailAccount();
                }
            } catch (Exception ex) {
                Timber.i("Ignored", "Exists account");
            }
            try {
                device.waitForIdle();
                accountDescription(DESCRIPTION, USER_NAME);
            } catch (Exception ex) {
                Timber.i("Ignored", "Ignored exception " + ex);
            }
        } catch (Exception ignoredException) {
            Timber.i("Ignored", "Ignored exception");
        }
    }

    void allowPermissions(){
        allowPermissions(2);
        allowPermissions(1);
    }

    void allowPermissions(int index) {
        boolean existPermission = false;
        while (!existPermission){
            try {
                device.waitForIdle();
                UiObject allowPermissions = device.findObject(new UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .index(index));
                if (allowPermissions.exists()) {
                    allowPermissions.click();
                    device.waitForIdle();
                } else {
                    Timber.i("There is no permissions dialog to interact with ");
                    existPermission = true;
                }
            } catch (Exception ignoredException) {
                Timber.i(ignoredException, "Failed trying to allow permission");
            }
        }
    }

    String getAccountDescription() {
        return DESCRIPTION;
    }

    public void fillMessage(BasicMessage inputMessage, boolean attachFilesToMessage) {
        boolean messageFilled = false;
        while (!messageFilled){
            try {
                device.waitForIdle();
                doWaitForResource(R.id.to);
                device.waitForIdle();
                device.findObject(By.res(APP_ID, "to")).click();
                device.waitForIdle();
                onView(withId(R.id.to)).perform(typeText(inputMessage.getTo()), closeSoftKeyboard());
                doWaitForResource(R.id.subject);
                device.waitForIdle();
                onView(withId(R.id.subject)).perform(typeText(inputMessage.getSubject()), closeSoftKeyboard());
                device.waitForIdle();
                onView(withId(R.id.message_content)).perform(typeText(inputMessage.getMessage()), closeSoftKeyboard());
                device.waitForIdle();
                messageFilled = true;
            } catch (Exception ex){
                Timber.i("Could not fill message: " + ex);
            }
        }
        Espresso.closeSoftKeyboard();
        if (attachFilesToMessage) {
            String fileName = "ic_test";
            String extension = ".png";
            attachFiles(fileName, extension);
        }
    }

    private void attachFiles(String fileName, String extension) {
        for (int fileNumber = 0; fileNumber < 3; fileNumber++) {
            Instrumentation.ActivityResult fileForActivityResultStub = createFileForActivityResultStub(fileName + fileNumber + ".png");
            try {
                intending(not(isInternal())).respondWith(fileForActivityResultStub);
            } catch (Exception ex) {
                Timber.e("Intending: " +ex);
            }
            device.waitForIdle();
            onView(withId(R.id.add_attachment)).perform(click());
            device.waitForIdle();
            onView(withId(R.id.attachments)).check(matches(hasDescendant(withText(fileName + fileNumber + extension))));
        }
    }

    public void externalAppRespondWithFile(int id) {
        intending(not(isInternal()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, insertFileIntoIntentAsData(id)));
    }

    private Instrumentation.ActivityResult createFileForActivityResultStub(String fileName) {
        convertResourceToBitmapFile(R.mipmap.icon, fileName);
        return new Instrumentation.ActivityResult(Activity.RESULT_OK, insertFileIntoIntentAsData(fileName));
    }

    private void convertResourceToBitmapFile(int resourceId, String fileName) {
        Bitmap bm = BitmapFactory.decodeResource(resources, resourceId);
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

    private Intent insertFileIntoIntentAsData(int id) {
        Uri fileUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                resources.getResourcePackageName(id) + "/" +
                resources.getResourceTypeName(id) + "/" +
                resources.getResourceEntryName(id));
        Intent resultData = new Intent();
        resultData.setData(fileUri);
        return resultData;
    }

    private Intent insertFileIntoIntentAsData(String fileName) {
        Intent resultData = new Intent();
        File fileLocation = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath(), fileName);
        resultData.setData(Uri.parse("file://" + fileLocation));
        return resultData;
    }

    public void sendMessage() {
        clickView(R.id.send);
    }

    public void pressBack() {
        device.pressBack();
    }

    void removeLastAccount() {
        longClick("accounts_list");
        selectRemoveAccount();
        clickAcceptButton();
    }

    public void goBackAndRemoveAccount() {
        goBackAndRemoveAccount(false);
    }

    public void goBackAndRemoveAccount(boolean discardMessage) {
        boolean accountRemoved = false;
        Activity currentActivity = getCurrentActivity();
        while (!accountRemoved) {
            try {
                device.waitForIdle();
                removeLastAccount();
                accountRemoved = true;
            } catch (Exception ex) {
                while (currentActivity == getCurrentActivity()) {
                    pressBack();
                    device.waitForIdle();
                    try {
                        if (discardMessage) {
                            onView(withText(R.string.discard_action)).check(matches(isCompletelyDisplayed()));
                            onView(withText(R.string.discard_action)).perform(click());
                            discardMessage = false;
                        }
                    } catch (Exception e) {
                        Timber.i("No dialog alert message");
                    }
                    Timber.i("View not found, pressBack to previous activity: " + ex);
                }
            }
            currentActivity = getCurrentActivity();
        }
    }

    public void clickAcceptButton() {
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.okay_action)).perform(click());
    }

    void clickCancelButton() {
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.cancel_action)).perform(click());
    }

    public void doWaitForObject(String object) {
        boolean finish = false;
        while (!finish){
            if (device.findObject(By.clazz(object)) != null) {
                finish = true;
            }
        }
    }

    private void selectRemoveAccount() {
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        while (size == 0) {
            size = device.findObjects(selector).size();
        }
        for (UiObject2 textView : device.findObjects(selector)) {
            if (textView.getText().equals(resources.getString(R.string.remove_account_action))) {
                textView.click();
                return;
            }
        }
    }

    void longClick(String viewId) {
        UiObject2 list = device.findObject(By.res(APP_ID, viewId));
        Rect bounds = list.getVisibleBounds();
        device.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 450);
    }

    public void testStatusEmpty() {
        checkStatus(Rating.pEpRatingUndefined);
        Espresso.pressBack();
    }

    void testStatusMail(BasicMessage inputMessage, BasicIdentity expectedIdentity) {
        fillMessage(inputMessage, false);
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(typeText(" "), closeSoftKeyboard());
        device.waitForIdle();
        checkStatus(expectedIdentity.getRating());
        Espresso.pressBack();
    }

    void testStatusMailAndListMail(BasicMessage inputMessage, BasicIdentity expectedIdentity) {
        fillMessage(inputMessage, false);
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(typeText(" "), closeSoftKeyboard());
        device.waitForIdle();
        checkStatus(expectedIdentity.getRating());
        onView(withText(expectedIdentity.getAddress())).check(doesNotExist());
        Espresso.pressBack();
    }

    public void checkStatus(Rating rating) {
        clickView(R.id.pEp_indicator);
        while (!exists(onView(withId(R.id.pEpTitle)))){
            doWaitForResource(R.id.pEpTitle);
            device.waitForIdle();
        }
        onView(withId(R.id.pEpTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, rating.value))));
    }

    public void goBackAndSaveAsDraft (IntentsTestRule activity){
        Activity currentActivity = getCurrentActivity();
        while (currentActivity == getCurrentActivity()){
            try {
                device.waitForIdle();
                device.pressBack();
                //doWaitForAlertDialog(activity, R.string.save_or_discard_draft_message_dlg_title);
                //doWaitForObject("android.widget.Button");
                device.waitForIdle();
                onView(withText(R.string.save_draft_action)).perform(click());
            } catch (Exception ex){
                Timber.i("Ignored exception: " + ex);
            }
        }
        device.waitForIdle();
    }

    public void assertsTextsOnScreenAreEqual(int resourceOnScreen, int comparedWith) {
        BySelector selector = By.clazz("android.widget.TextView");
        String textOnScreen = "Text not found on the Screen";
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains(resources.getString(resourceOnScreen))) {
                    device.waitForIdle();
                    textOnScreen = object.getText();
                    device.waitForIdle();
                    break;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
        device.pressBack();
        onView(withId(R.id.toolbar)).check(matches(valuesAreEqual(textOnScreen, resources.getString(comparedWith))));
    }

    public int stringToID(String text){
        return resources.getIdentifier(text, "string", BuildConfig.APPLICATION_ID);
    }

    public int intToID(String text){
        return resources.getIdentifier(text, "id", BuildConfig.APPLICATION_ID);
    }

    public int colorToID(String color){
        return resources.getIdentifier(color, "color", BuildConfig.APPLICATION_ID);
    }

    public void checkToolbarColor(int color) {
        boolean toolbarExists = false;
        while (!toolbarExists) {
            if (exists(onView(withId(R.id.toolbar)))) {
                onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(color)));
                toolbarExists = true;
            }
        }
    }

    public void selectoFromMenu(int viewId){
        device.waitForIdle();
        openOptionsMenu();
        selectFromScreen(viewId);
        boolean toolbarClosed = false;
        while (!toolbarClosed){
            try{
                onView(withId(R.id.message_content)).perform(typeText(" "));
                device.waitForIdle();
                toolbarClosed = true;
            } catch (Exception ex){
                Timber.i("Toolbar is not closed yet");
            }
        }
        try {
            onView(withId(R.id.subject)).perform(click());
        } catch (Exception ex) {
            Timber.i("Ignored Exception: " + ex);
        }
    }

    public String getTextFromTextViewThatContainsText(String text) {
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 textView : device.findObjects(selector)) {
            if (textView.getText() != null && textView.getText().contains(text)) {
                return textView.getText();
            }
        }
        return "not found";
    }

    public void getActivityInstance() {
        waitForExternalApp();
        goBackToOriginalApp();
    }

    private void waitForExternalApp() {
        while (APP_ID.equals(device.getCurrentPackageName())) {
            device.waitForIdle();
        }
    }

    private void goBackToOriginalApp() {
        while (!APP_ID.equals(device.getCurrentPackageName())) {
            device.pressBack();
        }
    }

    public void openOptionsMenu() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        device.waitForIdle();
    }

    public void selectFromScreen(int resource) {
        boolean textViewFound = false;
        BySelector selector = By.clazz("android.widget.TextView");
        while (!textViewFound) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().contains(resources.getString(resource))) {
                        device.waitForIdle();
                        object.click();
                        device.waitForIdle();
                        textViewFound = true;
                        break;
                    }
                } catch (Exception ex){
                    Timber.i("Cannot find text on screen: " + ex);
                }
            }
        }
    }

    void doWait(String viewId) {
        UiObject2 waitForView = device
                .wait(Until.findObject(By.res(APP_ID, viewId)),
                        150000);
        assertThat(waitForView, notNullValue());
    }

    public void doWaitForResource(int resource) {
        IdlingResource idlingResourceVisibility = null;
        Activity currentActivity = getCurrentActivity();
            try {
                idlingResourceVisibility = new ViewVisibilityIdlingResource(currentActivity, resource, View.VISIBLE);
                IdlingRegistry.getInstance().register(idlingResourceVisibility);
                onView(withId(resource)).check(matches(isDisplayed()));
            } catch (Exception ex) {
                Timber.i("Idling Resource does not exist: " + ex);
            } finally {
                IdlingRegistry.getInstance().unregister(idlingResourceVisibility);
            }
    }

    void doWaitForIdlingListViewResource(int resource){
        IdlingResource idlingResourceListView;
        device.waitForIdle();
        idlingResourceListView = new ListViewIdlingResource(instrumentation,
                getCurrentActivity().findViewById(resource));
            try {
                IdlingRegistry.getInstance().register(idlingResourceListView);
                onView(withId(resource)).check(matches(isDisplayed()));
            } catch (Exception ex){
                Timber.i("Idling Resource does not exist: " + ex);
            } finally {
                IdlingRegistry.getInstance().unregister(idlingResourceListView);
            }
    }

    public void doWaitForAlertDialog(IntentsTestRule<SplashActivity> intent, int displayText) {
        onView(withId(intent.getActivity().getResources()
                .getIdentifier("alertTitle", "id", "android")))
                .inRoot(isDialog())
                .check(matches(withText(displayText)))
                .check(matches(isDisplayed()));
    }

    String getResourceString(int id, int position) {
        return resources.getStringArray(id)[position];
    }

    public void assertMessageStatus(int status) {
        boolean viewDisplayed = false;
        while (!viewDisplayed){
            try{
                device.waitForIdle();
                doWaitForResource(R.id.pEpTitle);
                viewDisplayed = true;
                device.waitForIdle();
            } catch (Exception ex){
                Timber.i("View not found: " + ex);
            }
        }
        waitUntilIdle();
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, status))));
        device.waitForIdle();
    }

    public void clickMessageStatus() {
        clickView(R.id.tvPep);
    }

    public void goBackToMessageList(){
        boolean backToMessageCompose = false;
        while (!backToMessageCompose){
            device.pressBack();
            device.waitForIdle();
            if (viewIsDisplayed(R.id.fab_button_compose_message)){
                backToMessageCompose = true;
            }
        }
    }

    public void goToSentFolder() {
        BySelector textViewSelector;
        textViewSelector = By.clazz("android.widget.TextView");
        device.waitForIdle();
        openOptionsMenu();
        device.waitForIdle();
        selectFromScreen(R.string.account_settings_folders);
        device.waitForIdle();
        String folder = resources.getString(R.string.special_mailbox_name_sent);
        boolean folderClicked = false;
        while (!folderClicked) {
            for (UiObject2 textView : device.findObjects(textViewSelector)) {
                try {
                    if (textView.findObject(textViewSelector).getText() != null && textView.findObject(textViewSelector).getText().contains(folder)) {
                        textView.findObject(textViewSelector).click();
                        folderClicked = true;
                        return;
                    }
                    device.waitForIdle();
                } catch (Exception e) {
                    Timber.i("View is not sent folder");
                }
            }
        }
        device.waitForIdle();
        waitForTextOnScreen(resources.getString(R.string.special_mailbox_name_sent));
    }

    private void waitForTextOnScreen(String text) {
        boolean textIsOk = false;
        do {
            device.waitForIdle();
            try {
                textIsOk = getTextFromTextViewThatContainsText(text).contains(resources.getString(R.string.special_mailbox_name_sent));
            } catch (Exception e) {

            }
        } while (!textIsOk);
    }

    public void clickLastMessageReceived() {
        boolean messageClicked = false;
        while (!messageClicked){
            device.waitForIdle();
            if (viewIsDisplayed(R.id.message_list)) {
                try {
                    swipeDownMessageList();
                    onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
                    messageClicked = true;
                    device.waitForIdle();
                    if (viewIsDisplayed(R.id.fab_button_compose_message)) {
                        try {
                            messageClicked = false;
                            pressBack();
                        } catch (Exception ex) {
                            Timber.i("Last message has been clicked");
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("No message found");
                }
            }
        }
        try{
            onView(withText(R.string.cancel_action)).perform(click());
        }catch (NoMatchingViewException ignoredException){
            Timber.i("Ignored exception. Email is not encrypted");
        }
    }

    public void waitForNewMessage() {
        boolean newEmail = false;
        doWaitForResource(R.id.message_list);
        doWaitForIdlingListViewResource(R.id.message_list);
        while (!newEmail) {
            try {
                device.waitForIdle();
                swipeDownMessageList();
                device.waitForIdle();
                onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 1));
                if (messageListSize[1] > messageListSize[0]){
                    newEmail = true;
                }
            } catch (Exception ex) {
                Timber.i("Waiting for new message : " + ex);
            }
        }
        getMessageListSize();
    }

    void getMessageListSize(){
        onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 0));}

    void swipeDownMessageList (){
        boolean actionPerformed = false;
        while (!actionPerformed) {
            try {
                onView(withId(R.id.message_list)).perform(swipeDown());
                Timber.i("Message list found");
                actionPerformed = true;
            } catch (Exception ex) {
                Timber.i("Message list not found, waiting for view...");
            }
        }
    }

    void removeMessagesFromList(){
        clickFirstMessage();
            boolean emptyList = false;
            while (!emptyList){
                try{
                    device.waitForIdle();
                    onView(withText(R.string.cancel_action)).perform(click());
                }catch (NoMatchingViewException ignoredException){
                    Timber.i("Ignored exception");
                }
                try {
                    device.waitForIdle();
                    onView(withId(R.id.delete)).perform(click());
                } catch (NoMatchingViewException ignoredException) {
                    emptyList = true;
                }
                device.waitForIdle();
                if (exists(onView(withId(android.R.id.message)))){
                    emptyList = false;
                }
            }
    }

    public void clickFirstMessage(){
        boolean firstMessageClicked = false;
        while (!firstMessageClicked){
            try{
                if(viewIsDisplayed(R.id.message_list)) {
                    doWaitForResource(R.id.message_list);
                    doWaitForIdlingListViewResource(R.id.message_list);
                    device.waitForIdle();
                    swipeDownMessageList();
                    device.waitForIdle();
                    getMessageListSize();
                    if (viewIsDisplayed(R.id.reply_message) || messageListSize[0] == 1) {
                        firstMessageClicked = true;
                    }
                    else {
                        device.waitForIdle();
                        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
                        device.waitForIdle();
                    }
                    if (!viewIsDisplayed(R.id.message_list)) {
                        firstMessageClicked = true;
                    }
                }
            } catch (Exception ex){
                Timber.i("Cannot find list: " + ex);
            }
        }
    }

    void checkToolBarColor(int color) {
        device.waitForIdle();
        while (!exists(onView(withId(R.id.toolbar)))) {
            doWaitForResource(R.id.toolbar);
            device.waitForIdle();
        }
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(color)));
    }

    void goBackToMessageListAndPressComposeMessageButton() {
        boolean backToMessageList = false;
        Activity currentActivity = getCurrentActivity();
        while (!backToMessageList){
            try {
                device.pressBack();
                device.waitForIdle();
                try {
                    if (currentActivity == getCurrentActivity() && exists(onView(withText(R.string.discard_action)))) {
                        onView(withText(R.string.discard_action)).check(matches(isDisplayed()));
                        onView(withText(R.string.discard_action)).perform(click());
                    }
                } catch (Exception e) {
                    Timber.i("No dialog alert message");
                }
                if (exists(onView(withId(R.id.fab_button_compose_message)))){
                    onView(withId(R.id.fab_button_compose_message)).perform(click());
                    backToMessageList = true;
                }
            } catch (Exception ex){
                Timber.i("View not found");
            }
        }
    }

    public void startActivity() {
        device.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(APP_ID);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        disableAllAnimations();
        context.startActivity(intent);
        device.wait(Until.hasObject(By.pkg(APP_ID).depth(0)), LAUNCH_TIMEOUT);
    }

    private void disableAllAnimations() {
        if (getAnimationPermissionStatus() == PackageManager.PERMISSION_GRANTED) {
            setSystemAnimationsScale(ANIMATION_DISABLED);
        } else {
            Log.e(TAG, "Not granted permission to change animation scale.");
        }
    }

    private int getAnimationPermissionStatus() {
        Context context = InstrumentationRegistry.getTargetContext();
        return context.checkCallingOrSelfPermission(ANIMATION_PERMISSION);
    }

    public void checkBoxOnScreenChecked(int resource, boolean checked) {
        boolean textViewFound = false;
        BySelector selector = By.clazz("android.widget.TextView");
        while (!textViewFound) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().contains(resources.getString(resource))) {
                        device.waitForIdle();
                        UiObject2 checkbox = object.getParent().getParent().getChildren().get(1).getChildren().get(0);
                        if (checkbox.isChecked() != checked){
                            device.waitForIdle();
                            checkbox.longClick();
                            device.waitForIdle();
                        }
                        if (checkbox.isChecked() == checked) {
                            device.waitForIdle();
                            textViewFound = true;
                            break;
                        }
                    }
                } catch (Exception ex){
                    Timber.i("Cannot find text on screen: " + ex);
                }
            }
        }
    }

    private void setSystemAnimationsScale(float animationScale) {
        try {
            Class<?> windowManagerStubClazz = Class.forName("android.view.IWindowManager$Stub");
            Method asInterface = windowManagerStubClazz.getDeclaredMethod("asInterface", IBinder.class);
            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClazz.getDeclaredMethod("getService", String.class);
            Class<?> windowManagerClazz = Class.forName("android.view.IWindowManager");
            Method setAnimationScales = windowManagerClazz.getDeclaredMethod("setAnimationScales", float[].class);
            Method getAnimationScales = windowManagerClazz.getDeclaredMethod("getAnimationScales");

            IBinder windowManagerBinder = (IBinder) getService.invoke(null, "window");
            Object windowManagerObj = asInterface.invoke(null, windowManagerBinder);
            float[] currentScales = (float[]) getAnimationScales.invoke(windowManagerObj);
            for (int i = 0; i < currentScales.length; i++) {
                currentScales[i] = animationScale;
            }
            setAnimationScales.invoke(windowManagerObj, new Object[]{currentScales});
        } catch (Exception ex) {
            Log.e(TAG, "Could not use reflection to change animation scale to: " + animationScale, ex);
        }
    }

    @NonNull
    private String getEmail() {
        return "test006@peptest.ch";
        //return BuildConfig.PEP_TEST_EMAIL_ADDRESS;
    }

    @NonNull
    private String getPassword() {
        return "pEpdichauf5MailPassword";
        //return BuildConfig.PEP_TEST_EMAIL_PASSWORD;
    }

    public static class BasicMessage {
        String from;
        String message;
        String subject;
        String to;

        public BasicMessage(String from, String subject, String message, String to) {
            this.from = from;
            this.message = message;
            this.subject = subject;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getSubject() {
            return subject;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class BasicIdentity {
        Rating rating;
        String address;

        BasicIdentity(Rating rating, String address) {
            this.rating = rating;
            this.address = address;
        }

        public Rating getRating() {
            return rating;
        }

        public String getAddress() {
            return address;
        }
    }
}
