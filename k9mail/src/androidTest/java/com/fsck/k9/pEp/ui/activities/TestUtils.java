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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.swipeDown;
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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


class TestUtils {

    private static final String APP_ID = BuildConfig.APPLICATION_ID;
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";

    private UiDevice device;
    private Context context;
    private Resources resources;
    private BySelector textViewSelector;
    private String messageReceivedDate[];
    private int lastMessageReceivedPosition;
    private int messagesToRead;

    TestUtils(UiDevice device) {
        this.device = device;
        context = InstrumentationRegistry.getTargetContext();
        resources = context.getResources();
        textViewSelector = By.clazz("android.widget.TextView");
        messagesToRead = 6;
        messageReceivedDate = new String[messagesToRead];
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

    void newEmailAccount() {
        onView(withId(R.id.account_email)).perform(typeText(getEmail()));
        onView(withId(R.id.account_password)).perform(typeText(getPassword()), closeSoftKeyboard());
        onView(withId(R.id.manual_setup)).perform(click());
        fillImapData();
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
        fillSmptData();
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
    }

    void gmailAccount() {
        onView(withId(R.id.account_oauth2)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
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
        device.waitForIdle();
        onView(withId(R.id.done)).perform(click());
    }

    void composeMessageButton() {
        onView(withId(R.id.fab_button_compose_message)).perform(click());
    }

    void createAccount(boolean isGmail) {
        try{
            onView(withId(R.id.skip)).check(matches(isDisplayed()));
            onView(withId(R.id.skip)).perform(click());
            if (isGmail) {
                gmailAccount();
            } else {
                newEmailAccount();
            }
            accountDescription(DESCRIPTION, USER_NAME);
        }catch (Exception ex){

        }
    }

    String getAccountDescription() {
        return DESCRIPTION;
    }

    void fillMessage(BasicMessage inputMessage, boolean attachFilesToMessage) {
        doWait("to");
        device.waitForIdle();
        device.findObject(By.res(APP_ID, "to")).longClick();
        device.waitForIdle();
        device.pressKeyCode(KeyEvent.KEYCODE_DEL);
        device.waitForIdle();
        onView(withId(R.id.to)).perform(typeText(inputMessage.getTo()), closeSoftKeyboard());
        device.findObject(By.res(APP_ID, "subject")).click();
        device.findObject(By.res(APP_ID, "subject")).setText(inputMessage.getSubject());
        device.findObject(By.res(APP_ID, "message_content")).click();
        device.findObject(By.res(APP_ID, "message_content")).setText(inputMessage.getMessage());
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
        doWaitForResource(R.id.send);
        onView(withId(R.id.send)).perform(click());
    }

    void pressBack() {
        device.pressBack();
    }

    void removeLastAccount() {
        device.waitForIdle();
        doWait("accounts_list");
        device.waitForIdle();
        longClick("accounts_list");
        device.waitForIdle();
        selectRemoveAccount();
        device.waitForIdle();
        clickAcceptButton();
    }

    void clickAcceptButton() {
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.okay_action)).perform(click());
    }

