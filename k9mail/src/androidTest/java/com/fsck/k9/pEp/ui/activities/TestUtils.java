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
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.View;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;

import org.pEp.jniadapter.Rating;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

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
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.saveSizeInInt;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.assertThat;


class TestUtils {

    private static final String APP_ID = BuildConfig.APPLICATION_ID;
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final int FIVE_MINUTES = 5;
    private static final int MINUTE_IN_SECONDS = 60;
    private static final int SECOND_IN_MILIS = 1000;

    private IdlingResource idlingResource;
    private UiDevice device;
    private Context context;
    private Resources resources;
    private Instrumentation instrumentation;
    int messageListSize[] = new int[2];

    public static final int TIMEOUT_TEST = FIVE_MINUTES * MINUTE_IN_SECONDS * SECOND_IN_MILIS;

    TestUtils(UiDevice device, Instrumentation instrumentation) {
        this.device = device;
        this.instrumentation = instrumentation;
        context = InstrumentationRegistry.getTargetContext();
        resources = context.getResources();
    }

    void increaseTimeoutWait() {
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
        device.waitForIdle();
        onView(withId(R.id.account_description)).perform(typeText(description));
        onView(withId(R.id.account_name)).perform(typeText(userName), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.done)).perform(click());
    }

    void composeMessageButton() {
        clickView(R.id.fab_button_compose_message);
    }

    void goBackToMessageCompose() {
        boolean backToMessageCompose = false;
        while (!backToMessageCompose){
            try {
                pressBack();
                onView(withId(R.id.send)).check(matches(isDisplayed()));
                backToMessageCompose = true;
            } catch (Exception ex){
                Timber.i("View not found");
            }
        }
    }

    void clickView(int viewId) {
        boolean buttonClicked = false;
        while (!buttonClicked) {
            try {
                onView(withId(viewId)).perform(click());
                buttonClicked = true;
            } catch (Exception ex) {
                Timber.i("View not found: " + ex);
            }
        }
    }

     Activity getCurrentActivity() {
        final Activity[] activity = new Activity[1];
        onView(isRoot()).check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                java.util.Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                activity[0] = Iterables.getOnlyElement(activities);
            }
        });
        return activity[0];
    }

    void createAccount(boolean isGmail) {
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
            try {
                device.waitForIdle();
                UiObject allowPermissions = device.findObject(new UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .index(1));
                if (allowPermissions.exists()) {
                    allowPermissions.click();
                    device.waitForIdle();
                }
            } catch (Exception ignoredException) {
                Timber.i(ignoredException, "There is no permissions dialog to interact with ");
            }
            try {
                device.waitForIdle();
                UiObject allowPermissions = device.findObject(new UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .index(1));
                if (allowPermissions.exists()) {
                    allowPermissions.click();
                    device.waitForIdle();
                }
            } catch (Exception ignoredException) {
                Timber.i(ignoredException, "There is no permissions dialog to interact with ");
            }
            try {
                device.waitForIdle();
                UiObject allowPermissions = device.findObject(new UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .index(1));
                if (allowPermissions.exists()) {
                    allowPermissions.click();
                    device.waitForIdle();
                }
            } catch (Exception ignoredException) {
                Timber.i(ignoredException, "There is no permissions dialog to interact with ");
            }
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
                doWaitForResource(R.id.account_description);
                device.waitForIdle();
                accountDescription(DESCRIPTION, USER_NAME);
            } catch (Exception ex) {
                Timber.i("Ignored", "Ignored exception");
            }
        } catch (Exception ignoredException) {
            Timber.i("Ignored", "Ignored exception");
        }
    }

    String getAccountDescription() {
        return DESCRIPTION;
    }

    void fillMessage(BasicMessage inputMessage, boolean attachFilesToMessage) {
        boolean messageFilled = false;
        while (!messageFilled){
            try {
                doWaitForResource(R.id.to);
                device.waitForIdle();
                device.findObject(By.res(APP_ID, "to")).longClick();
                device.waitForIdle();
                device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                device.waitForIdle();
                onView(withId(R.id.to)).perform(typeText(inputMessage.getTo()), closeSoftKeyboard());
                device.waitForIdle();
                onView(withId(R.id.subject)).perform(click());
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
            intending(not(isInternal())).respondWith(createFileForActivityResultStub(fileName + fileNumber + ".png"));
            device.waitForIdle();
            onView(withId(R.id.add_attachment)).perform(click());
            device.waitForIdle();
            onView(withId(R.id.attachments)).check(matches(hasDescendant(withText(fileName + fileNumber + extension))));
        }
    }

    void externalAppRespondWithFile(int id) {
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

    void sendMessage() {
        clickView(R.id.send);
    }

    void pressBack() {
        device.pressBack();
    }

    void removeLastAccount() {
        longClick("accounts_list");
        selectRemoveAccount();
        clickAcceptButton();
    }

    public void goBackAndRemoveAccount(){
        boolean accountRemoved = false;
        while (!accountRemoved) {
            try {
                device.waitForIdle();
                removeLastAccount();
                accountRemoved = true;
            } catch (Exception ex) {
                pressBack();
                Timber.i("View not found, pressBack to previous activity: " + ex);
            }
        }
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    void clickAcceptButton() {
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.okay_action)).perform(click());
    }

    void clickCancelButton() {
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.cancel_action)).perform(click());
    }

    void doWaitForObject(String object) {
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

    void testStatusEmpty() {
        checkStatus(Rating.pEpRatingUndefined);
        Espresso.pressBack();
    }

    void testStatusMail(BasicMessage inputMessage, BasicIdentity expectedIdentity) {
        fillMessage(inputMessage, false);
        device.waitForIdle();
        checkStatus(expectedIdentity.getRating());
        Espresso.pressBack();
    }

    void testStatusMailAndListMail(BasicMessage inputMessage, BasicIdentity expectedIdentity) {
        fillMessage(inputMessage, false);
        device.waitForIdle();
        checkStatus(expectedIdentity.getRating());
        onView(withText(expectedIdentity.getAddress())).check(doesNotExist());
        Espresso.pressBack();
    }

    void checkStatus(Rating rating) {
        clickView(R.id.pEp_indicator);
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, rating.value))));
    }

    void disableProtection(){
        device.waitForIdle();
        openOptionsMenu();
        selectFromScreen(R.string.pep_force_unprotected);
        instrumentation.waitForIdleSync();
        boolean toolbarClosed = false;
        while (!toolbarClosed){
            try{
                onView(withId(R.id.message_content)).perform(typeText(""));
                toolbarClosed = true;
            } catch (Exception ex){
                Timber.i("Toolbar is not closed yet");
            }
        }
        onView(withId(R.id.subject)).perform(click());
    }

    String getTextFromTextViewThatContainsText(String text) {
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 textView : device.findObjects(selector)) {
            if (textView.getText() != null && textView.getText().contains(text)) {
                return textView.getText();
            }
        }
        return "not found";
    }

    void getActivityInstance() {
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

    void openOptionsMenu() {
        device.waitForIdle();
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    }

    void selectFromScreen(int resource) {
        boolean textViewFound = false;
        while (!textViewFound) {
            BySelector selector = By.clazz("android.widget.TextView");
            for (UiObject2 object : device.findObjects(selector)) {
                if (object.getText().equals(resources.getString(resource))) {
                    device.waitForIdle();
                    object.click();
                    device.waitForIdle();
                    textViewFound = true;
                    break;
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

    void doWaitForResource(int resource) {
        try {
            idlingResource = new ViewVisibilityIdlingResource(getCurrentActivity(), resource, View.VISIBLE);
            IdlingRegistry.getInstance().register(idlingResource);
        } catch (Exception ex){
            Timber.i("Idling Resource does not exist: " + ex);
        }
    }

    void doWaitForIdlingListViewResource(int resource){
            try {
                device.waitForIdle();
                idlingResource = new ListViewIdlingResource(instrumentation,
                        getCurrentActivity().findViewById(resource));
                IdlingRegistry.getInstance().register(idlingResource);
            } catch (Exception ex){
                Timber.i("Idling Resource does not exist: " + ex);
            }
    }

    void doWaitForAlertDialog(IntentsTestRule<SplashActivity> intent, int displayText) {
        onView(withId(intent.getActivity().getResources()
                .getIdentifier("alertTitle", "id", "android")))
                .inRoot(isDialog())
                .check(matches(withText(displayText)))
                .check(matches(isDisplayed()));
    }

    String getResourceString(int id, int position) {
        return resources.getStringArray(id)[position];
    }

    void assertMessageStatus(int status) {
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
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, status))));
        device.waitForIdle();
    }

    void clickMessageStatus() {
        clickView(R.id.tvPep);
    }

    void clickLastMessageReceived() {
        boolean messageClicked = false;
        while (!messageClicked){
            try {
                device.waitForIdle();
                onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
            } catch (Exception ex){
                Timber.i("Message has been clicked");
                messageClicked = true;
            }
        }
        try{
            device.waitForIdle();
            onView(withText(R.string.cancel_action)).perform(click());
        }catch (NoMatchingViewException ignoredException){
            Timber.i("Ignored exception. Email is not encrypted");
        }
    }

    void waitForNewMessage() {
        boolean newEmail = false;
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
                device.waitForIdle();
                onView(withId(R.id.message_list)).perform(swipeDown());
                Timber.i("Message list found");
                actionPerformed = true;
            } catch (Exception ex) {
                Timber.i("Message list not found, waiting for view...");
            }
        }
    }

    void removeMessagesFromList(){
            device.waitForIdle();
            doWaitForResource(R.id.message_list);
            device.waitForIdle();
            doWaitForIdlingListViewResource(R.id.message_list);
            onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
            IdlingRegistry.getInstance().unregister(idlingResource);
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
            }
    }

    void checkToolBarColor(int color) {
        doWaitForResource(R.id.toolbar);
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(color)));
    }

    void startActivity() {
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
    private String getEmail() {
        return BuildConfig.PEP_TEST_EMAIL_ADDRESS;
    }

    @NonNull
    private String getPassword() {
        return BuildConfig.PEP_TEST_EMAIL_PASSWORD;
    }

    public static class BasicMessage {
        String from;
        String message;
        String subject;
        String to;

        BasicMessage(String from, String subject, String message, String to) {
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
