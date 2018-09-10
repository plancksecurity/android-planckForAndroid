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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collection;
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
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.hasValueEqualTo;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.saveSizeInInt;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.valuesAreEqual;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.viewIsDisplayed;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.viewWithTextIsDisplayed;
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

    private static UiDevice device;
    private static Context context;
    private Resources resources;
    private Instrumentation instrumentation;
    private int messageListSize[] = new int[2];

    public static final int TIMEOUT_TEST = FIVE_MINUTES * MINUTE_IN_SECONDS * SECOND_IN_MILIS;
    public TestConfig testConfig;
    public String botList[];

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
        onView(withId(R.id.account_email)).perform(typeText(testConfig.getMail())); // getEmail()
        onView(withId(R.id.account_password)).perform(typeText(testConfig.getPassword()), closeSoftKeyboard()); // getPassword()
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
        if (viewWithTextIsDisplayed(resources.getString(R.string.account_already_exists))) {
            device.pressBack();
        }
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

    private void assertCurrentActivityIsInstanceOf(Class<? extends Activity> activityClass) {
        Activity currentActivity = getCurrentActivity();
        checkNotNull(currentActivity);
        checkNotNull(activityClass);
        assertTrue(currentActivity.getClass().isAssignableFrom(activityClass));
    }

     public Activity getCurrentActivity() {

         final Activity[] resumedActivity = {null};
         getInstrumentation().runOnMainSync(() -> {
             Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                     .getActivitiesInStage(RESUMED);
             if (resumedActivities.iterator().hasNext()) {
                 resumedActivity[0] = (Activity) resumedActivities.iterator().next();
             }
         });
         return resumedActivity[0];
    }

    public void createAccount(boolean isGmail) {
        createNewAccountWithPermissions(isGmail);
        removeMessagesFromList();
        getMessageListSize();
    }

    private void readConfigFile() {
        File directory = new File(Environment.getExternalStorageDirectory().toString());

        File newFile = new File(directory, "test/test_config.txt");
        testConfig = new TestConfig();
        try  {
            FileInputStream fin = new FileInputStream(newFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fin);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            while ( (receiveString = bufferedReader.readLine()) != null ) {
                String line[] = receiveString.split(" = ");
                if (line.length > 1) {
                    switch (line[0]) {
                        case "mail":
                            testConfig.setMail(line[1]);
                            break;
                        case "password":
                            testConfig.setPassword(line[1]);
                            break;
                        case "username":
                            testConfig.setUsername(line[1]);
                            break;
                        case "imap_server":
                            testConfig.setImap_server(line[1]);
                            break;
                        case "smtp_server":
                            testConfig.setSmtp_server(line[1]);
                            break;
                        case "imap_port":
                            testConfig.setImap_port(line[1]);
                            break;
                        case "smtp_port":
                            testConfig.setSmtp_port(line[1]);
                            break;
                        default:
                            break;
                    }
                }
            }
            fin.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void readBotList(){
        Timber.i("Lee la lista");
        File directory = new File(Environment.getExternalStorageDirectory().toString());

        File newFile = new File(directory, "test/botlist.txt");
        testConfig = new TestConfig();
        try  {
            FileInputStream fin = new FileInputStream(newFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fin);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            botList = new String[4];
            int position = 0;
            while ( (receiveString = bufferedReader.readLine()) != null ) {
                botList[position++] = receiveString;
            }
            fin.close();
        }catch (Exception e){
            e.printStackTrace();
        }

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
            readConfigFile();
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
                boolean descriptionFilled = false;
                while (!descriptionFilled) {
                    try {
                        device.waitForIdle();
                        accountDescription(DESCRIPTION, USER_NAME);
                        descriptionFilled = true;
                    } catch (Exception e){
                        Timber.i("Can not fill account description");
                    }
                }
            } catch (Exception ex) {
                Timber.i("Ignored", "Exists account");
            }
        } catch (Exception ex) {
            readConfigFile();
            Timber.i("Ignored", "Exists account, failed creating new one");
        }
    }

    private void allowPermissions(){
        allowPermissions(2);
        allowPermissions(1);
    }

    private void allowPermissions(int index) {
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
                device.findObject(By.res(APP_ID, "subject")).click();
                device.waitForIdle();
                device.findObject(By.res(APP_ID, "message_content")).click();
                device.waitForIdle();
                onView(withId(R.id.to)).perform(typeText(inputMessage.getTo() +  ","), closeSoftKeyboard());
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
            attachFiles(fileName, extension, 3);
        }
    }

    private void attachFiles(String fileName, String extension, int total) {
        for (int fileNumber = 0; fileNumber < total; fileNumber++) {
            Instrumentation.ActivityResult fileForActivityResultStub = createFileForActivityResultStub(fileName + fileNumber + extension);
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

    public void attachFile(String fileName) {//, String extension
        device.waitForIdle();
        onView(withId(R.id.add_attachment)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.attachments)).check(matches(hasDescendant(withText(fileName))));
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

    public static void createFile(final String fileName, final int inputRawResources) {
        while (true) {
            try {
                String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                File file = new File(extStorageDirectory, fileName);

                final OutputStream outputStream = new FileOutputStream(file);

                final Resources resources = context.getResources();
                final byte[] largeBuffer = new byte[1024 * 4];
                int bytesRead;

                final InputStream inputStream = resources.openRawResource(inputRawResources);
                while ((bytesRead = inputStream.read(largeBuffer)) > 0) {
                    if (largeBuffer.length == bytesRead) {
                        outputStream.write(largeBuffer);
                    } else {
                        final byte[] shortBuffer = new byte[bytesRead];
                        System.arraycopy(largeBuffer, 0, shortBuffer, 0, bytesRead);
                        outputStream.write(shortBuffer);
                    }
                }
                inputStream.close();


                outputStream.flush();
                outputStream.close();
                device.waitForIdle();
                waitUntilIdle();
                intending(not(isInternal()))
                        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, insertFileIntoIntentAsData(fileName)));
                return;
            } catch (Exception ex) {
                Timber.i("Cannot insert file as data");
            }
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

    private static Intent insertFileIntoIntentAsData(String fileName) {
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
        try {
            selectRemoveAccount();
            clickAcceptButton();
        } catch (Exception ex) {
        }
    }

    public void goBackAndRemoveAccount() {
        goBackAndRemoveAccount(false);
    }

    public void goBackAndRemoveAccount(boolean discardMessage) {
        Activity currentActivity = getCurrentActivity();
        while (true) {
            try {
                device.waitForIdle();
                removeLastAccount();
                return;
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
                currentActivity = getCurrentActivity();
            }
        }
    }

    public void clickAcceptButton() {
        device.waitForIdle();
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.okay_action)).perform(click());
        device.waitForIdle();
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
            } else {
                device.waitForIdle();
                device.pressBack();
                device.waitForIdle();
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

    public void clickAttachedFiles(int total) {
        BySelector selector = By.clazz("android.widget.FrameLayout");
        Activity sentFolderActivity = getCurrentActivity();
        /*intending((isInternal()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));*/
        int position;
        for (int start = 0; start < total; start++) {
            int size = device.findObjects(selector).size();
            while (size == 0) {
                size = device.findObjects(selector).size();
            }
            UiObject2 uiObject = device.findObject(By.res("security.pEp:id/attachment"));
            position = -1;
            for (UiObject2 frameLayout : device.findObjects(selector)) {
                device.waitForIdle();
                try {
                    if (frameLayout.getResourceName().equals(uiObject.getResourceName())) {
                        position++;
                    }
                    if (start == position) {
                        frameLayout.longClick();
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        while (getCurrentActivity() != sentFolderActivity) {
                            device.waitForIdle();
                            device.pressBack();
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot read attached files");
                    break;
                }
            }
        }
    }

    public void longClick(String viewId) {
        UiObject2 list = device.findObject(By.res(APP_ID, viewId));
        Rect bounds = list.getVisibleBounds();
        device.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 450);
    }

    public void removeTextFromTextView(String viewId) {
        device.waitForIdle();
        int view = intToID(viewId);
        onView(withId(view)).perform(closeSoftKeyboard());
        onView(withId(view)).perform(click());
        UiObject2 list = device.findObject(By.res(APP_ID, viewId));
        Rect bounds = list.getVisibleBounds();
        device.click(bounds.left - 1, bounds.centerY());
        while (!(hasValueEqualTo(onView(withId(view)), " ")
                || hasValueEqualTo(onView(withId(view)), ""))) {
            try {
                device.waitForIdle();
                device.waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                device.waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                device.waitForIdle();
            } catch (Exception ex) {
                Timber.i("Cannot remove text from field " + viewId + ": " + ex.getMessage());
            }
        }
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
        assertMessageStatus(rating.value);
    }

    public void assertMessageStatus(int status){
        device.waitForIdle();
        if (!exists(onView(withId(R.id.reply_message)))) {
            device.waitForIdle();
            doWaitForResource(R.id.pEp_indicator);
            waitUntilIdle();
            device.waitForIdle();
            clickView(R.id.pEp_indicator);
            while (!exists(onView(withId(R.id.pEpTitle)))) {
                doWaitForResource(R.id.pEpTitle);
                device.waitForIdle();
            }
        } else {
            clickView(R.id.tvPep);
        }
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, status))));
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
            waitUntilIdle();
            device.waitForIdle();
            if (exists(onView(withId(R.id.toolbar))) && viewIsDisplayed(R.id.toolbar)) {
                device.waitForIdle();
                onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(color)));
                toolbarExists = true;
            }
        }
    }

    public void selectFromMenu(int viewId){
        device.waitForIdle();
        while (true) {
            try {
                openOptionsMenu();
                selectFromScreen(viewId);
                device.waitForIdle();
                if (!viewIsDisplayed(R.id.text1)) {
                    return;
                }
            } catch (Exception ex) {
                Timber.i("Toolbar is not closed yet");
            }
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

    public boolean textExistsOnScreen (String text) {
        boolean viewExists = false;
        device.waitForIdle();
        BySelector selector = By.clazz("android.view.View");
        while (!viewExists) {
            for (UiObject2 view : device.findObjects(selector)) {
                if (view.getText() != null) {
                    viewExists = true;
                    if (view.getText().contains(text)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        while (true) {
            try {
                openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
                device.waitForIdle();
                return;
            } catch (Exception ex) {
                Timber.i("Cannot open menu");
            }
        }
    }

    public void selectFromScreen(int resource) {
        BySelector selector = By.clazz("android.widget.TextView");
        try {
            while (true) {
                for (UiObject2 object : device.findObjects(selector)) {
                    if (object.getText().contains(resources.getString(resource))) {
                        device.waitForIdle();
                        object.click();
                        device.waitForIdle();
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            Timber.i("Cannot find text on screen: " + ex);
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

    private void doWaitForIdlingListViewResource(int resource){
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
        selectFromMenu(R.string.account_settings_folders);
        device.waitForIdle();
        String folder = resources.getString(R.string.special_mailbox_name_sent);
        while (true) {
            for (UiObject2 textView : device.findObjects(textViewSelector)) {
                try {
                    if (textView.findObject(textViewSelector).getText() != null && textView.findObject(textViewSelector).getText().contains(folder)) {
                        textView.findObject(textViewSelector).click();
                        return;
                    }
                    device.waitForIdle();
                } catch (Exception e) {
                    Timber.i("View is not sent folder");
                }
            }
        }
    }

    private void waitForTextOnScreen(String text) {
        boolean textIsOk = false;
        do {
            device.waitForIdle();
            try {
                textIsOk = getTextFromTextViewThatContainsText(text).contains(resources.getString(R.string.special_mailbox_name_sent));
            } catch (Exception e) {
                Timber.i("Text is not on the screen");
            }
        } while (!textIsOk);
    }

    public void clickLastMessageReceived() {
        Timber.i("MessageList antes: " + messageListSize[0] + " " + messageListSize[1]);
        boolean messageClicked = false;
        if (messageListSize[0] > messageListSize [1]) {
            waitForNewMessage();
        }
        Timber.i("MessageList despues: " + messageListSize[0] + " " + messageListSize[1]);
        while (!messageClicked){
            device.waitForIdle();
            if (!viewIsDisplayed(R.id.reply_message)) {
                try {
                    swipeDownMessageList();
                    device.waitForIdle();
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
                device.waitForIdle();
            } else {
                messageClicked = true;
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
        if (viewIsDisplayed(R.id.delete)) {
            pressBack();
            device.waitForIdle();
        }
        getMessageListSize();
    }

    public void getMessageListSize() {
        while (true) {
            try {
                onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 0));
                return;
            } catch (Exception ex) {
                Timber.i("Cannot find view message_list: " + ex.getMessage());
            }
        }
    }

    public void swipeDownMessageList(){
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

    private void removeMessagesFromList(){
        getMessageListSize();
        if (messageListSize[0] != 1) {
            clickFirstMessage();
            boolean emptyList = false;
            while (!emptyList) {
                try {
                    device.waitForIdle();
                    onView(withText(R.string.cancel_action)).perform(click());
                } catch (NoMatchingViewException ignoredException) {
                    Timber.i("Ignored exception");
                }
                try {
                    device.waitForIdle();
                    onView(withId(R.id.delete)).perform(click());
                } catch (NoMatchingViewException ignoredException) {
                    emptyList = true;
                }
                device.waitForIdle();
                if (exists(onView(withId(android.R.id.message)))) {
                    emptyList = false;
                }
            }
        }
    }

    public void clickFirstMessage(){
        boolean firstMessageClicked = false;
        device.waitForIdle();
        //doWaitForResource(R.id.message_list);
        while (!firstMessageClicked){
            try{
                if(viewIsDisplayed(R.id.message_list)) {
                    doWaitForIdlingListViewResource(R.id.message_list);
                    device.waitForIdle();
                    swipeDownMessageList();
                    device.waitForIdle();
                    getMessageListSize();
                    if (viewIsDisplayed(R.id.reply_message)) {
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
                } else {
                    if (!exists(onView(withId(R.id.message_list)))) {
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

    public static class TestConfig {
        String mail;
        String password;
        String username;
        String imap_server;
        String smtp_server;
        String imap_port;
        String smtp_port;

        TestConfig(){
            this.mail = "";
            this.password = "";
            this.username = "";
            this.imap_server = "";
            this.smtp_server = "";
            this.imap_port = "";
            this.smtp_port = "";
        }

        public void setMail(String mail) { this.mail = mail;}
        void setPassword(String password) { this.password = password;}
        void setUsername(String username) { this.username = username;}
        void setImap_server(String imap_server) { this.imap_server = imap_server;}
        void setSmtp_server(String smtp_server) { this.smtp_server = smtp_server;}
        void setImap_port(String imap_port) { this.imap_port = imap_port;}
        void setSmtp_port(String smtp_port) { this.smtp_port = smtp_port;}

        String getMail() { return mail;}
        String getPassword() { return password;}
        String getUsername() { return username;}
        String getImap_server() { return imap_server;}
        String getSmtp_server() { return smtp_server;}
        String getImap_port() { return imap_port;}
        String getSmtp_port() { return smtp_port;}
    }
}