    void clickCancelButton() {
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.cancel_action)).perform(click());
    }

    public void doWaitForObject(String object) {
        boolean finish = false;
        do {
            if (device.findObject(By.clazz(object)) != null) {
                finish = true;
            }
        } while (!finish);
    }

    private void selectRemoveAccount() {
        BySelector selector = By.clazz("android.widget.TextView");
        int size = device.findObjects(selector).size();
        while (size == 0) {
            device.waitForIdle();
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
        device.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 180);
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

    public void checkStatus(Rating rating) {
        device.waitForIdle();
        onView(withId(R.id.pEp_indicator)).perform(click());
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, rating.value))));
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
        openActionBarOverflowOrOptionsMenu(context);
    }

    void selectFromMenu(int resource) {
        BySelector selector = By.clazz("android.widget.TextView");
        device.waitForIdle();
        for (UiObject2 object : device.findObjects(selector)) {
            if (object.getText().equals(resources.getString(resource))) {
                object.click();
                break;
            }
        }
    }

    void doWait() {
        device.waitForIdle();
    }

    void doWait(String viewId) {
        UiObject2 waitForView = device
                .wait(Until.findObject(By.res(APP_ID, viewId)),
                        150000);
        assertThat(waitForView, notNullValue());
    }

    void doWaitForResource(int resource) {
        device.wait(Until.hasObject(By.desc(resources.getString(resource))), 1);
    }

    void doWaitForAlertDialog(IntentsTestRule<SplashActivity> intent, int displayText) {
        onView(withId(intent.getActivity().getResources()
                .getIdentifier("alertTitle", "id", "android")))
                .inRoot(isDialog())
                .check(matches(withText(displayText)))
                .check(matches(isDisplayed()));
    }

    public String getResourceString(int id, int position) {
        return resources.getStringArray(id)[position];
    }

    public void assertMessageStatus(int status) {
        device.waitForIdle();
        clickMessageStatus();
        device.waitForIdle();
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, status))));
    }

    public void clickMessageStatus() {
        device.waitForIdle();
        doWaitForResource(R.id.tvPep);
        device.waitForIdle();
        onView(withId(R.id.tvPep)).perform(click());
        device.waitForIdle();
    }

    public void clickLastMessageReceived() {
        device.waitForIdle();
        device.findObjects(textViewSelector).get(lastMessageReceivedPosition).click();
    }

    public void getLastMessageReceived() {
        device.waitForIdle();
        onView(withId(R.id.message_list))
                .perform(swipeDown());
        device.waitForIdle();
        lastMessageReceivedPosition = getLastMessageReceivedPosition();
        int size = device.findObjects(textViewSelector).size();
        int message = 0;
        if (lastMessageReceivedPosition != -1) {
            for (; (message < messagesToRead) && (lastMessageReceivedPosition + 1 + message * 3 < size); message++) {
                messageReceivedDate[message] = device.findObjects(textViewSelector).get(lastMessageReceivedPosition + 1 + message * 3).getText();
            }
        } else {
            for (; message < messagesToRead; message++) {
                messageReceivedDate[message] = "";
            }
            lastMessageReceivedPosition = device.findObjects(textViewSelector).size();
        }
    }

    public int getLastMessageReceivedPosition() {
        int size = device.findObjects(textViewSelector).size();
        for (int position = 0; position < size; position++) {
            String textAtPosition = device.findObjects(textViewSelector).get(position).getText();
            if (textAtPosition != null && textAtPosition.contains("@")) {
                position++;
                while (device.findObjects(textViewSelector).get(position).getText() == null) {
                    position++;
                    if (position >= size) {
                        return -1;
                    }
                }
                return position;
            }
        }
        return size;
    }

    public void waitForMessageWithText(String textInMessage, String preview) {
        boolean messageSubject;
        boolean messagePreview;
        boolean emptyMessageList;
        emptyMessageList = device.findObjects(textViewSelector).size() <= lastMessageReceivedPosition;
        if (!emptyMessageList) {
            do {
                boolean newMessage = false;
                do {
                    device.waitForIdle();
                    int size = device.findObjects(textViewSelector).size();
                    for (int message = 0; (message < messagesToRead) && (lastMessageReceivedPosition + 1 + message * 3 < size); message++) {
                        if (!(device.findObjects(textViewSelector).get(lastMessageReceivedPosition + 1 + message * 3).getText())
                                .equals(messageReceivedDate[message])) {
                            newMessage = true;
                            break;
                        }
                    }
                } while (!newMessage);
                messageSubject = getTextFromTextViewThatContainsText(textInMessage)
                        .equals(device.findObjects(textViewSelector).get(lastMessageReceivedPosition).getText());
                messagePreview = getTextFromTextViewThatContainsText(preview)
                        .equals(device.findObjects(textViewSelector).get(lastMessageReceivedPosition + 2).getText());
            } while (!(messageSubject && messagePreview));
        } else {
            while (emptyMessageList) {
                emptyMessageList = device.findObjects(textViewSelector).size() <= lastMessageReceivedPosition;
            }
        }
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
    private String getEmailServer() {
        return BuildConfig.PEP_TEST_EMAIL_SERVER;
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

        BasicMessage(String from, String message, String subject, String to) {
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

    private class SameStatusdentities {
        List<String> addresses;
        Rating rating;
    }
}
