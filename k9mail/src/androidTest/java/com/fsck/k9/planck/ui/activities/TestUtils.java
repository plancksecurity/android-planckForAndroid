package com.fsck.k9.planck.ui.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.Root;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.common.GetListSizeAction;
import com.fsck.k9.planck.PlanckColorUtils;
import com.fsck.k9.planck.PlanckUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import foundation.pEp.jniadapter.Rating;
import security.planck.ui.PlanckUIUtils;
import security.planck.ui.support.export.ExportPlanckSupportDataPresenterKt;
import timber.log.Timber;

import static android.content.ContentValues.TAG;
import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Checks.checkNotNull;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.runner.lifecycle.Stage.RESUMED;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.appendTextInTextView;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.getTextFromView;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.hasValueEqualTo;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.saveSizeInInt;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.valuesAreEqual;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.viewIsDisplayed;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.viewWithTextIsDisplayed;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.waitUntilIdle;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withRecyclerView;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withTextColor;
import static java.lang.Thread.sleep;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static foundation.pEp.jniadapter.Rating.pEpRatingMistrust;
import static org.junit.Assert.fail;

import junit.framework.AssertionFailedError;


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
    private int[] messageListSize = new int[2];
    private int totalAccounts = -1;
    private int account = 0;

    public static final long TIMEOUT_TEST = FIVE_MINUTES * MINUTE_IN_SECONDS * SECOND_IN_MILIS;
    private TestConfig testConfig;
    public String[] botList;
    public boolean testReset = false;
    public static JSONObject json;
    public static JSONArray jsonArray;
    public static String rating;
    public String trustWords = "nothing";
    private String emailForDevice;
    private static final String HOST = "@sq.planck.security";
    private Connection connection;


    public TestUtils(UiDevice device, Instrumentation instrumentation) {
        TestUtils.device = device;
        this.instrumentation = instrumentation;
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        resources = context.getResources();
    }

    public static Context getContext() {
        return context;
    }

    public void increaseTimeoutWait() {
        long waitingTime = DateUtils.SECOND_IN_MILLIS * 500;
        IdlingPolicies.setMasterPolicyTimeout(waitingTime, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTime, TimeUnit.MILLISECONDS);
    }

    private String getLauncherPackageName() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    private void automaticAccount() {
        trustedServer();
    }

    private void trustedServer() {
        /*if (testConfig.getTrusted_server(account)) {
            waitForIdle();
            clickView(R.id.manual_setup);
            waitForIdle();
            while (true) {
                try {
                    while (exists(onView(withId(R.id.account_trust_server)))) {
                        setCheckBox("Trust server", true);
                        waitForIdle();
                        onView(withId(R.id.next)).perform(click());
                        waitForIdle();
                        return;
                    }
                    waitForIdle();
                    onView(withId(R.id.next)).perform(click());
                    waitForIdle();
                } catch (Exception eNext) {
                    Timber.i("Trust server not enabled yet");
                }
            }
        } else {
            clickNextButton();
        }*/
    }

    private void clickNextButton() {
        waitForIdle();
        onView(withId(R.id.next)).perform(click());
        for (int i = 0; i < 100; i++) {
            waitForIdle();
        }
        while (viewIsDisplayed(R.id.account_email)) {
            waitForIdle();
        }
        while (exists(onView(withId(R.id.parentPanel)))) {
            pressOKButtonInDialog();
            waitForIdle();
        }
        try {
            UiObject2 uiObject = device.findObject(By.res(BuildConfig.APPLICATION_ID + ":id/alertTitle"));
            while (uiObject.getText() != null) {
                pressBack();
                waitForIdle();
                onView(withId(R.id.next)).perform(click());
                waitForIdle();
                uiObject = device.findObject(By.res(BuildConfig.APPLICATION_ID + ":id/alertTitle"));
            }
        } catch (Exception ex) {
            Timber.i("Doesn't exist popup alert message");
        }
    }

    private void fillAccountAddress(String accountAddress) {
        if (!getTextFromView(onView(withId(R.id.account_email))).equals(accountAddress)) {
            removeTextFromTextView(R.id.account_email);
        }
        while (getTextFromView(onView(withId(R.id.account_email))).equals("")) {
            try {
                waitForIdle();
                onView(withId(R.id.account_email)).perform(click());
                waitForIdle();
                onView(withId(R.id.account_email)).perform(typeText(accountAddress), closeSoftKeyboard());
            } catch (Exception ex) {
                Timber.i("Cannot fill account email: " + ex.getMessage());
            }
        }
    }

    private void gmailAccount() {
        //onView(withId(R.id.account_oauth2)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.next)).perform(click());
        waitForIdle();
        onView(withId(R.id.next)).perform(click());
        waitForIdle();
        onView(withId(R.id.next)).perform(click());
    }

    private void accountDescription(String description, String userName, boolean isSync) {
        doWaitForResource(R.id.account_description);
        while (!viewIsDisplayed(R.id.account_description)) {
            waitForIdle();
        }
        while (getTextFromView(onView(withId(R.id.account_description))).equals("")) {
            try {
                waitForIdle();
                onView(withId(R.id.account_description)).perform(click());
                waitForIdle();
                if (!description.equals("")) {
                    onView(withId(R.id.account_description)).perform(typeText(description), closeSoftKeyboard());
                } else {
                    onView(withId(R.id.account_description)).perform(typeText("TEST"), closeSoftKeyboard());
                }
                waitForIdle();
            } catch (Exception ex) {
                Timber.i("Cannot find account description field");
            }
        }
        while (getTextFromView(onView(withId(R.id.account_name))).equals("")) {
            try {
                waitForIdle();
                onView(withId(R.id.account_name)).perform(click());
                waitForIdle();
                if (!userName.equals("")) {
                    onView(withId(R.id.account_name)).perform(typeText(userName), closeSoftKeyboard());
                } else {
                    onView(withId(R.id.account_name)).perform(typeText("USER"), closeSoftKeyboard());
                }
                waitForIdle();
            } catch (Exception ex) {
                Timber.i("Cannot find account name field");
            }
        }
        if (testConfig.test_number.equals("3") || !isSync) {
            onView(withId(R.id.pep_enable_sync_account)).perform(click());
        }
        waitForIdle();
        onView(withId(R.id.done)).perform(click());
    }

    public void composeMessageButton() {
        waitForIdle();
        getMessageListSize();
        clickView(R.id.fab_button_compose_message);
        waitForIdle();
        onView(withId(R.id.to)).perform(closeSoftKeyboard());
        waitForIdle();
    }

    void goBackToMessageCompose() {
        boolean backToMessageCompose = false;
        while (!backToMessageCompose) {
            pressBack();
            waitForIdle();
            if (exists(onView(withId(R.id.send)))) {
                backToMessageCompose = true;
            }
        }
    }

    public void clickView(int viewId) {
        boolean buttonClicked = false;
        while (!viewIsDisplayed(viewId)) {
            waitForIdle();
        }
        doWaitForResource(viewId);
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
        while (!buttonClicked) {
            if (exists(onView(withId(viewId))) || viewIsDisplayed(viewId)) {
                waitForIdle();
                try {
                    onView(withId(viewId)).check(matches(isDisplayed()));
                    onView(withId(viewId)).perform(click());
                    waitForIdle();
                    buttonClicked = true;
                    Timber.i("View found, can click it");
                } catch (Exception ex) {
                    Timber.i("View not found, cannot click it: " + ex);
                }
            } else {
                buttonClicked = true;
            }
        }
    }

    private void assertCurrentActivityIsInstanceOf(Class<? extends Activity> activityClass) {
        Activity currentActivity = getCurrentActivity();
        checkNotNull(currentActivity);
        checkNotNull(activityClass);
        assertTrue(currentActivity.getClass().isAssignableFrom(activityClass));
    }

    public static Activity getCurrentActivity() {

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

    public void createAccount() {
        createNewAccountWithPermissions();
        if (test_number().equals("0")) {
            getMessageListSize();
        }
    }

    public String getAccountEmailForDevice() {
        if (emailForDevice != null) return emailForDevice;
        String out = "error: email for device not initialized";
        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File directory = new File(downloadsDirectory.getAbsolutePath() + File.separator + "test");
        File configFile = new File(directory, "test_config.txt");
        if (!configFile.exists()) return BuildConfig.PLANCK_TEST_EMAIL_ADDRESS;

        FileInputStream fin;
        if (configFile.canRead()) {
            try {
                fin = new FileInputStream(configFile);
                InputStreamReader inputStreamReader = new InputStreamReader(fin);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                while ((receiveString = bufferedReader.readLine()) != null && !receiveString.contains("mail")) {
                    Timber.v("Searching for test email address for device...");
                }
                fin.close();
                bufferedReader.close();
                if (receiveString != null && !receiveString.isEmpty()) {
                    String[] line = receiveString.split(" = ");
                    out = emailForDevice = line[1];
                }
            } catch (Exception e) {
                Timber.e(e, "could not read from file %s", configFile);
                out = e.getMessage();
            }
        }
        return out;
    }

    public void moveFile(File file, File dir) throws IOException {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File newFile = new File(dir, file.getName());
        Files.move(file.toPath(), newFile.toPath(), REPLACE_EXISTING);
    }

    public void readConfigFile() {
        testConfig = new TestConfig();
        String[] strSplit = new String[0];
        String[] line = new String[2];
        boolean configFileReaded = false;
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (manufacturer.equals("Google")&& !model.equals("Pixel 3 XL")) {
            model = "default";
        }
        while (!configFileReaded) {
            try {
                InputStream is = getInstrumentation().getContext().getAssets().open("features/test_config/" + model + ".txt");
                String str = IOUtils.toString(is);
                strSplit = str.split("\n");
                configFileReaded = true;
            } catch (IOException e) {
                model = "default";
                Timber.i("Cannot find config file for " + model + ", using default file");
            }
        }
        for (int readingLine = 0; readingLine < strSplit.length; readingLine++) {
            line = strSplit[readingLine].split(" = ");
            switch (line[0]) {
                case "mail":
                    testConfig.setMail(line[1], 0);
                    if (!testConfig.getMail(0).equals("")) {
                        totalAccounts = 1;
                    }
                    break;
                case "password":
                    testConfig.setPassword(line[1], 0);
                    break;
                case "username":
                    testConfig.setUsername(line[1], 0);
                    break;
                case "trusted_server":
                    if (line[1].equals("true")) {
                        testConfig.setTrusted_server(true, 0);
                    } else if (line[1].equals("false")) {
                        testConfig.setTrusted_server(false, 0);
                    } else {
                        fail("Trusted_server must be true or false");
                    }
                    break;
                case "imap_server":
                    testConfig.setImap_server(line[1], 0);
                    break;
                case "smtp_server":
                    testConfig.setSmtp_server(line[1], 0);
                    break;
                case "imap_port":
                    testConfig.setImap_port(line[1], 0);
                    break;
                case "smtp_port":
                    testConfig.setSmtp_port(line[1], 0);
                    break;
                case "mail2":
                    testConfig.setMail(line[1], 1);
                    if (!testConfig.getMail(1).equals("")) {
                        totalAccounts = 2;
                    }
                    break;
                case "password2":
                    testConfig.setPassword(line[1], 1);
                    if (testConfig.getPassword(1).equals("") && !testConfig.getMail(1).equals("")) {
                        fail("Password is empty");
                    }
                    break;
                case "username2":
                    testConfig.setUsername(line[1], 1);
                    break;
                case "trusted_server2":
                    if (line[1].equals("true")) {
                        testConfig.setTrusted_server(true, 1);
                    } else if (line[1].equals("false")) {
                        testConfig.setTrusted_server(false, 1);
                    } else {
                        fail("Trusted_server must be true or false");
                    }
                    break;
                case "imap_server2":
                    testConfig.setImap_server(line[1], 1);
                    break;
                case "smtp_server2":
                    testConfig.setSmtp_server(line[1], 1);
                    break;
                case "imap_port2":
                    testConfig.setImap_port(line[1], 1);
                    break;
                case "smtp_port2":
                    testConfig.setSmtp_port(line[1], 1);
                    break;
                case "mail3":
                    testConfig.setMail(line[1], 2);
                    if (!testConfig.getMail(2).equals("")) {
                        totalAccounts = 3;
                    }
                    break;
                case "password3":
                    testConfig.setPassword(line[1], 2);
                    if (testConfig.getPassword(2).equals("") && !testConfig.getMail(2).equals("")) {
                        fail("Password is empty");
                    }
                    break;
                case "username3":
                    testConfig.setUsername(line[1], 2);
                    break;
                case "trusted_server3":
                    if (line[1].equals("true")) {
                        testConfig.setTrusted_server(true, 2);
                    } else if (line[1].equals("false")) {
                        testConfig.setTrusted_server(false, 2);
                    } else {
                        fail("Trusted_server must be true or false");
                    }
                    break;
                case "imap_server3":
                    testConfig.setImap_server(line[1], 2);
                    break;
                case "smtp_server3":
                    testConfig.setSmtp_server(line[1], 2);
                    break;
                case "imap_port3":
                    testConfig.setImap_port(line[1], 2);
                    break;
                case "smtp_port3":
                    testConfig.setSmtp_port(line[1], 2);
                    break;
                case "keysync_account_1":
                    testConfig.setKeySync_account(line[1], 0);
                    break;
                case "keysync_password_1":
                    testConfig.setKeySync_password(line[1], 0);
                    break;
                case "keysync_account_2":
                    testConfig.setKeySync_account(line[1], 1);
                    break;
                case "keysync_password_2":
                    testConfig.setKeySync_password(line[1], 1);
                    break;
                case "test_number":
                    boolean fileExists = false;
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test/");
                    for (int i = 0; i < 20; i++) {
                        try {
                            if (new File(directory, i + ".txt").exists()) {
                                testConfig.settest_number(String.valueOf(i));
                                fileExists = true;
                                break;
                            }
                        } catch (Exception e) {
                            Timber.i("Cannot find file " + i + ".txt");
                        }
                    }
                    if (!fileExists) {
                        Timber.i("Cannot find the file for test_number. Using the test_number in the config_file");
                        testConfig.settest_number(line[1]);
                    }
                    if (!testConfig.gettest_number().equals("0")) {
                        totalAccounts = 1;
                        if (testConfig.gettest_number().equals("3")) {
                            totalAccounts = 2;
                        }
                    }
                    break;
                case "passphrase_account_1":
                    testConfig.setPassphrase_account(line[1], 0);
                    break;
                case "passphrase_password_1":
                    testConfig.setPassphrase_password(line[1], 0);
                    break;
                case "passphrase_account_2":
                    testConfig.setPassphrase_account(line[1], 1);
                    break;
                case "passphrase_password_2":
                    testConfig.setPassphrase_password(line[1], 1);
                    break;
                case "passphrase_account_3":
                    testConfig.setPassphrase_account(line[1], 2);
                    break;
                case "passphrase_password_3":
                    testConfig.setPassphrase_password(line[1], 2);
                    break;
                case "format_test_account":
                    testConfig.setFormat_test_account(line[1]);
                    break;
                case "format_test_password":
                    testConfig.setFormat_test_password(line[1]);
                    break;
                default:
                    break;
            }
            if (BuildConfig.IS_OFFICIAL) {
                totalAccounts = 1;
            }
        }
    }

    public void syncDevices() {
        waitForSyncPopUp();
        onView(withId(R.id.afirmativeActionButton)).perform(click());
        waitForIdle();
        onView(withId(R.id.show_long_trustwords)).perform(click());
        waitForIdle();
        setTrustWords(getTextFromView(onView(withId(R.id.trustwords))));
        onView(withId(R.id.afirmativeActionButton)).perform(click());
        waitForIdle();
        while (!viewIsDisplayed(R.id.loading)) {
            waitForIdle();
        }
        while (viewIsDisplayed(R.id.loading)) {
            waitForIdle();
        }
        if (!viewIsDisplayed(R.id.afirmativeActionButton)) {
            fail("Cannot sync devices");
        } else {
            onView(withId(R.id.afirmativeActionButton)).perform(click());
            waitForIdle();
        }
    }

    public void waitForSyncPopUp() {
        waitForIdle();
        Espresso.onIdle();
        while (!viewIsDisplayed(R.id.main_container) || !viewIsDisplayed(R.id.afirmativeActionButton)) {
            waitForIdle();
            Espresso.onIdle();
        }
    }

    public void checkSyncIsWorking_FirstDevice() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        waitForIdle();
        getMessageListSize();
        waitForIdle();
        composeMessageButton();
        waitForIdle();
        fillMessage(new TestUtils.BasicMessage("",
                        "SyncFirstDevice",
                        trustWords,
                        getKeySyncAccount(0)),
                false);
        while (exists(onView(withId(R.id.send)))) {
            clickView(R.id.send);
        }
        waitForNewMessage();
        waitForMessageAndClickIt();
        compareMessageBodyWithText(trustWords);
        pressBack();
    }

    public void checkIsNotProtected_FirstDevice() {
        getMessageListSize();
        composeMessageButton();
        fillMessage(new TestUtils.BasicMessage("",
                        "FirstDevice",
                        "Account is not protected",
                        getKeySyncAccount(0)),
                false);
        while (exists(onView(withId(R.id.send)))) {
            clickView(R.id.send);
        }
        waitForNewMessage();
        waitForMessageAndClickIt();
        compareMessageBodyWithText("Account is not protected");
        pressBack();
    }

    public void checkSyncIsWorking_SecondDevice() {
        waitForIdle();
        getMessageListSize();
        waitForIdle();
        waitForMessageAndClickIt();
        compareMessageBodyWithText(trustWords);
        pressBack();
        composeMessageButton();
        fillMessage(new TestUtils.BasicMessage("",
                        "SyncSecondDevice",
                        trustWords,
                        getKeySyncAccount(0)),
                false);
        while (exists(onView(withId(R.id.send)))) {
            clickView(R.id.send);
        }
        waitForNewMessage();
    }

    public void checkIsNotProtected_SecondDevice() {
        getMessageListSize();
        waitForMessageAndClickIt();
        compareMessageBodyWithText("Account is not protected");
        pressBack();
        composeMessageButton();
        fillMessage(new TestUtils.BasicMessage("",
                        "SecondDevice",
                        "Account is not protected",
                        getKeySyncAccount(0)),
                false);
        while (exists(onView(withId(R.id.send)))) {
            clickView(R.id.send);
        }
        waitForNewMessage();
    }

    public void checkSyncIsNotWorking_FirstDevice() {
        getMessageListSize();
        composeMessageButton();
        fillMessage(new TestUtils.BasicMessage("",
                        "NotSync_FirstDevice",
                        "This should be encrypted",
                        getKeySyncAccount(0)),
                false);
        while (exists(onView(withId(R.id.send)))) {
            clickView(R.id.send);
        }
        waitForNewMessage();
        if (waitForMessageAndClickIt()) {
            pressBack();
        } else {
            fail("Failed checking 1st devices is not sync");
        }
    }

    public void checkSyncIsNotWorking_SecondDevice() {
        getMessageListSize();
        if (waitForMessageAndClickIt()) {
            pressBack();
        } else {
            fail("Failed checking 2nd devices is not sync");
        }
        getMessageListSize();
        composeMessageButton();
        fillMessage(new TestUtils.BasicMessage("",
                        "NotSync_SecondDevice",
                        "This should be encrypted",
                        getKeySyncAccount(0)),
                false);
        while (exists(onView(withId(R.id.send)))) {
            clickView(R.id.send);
        }
        waitForNewMessage();
    }

    public void disableKeySync() {
        selectFromMenu(R.string.prefs_title);
        selectFromScreen(stringToID("privacy_preferences"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        selectFromScreen(stringToID("pep_sync"));
        selectButtonFromScreen(stringToID("keysync_disable_warning_action_disable"));
    }

    public void enableAccountGlobalKeySync() {
        selectFromMenu(R.string.prefs_title);
        goToTheAccountSettings(1);
        selectFromScreen(stringToID("privacy_preferences"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        clickTextOnScreen(stringToID("pep_sync_enable_account"));
        pressBack();
        pressBack();
        selectFromScreen(stringToID("privacy_preferences"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        clickTextOnScreen(stringToID("pep_sync"));
    }

    public void enableKeySync() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        selectFromMenu(R.string.prefs_title);
        selectFromScreen(stringToID("privacy_preferences"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        clickTextOnScreen(stringToID("pep_sync"));

    }

    public String test_number() {
        while (testConfig.gettest_number().equals("-10")) {
            readConfigFile();
        }
        return testConfig.gettest_number();
    }

    public boolean keySyncAccountsExist() {
        return testConfig.getKeySync_password(0) != null
                && testConfig.getKeySync_password(0) != null
                && testConfig.getKeySync_account(1) != null
                && testConfig.getKeySync_password(1) != null;
    }

    public void compareMessageBodyWithText(String cucumberBody) {
        waitForIdle();
        switch (cucumberBody) {
            case "empty":
                cucumberBody = "";
                break;
            case "longText":
                cucumberBody = longText();
                break;
            case "longWord":
                cucumberBody = longWord();
                break;
            case "specialCharacters":
                cucumberBody = specialCharacters();
                break;
            default:
                compareMessageBody(cucumberBody);
                break;
        }
        compareMessageBodyLongText(cucumberBody);
    }

    public void clearAllRecentApps () {
        waitForIdle();
        Intents.release();
        device.pressHome();
        waitForIdle();
        try {
            device.pressRecentApps();
            waitForIdle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        UiObject2 clear = device.findObject(By.res("com.sec.android.app.launcher:id/clear_all_button"));
        try {
            clear.click();
            waitForIdle();
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readBotList() {
        int millis = (int) System.currentTimeMillis();
        botList = new String[9];
        int position = 0;
        for (; position < botList.length; position++) {
            botList[position] = "bot" + position + millis;
        }
    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public int getAvailableAccounts() {
        int[] accounts = new int[1];
        onView(withId(R.id.accounts_list)).perform(saveSizeInInt(accounts, 0));
        return accounts[0];
    }

    private void createNewAccountWithPermissions() {
        testReset = false;
        boolean selectOptionDisplayed = false;
        try {
            if (exists(onView(withId(R.id.next)))) {
                onView(withId(R.id.next)).perform(click());
            }
            waitForIdle();
            try {
                if (exists(onView(withId(R.id.next)))) {
                    onView(withId(R.id.next)).perform(click());
                }
                waitForIdle();
                onView(withId(R.id.skip)).perform(click());
                waitForIdle();
            } catch (Exception ignoredException) {
                Timber.i("Ignored", "Ignored exception");
            }
            try {
                waitForIdle();
                onView(withId(R.id.action_continue)).perform(click());
                waitForIdle();
            } catch (Exception ignoredException) {
                Timber.i("Ignored", "Ignored exception");
            }
            allowPermissions();
            readConfigFile();
            while (exists(onView(withId(R.id.action_continue)))) {
                try {
                    onView(withId(R.id.action_continue)).perform(click());
                    waitForIdle();
                } catch (Exception ignoredException) {
                    Timber.i("Ignored", "Ignored exception");
                }
            }
            waitForIdle();
            if (exists(onView(withId(R.id.other_method_sign_in_button)))) {
                onView(withId(R.id.other_method_sign_in_button)).perform(click());
                selectOptionDisplayed = true;
                waitForIdle();
            }
            /*if (exists(onView(withId(R.id.microsoft_sign_in_button)))) {
                onView(withId(R.id.microsoft_sign_in_button)).perform(click());
                selectOptionDisplayed = true;
                waitForIdle();
            }
            if (exists(onView(withId(R.id.google_sign_in_button)))) {
                onView(withId(R.id.google_sign_in_button)).perform(click());
                selectOptionDisplayed = true;
                waitForIdle();
            }*/
            if (!selectOptionDisplayed) {
                fail("No sign in options to choose");
            }
            waitForIdle();
            switch (test_number()) {
                case "0":
                    createNAccounts(getTotalAccounts(), false, false);
                    break;
                case "1":
                case "2":
                    createNAccounts(getTotalAccounts(), true, false);
                    break;
                case "3":
                    createNAccounts(1, true, false);
                    break;
                case "4":
                    fillAccountAddress(testConfig.getPassphrase_account(0));
                    fillAccountPassword(testConfig.getPassphrase_password(0));
                    automaticAccount();
                    accountDescription("importKeyWithPassphrase", "Passphrase", true);
                    break;
                case "5":
                    fillAccountAddress(testConfig.getPassphrase_account(1));
                    fillAccountPassword(testConfig.getPassphrase_password(1));
                    automaticAccount();
                    accountDescription("importKeyWithPassphrase", "Passphrase", true);
                    break;
                case "6":
                    fillAccountAddress(testConfig.getPassphrase_account(2));
                    fillAccountPassword(testConfig.getPassphrase_password(2));
                    automaticAccount();
                    accountDescription("importKeyWithPassphrase", "Passphrase", true);
                    break;
                case "7":
                case "8":
                case "9":
                    fillAccountAddress(testConfig.getFormat_test_account());
                    fillAccountPassword(testConfig.getFormat_test_password());
                    automaticAccount();
                    accountDescription("formatTest", "Format Test", false);
                    break;
                default:
                    Timber.i("Test is not valid");
            }
        } catch (Exception ex) {
            if (!exists(onView(withId(R.id.accounts_list)))) {
                readConfigFile();
                Timber.i("Ignored", "Exists account, failed creating new one");
            }
        }
    }

    public void setTestNumber (int number) {
        testConfig.settest_number(String.valueOf(number));
    }

    public void clickHandShakeButton() {
        waitForIdle();
        if (exists(onView(withId(R.id.buttonHandshake)))) {
            onView(withId(R.id.buttonHandshake)).perform(click());
            waitForIdle();
        }
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
        waitForIdle();
    }

    public void goToHandshakeDialog() {
        waitForIdle();
        clickStatus();
        doWaitForResource(R.id.toolbar);
        clickHandShakeButton();
    }

    public void clickAfirmativeButton () {
        BySelector acceptButton = By.clazz("android.widget.Button");
        while (true) {
            for (UiObject2 object : device.findObjects(acceptButton)) {
                if (object.getResourceName() != null && object.getResourceName().equals("security.planck.test.enterprise.debug:id/afirmativeActionButton")) {
                    object.click();
                    waitForIdle();
                    return;
                }
            }
        }
    }

    public void clickNegativeButton () {
        BySelector acceptButton = By.clazz("android.widget.Button");
        while (true) {
            for (UiObject2 object : device.findObjects(acceptButton)) {
                if (object.getResourceName() != null && object.getResourceName().equals("security.planck.test.enterprise.debug:id/negativeActionButton")) {
                    object.click();
                    waitForIdle();
                    return;
                }
            }
        }
    }

    public void resetHandshake() {
        waitForIdle();
        try {
            onView(withId(R.id.recipientContainer)).perform(ViewActions.longClick());
            waitForIdle();
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ListView"));
            scroll.click();
        } catch (Exception ex) {
            Timber.e("Fail: " + ex.getMessage());
        }
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
    }

    public String getMessageBody () {
        String message = "";
        waitForIdle();
        BySelector selector = By.clazz("android.widget.FrameLayout");
        for (UiObject2 frameLayout : device.findObjects(selector)) {
            if (frameLayout.getResourceName() != null && frameLayout.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/message_container")) {
                message = frameLayout.getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(0).getText();
                message = message.substring(0, message.indexOf("\n"));
                break;
            }
        }
        return message;
    }

    public void createNAccounts(int n, boolean isKeySync, boolean isThirdSync) {
        try {
            for (; account < n; account++) {
                if (testConfig.getMail(account) == null || testConfig.getMail(account).equals("")) {
                    Timber.e("Is not possible to create more accounts, email address is empty");
                    return;
                }
                waitForIdle();
                while (exists(onView(withId(R.id.message_list)))) {
                    openOptionsMenu();
                    selectFromMenu(R.string.action_settings);
                    waitForIdle();
                }
                addAccount();
                if (isKeySync) {
                    int account = 0;
                    if (testConfig.test_number.equals("3") && !isThirdSync) {
                        account = 1;
                    }
                    fillAccountAddress(testConfig.getKeySync_account(account));
                    fillAccountPassword(testConfig.getKeySync_password(account));
                } else {
                    fillAccountAddress(testConfig.getMail(account));
                    fillAccountPassword(testConfig.getPassword(account));
                }
                if (BuildConfig.IS_OFFICIAL) {
                    automaticAccount();
                } else {
                    if (!(testConfig.getImap_server(account) == null) && !(testConfig.getSmtp_server(account) == null)) {
                        manualAccount();
                    } else {
                        automaticAccount();
                    }
                }
                try {
                    waitForIdle();
                    accountDescription(testConfig.getUsername(account), testConfig.getUsername(account), true);
                } catch (Exception e) {
                    Timber.i("Can not fill account description");
                }
            }
        } catch (Exception ex) {
            Timber.i("Ignored", "Exists account");
        }
    }

    public void addAccount() {
        try {
            swipeUpScreen();
            onView(withId(R.id.add_account_container)).perform(click());
            waitForIdle();
        } catch (Exception list) {
            Timber.i("Cannot add a new account");
        }
    }

    public void modifyProtection(int account) {
        if (!exists(onView(withId(R.id.available_accounts_title)))) {
            selectFromMenu(R.string.action_settings);
        }
        selectAccountSettingsFromList(account);
        selectFromScreen(stringToID("privacy_preferences"));
        clickTextOnScreen(stringToID("pep_enable_privacy_protection"));
        while (!exists(onView(withId(R.id.available_accounts_title)))) {
            pressBack();
            waitForIdle();
        }
    }

    public void selectAccount(String folder, int accountToSelect) {
        while (true) {
            try {
                waitForIdle();
                onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                waitForIdle();
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    selectAccountFromList(folder, accountToSelect);
                    getMessageListSize();
                    return;
                } else if (exists(onView(withId(R.id.accounts_list)))) {
                    selectAccountFromList(folder, accountToSelect);
                } else if (exists(onView(withId(android.R.id.list)))) {
                    clickFolder(folder);
                    return;
                } else if (!exists(onView(withId(R.id.available_accounts_title)))) {
                    selectFromMenu(R.string.action_settings);
                    selectAccountFromList(folder, accountToSelect);
                    getMessageListSize();
                    return;
                } else {
                    waitForIdle();
                    onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                    waitForIdle();
                }
            } catch (Exception ex) {
                if (!(accountToSelect < getAvailableAccounts())) {
                    fail("Cannot find account " + accountToSelect);
                }
                Timber.i("Cannot click account " + accountToSelect + ": " + ex.getMessage());
                while (!exists(onView(withId(R.id.accounts_list)))) {
                    pressBack();
                    waitForIdle();
                }
            }
        }
    }

    public static void swipeDownScreen() {
        try {
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
            waitForIdle();
            scroll.swipe(Direction.DOWN, 1.0f);
            waitForIdle();
        } catch (Exception swipe) {
            Timber.i("Cannot do swipeDown");
        }
    }

    public static void swipeDownList() {
        try {
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ListView"));
            waitForIdle();
            scroll.swipe(Direction.DOWN, 1.0f);
            waitForIdle();
        } catch (Exception swipe) {
            Timber.i("Cannot do swipeDown");
        }
    }

    public static void swipeUpScreen() {
        try {
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
            waitForIdle();
            scroll.swipe(Direction.UP, 0.9f);
            waitForIdle();
        } catch (Exception swipe) {
            Timber.i("Cannot do swipeUp");
        }
    }

    private void selectAccountFromHamburgerMenu(int accountToSelect) {
        /*waitForIdle();
        openHamburgerMenu();
        clickView(R.id.nav_header_accounts);
        waitForIdle();
        onView(withId(R.id.navigation_accounts)).perform(RecyclerViewActions.actionOnItemAtPosition(accountToSelect, click()));
    */
    }

    private void selectAccountFromList(String folder, int accountToSelect) {
        while (!viewIsDisplayed(R.id.accounts_list)) {
            swipeUpScreen();
        }
        onView(withId(R.id.accounts_list)).check(matches(isCompletelyDisplayed()));
        if (!(accountToSelect < getAvailableAccounts())) {
            fail("Cannot find account " + accountToSelect);
        }
        goToFolder(folder, accountToSelect);
        if (exists(onView(withId(R.id.message_list)))) {
            getMessageListSize();
        }
    }

    public void selectAccountSettingsFromList(int accountToSelect) {
        while (!viewIsDisplayed(R.id.accounts_list)) {
            swipeUpScreen();
        }
        onView(withId(R.id.accounts_list)).check(matches(isCompletelyDisplayed()));
        goToTheAccountSettings(accountToSelect);
        if (exists(onView(withId(R.id.message_list)))) {
            getMessageListSize();
        }
        waitForIdle();
    }

    private void goToFolder(String folder, int accountToSelect) {
        while (true) {
            waitForIdle();
            try {
                UiObject2 wb;
                wb = device.findObject(By.clazz("android.widget.ListView"));
                waitForIdle();
                wb.getChildren().get(accountToSelect).getChildren().get(1).click();
                clickFolder(folder);
                return;
            } catch (Exception e) {
                Timber.i("Cannot click account from list: " + e.getMessage());
            }
            waitForIdle();
        }
    }

    private void goToTheAccountSettings(int accountToSelect) {
        while (true) {
            waitForIdle();
            try {
                UiObject2 wb;
                wb = device.findObject(By.clazz("android.widget.ListView"));
                waitForIdle();
                wb.getChildren().get(accountToSelect).getChildren().get(0).click();
                return;
            } catch (Exception e) {
                Timber.i("Cannot click account from list: " + e.getMessage());
            }
            waitForIdle();
        }
    }

    public void clickFolder(String folder) {
        if (folder.equals("Spam")) {
            folder = folder
                    + " (" + folder + ")";
        }
        waitForIdle();
        while (true) {
            try {
                selectFromScreen(folder);
                waitForIdle();
                waitForToolbar();
                return;
            } catch (Exception noInbox) {
                Timber.i("No inbox to click: " + noInbox.getMessage());
            }
        }
    }

    public void clickSearch() {
        waitForIdle();
        onView(withId(R.id.search)).perform(click());
        waitForIdle();
    }

    private void fillAccountPassword(String accountPassword) {
        while (exists(onView(withId(R.id.account_password))) && getTextFromView(onView(withId(R.id.account_password))).equals("")) {
            try {
                waitForIdle();
                onView(withId(R.id.account_password)).perform(typeText(accountPassword), closeSoftKeyboard());
                waitForIdle();
                if (viewWithTextIsDisplayed(resources.getString(R.string.account_already_exists))) {
                    pressBack();
                    return;
                }
            } catch (Exception ex) {
                Timber.i("Cannot fill account password: " + ex.getMessage());
            }
        }
    }

    public void assertTextInView(String text, int view) {
        waitForIdle();
        try {
            waitForIdle();
            if (!getTextFromView(onView(withId(view))).contains(text)) {
                fail("View doesn't contain text: " + text);
            }
        } catch (Exception ex) {
            Timber.i("Cannot find view: " + ex.getMessage());
        }
    }

    private void manualAccount() {
        while (!viewIsDisplayed(R.id.manual_setup)) {
            waitForIdle();
        }
        while (exists(onView(withId(R.id.manual_setup)))) {
            waitForIdle();
            onView(withId(R.id.manual_setup)).perform(click());
        }
        setupImapServer();
        setupSMTPServer();
        waitForIdle();
        /*while (exists(onView(withId(R.id.account_server)))) {
            waitForIdle();
        }*/
        while (!exists(onView(withId(R.id.account_description)))) {
            try {
                waitForIdle();
                onView(withId(R.id.next)).perform(click());
                waitForIdle();
            } catch (Exception e) {
                Timber.i("Cannot find Description");
            }
        }
    }

    private void setupImapServer() {
        setupAccountIMAPServer(testConfig.getImap_server(account), "IMAP server");
    }

    private void setupSMTPServer() {
        setupAccountSMTPServer(testConfig.getSmtp_server(account), "SMTP server");
    }

    private void setupAccountIMAPServer(String accountServer, String server) {
        waitForIdle();
        onView(withId(R.id.account_server_label)).check(matches(isCompletelyDisplayed()));
        while (!viewIsDisplayed(R.id.account_server_label) &&
                !getTextFromView(onView(withId(R.id.account_server_label))).equals(server)) {
            waitForIdle();
        }
        while (!getTextFromView(onView(withId(R.id.account_server))).equals(accountServer) &&
                exists(onView(withId(R.id.account_server_label)))) {
            try {
                waitForIdle();
                while (!getTextFromView(onView(withId(R.id.account_server))).equals("")) {
                    removeTextFromTextView(R.id.account_server);
                }
                waitForIdle();
                while (!getTextFromView(onView(withId(R.id.account_server))).equals(accountServer)) {
                    onView(withId(R.id.account_server)).perform(typeText(accountServer), closeSoftKeyboard());
                    waitForIdle();
                    onView(withId(R.id.account_server)).check(matches(isDisplayed()));
                    waitForIdle();
                }
                setupPort(testConfig.getImap_port(account));
                waitForIdle();
                while (exists(onView(withId(R.id.account_server_label)))) {
                    try {
                        onView(withId(R.id.next)).perform(click());
                        while (exists(onView(withId(R.id.account_server_label)))) {
                            waitForIdle();
                        }
                        waitUntilIdle();
                        if (exists(onView(withId(R.id.alertTitle)))) {
                            pressBack();
                            waitForIdle();
                        }
                    } catch (Exception ex) {
                        Timber.i("Cannot click next button: " + ex.getMessage());
                    }
                }
                return;
            } catch (Exception e) {
                Timber.i("Cannot setup IMAP server: " + e.getMessage());
            }
        }
    }

    private void setupPort(String port) {
        if (port != null && !getTextFromView(onView(withId(R.id.account_port))).equals(port)) {
            waitForIdle();
            removeTextFromTextView(R.id.account_port);
            waitForIdle();
            onView(withId(R.id.account_port)).perform(click());
            onView(withId(R.id.account_port)).perform(typeText(port), closeSoftKeyboard());
            waitForIdle();
            onView(withId(R.id.account_port)).check(matches(isDisplayed()));
            waitForIdle();
        }
    }

    private void setupAccountSMTPServer(String accountServer, String server) {
        waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.account_server)).check(matches(isDisplayed()));
        while (!exists(onView(withId(R.id.account_server)))) {
            waitForIdle();
        }
        while (!getTextFromView(onView(withId(R.id.account_server))).equals("")) {
            removeTextFromTextView(R.id.account_server);
        }
        onView(withId(R.id.account_server)).check(matches(isCompletelyDisplayed()));
        waitForIdle();
        while (exists(onView(withId(R.id.account_server))) && !getTextFromView(onView(withId(R.id.account_server))).equals(accountServer)) {
            try {
                waitForIdle();
                waitForIdle();
                while (!getTextFromView(onView(withId(R.id.account_server))).equals(accountServer)) {
                    onView(withId(R.id.account_server)).perform(typeText(accountServer), closeSoftKeyboard());
                    waitForIdle();
                }
                setupPort(testConfig.getSmtp_port(account));
                waitForIdle();
                while (viewIsDisplayed(R.id.account_server)) {
                    waitForIdle();
                    if (exists(onView(withId(R.id.alertTitle)))) {
                        pressBack();
                        waitForIdle();
                    } else if (!exists(onView(withId(R.id.account_server)))) {
                        return;
                    } else {
                        onView(withId(R.id.next)).perform(click());
                        while (viewIsDisplayed(R.id.account_server)) {
                            waitForIdle();
                        }
                    }
                }
            } catch (Exception e) {
                Timber.i("Cannot setup server: " + e.getMessage());
            }
        }
    }

    private void allowPermissions() {
        waitForIdle();
        try {
            BySelector popUpMessage = By.clazz("android.widget.Button");
            boolean buttonExists = true;
            while (buttonExists) {
                buttonExists = false;
                for (UiObject2 object : device.findObjects(popUpMessage)) {
                    if (object.getResourceName() != null && object.getResourceName().equals("com.android.permissioncontroller:id/permission_allow_button")) {
                        buttonExists = true;
                        object.click();
                    }
                }
            }
        } catch (Exception ex) {
            Timber.i("Cannot allow permissions");
        }
        do {
            allowPermissions(2);
            allowPermissions(1);
        } while (!viewIsDisplayed((R.id.action_continue)) && !viewIsDisplayed((R.id.account_email)) && !viewIsDisplayed((R.id.terms_and_conditions)));
    }

    public void allowPermissions(int index) {
        try {
            waitForIdle();
            UiObject allowPermissions = device.findObject(new UiSelector()
                    .clickable(true)
                    .checkable(false)
                    .index(index));
            if (allowPermissions.exists()) {
                allowPermissions.click();
                waitForIdle();
            }
        } catch (Exception ignoredException) {
            Timber.i(ignoredException, "Failed trying to allow permission");
        }
    }

    String getAccountDescription() {
        return DESCRIPTION;
    }

    public void fillMessage(BasicMessage inputMessage, boolean attachFilesToMessage) {
        waitForIdle();
        while (!viewIsDisplayed(R.id.to)) {
            waitForIdle();
        }
        if (!inputMessage.getTo().equals("")) {
            typeTextInField(inputMessage.getTo(), R.id.to, "to");
            onView(withId(R.id.subject)).perform(click());
        }
        while (!getTextFromView(onView(withId(R.id.subject))).contains(inputMessage.getSubject())
                || !getTextFromView(onView(withId(R.id.message_content))).contains(inputMessage.getMessage())) {
            try {
                waitForIdle();
                waitForIdle();
                device.findObject(By.res(APP_ID, "subject")).click();
                waitForIdle();
                device.findObject(By.res(APP_ID, "message_content")).click();
                waitForIdle();
                onView(withId(R.id.subject)).perform(typeText(inputMessage.getSubject()), closeSoftKeyboard());
                waitForIdle();
                onView(withId(R.id.message_content)).perform(click());
                onView(withId(R.id.message_content)).perform(typeText(inputMessage.getMessage()), closeSoftKeyboard());
                waitForIdle();
            } catch (Exception ex) {
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

    public void typeTextInField(String text, int field, String resourceID) {
        UiObject2 list = null;
        Rect bounds = null;
        while (list == null || bounds == null) {
            try {
                waitForIdle();
                list = device.findObject(By.res(APP_ID, resourceID));
                bounds = list.getVisibleBounds();
            } catch (Exception ex) {
                Timber.i("Cannot find view TO");
            }
        }
        onView(withId(field)).perform(click(), closeSoftKeyboard());
        waitForIdle();
        device.click(bounds.left - 1, bounds.centerY());
        waitForIdle();
        device.click(bounds.left - 1, bounds.centerY());
        waitForIdle();
        if (text.equals("") && resourceID.equals("to")) {
            while (!getTextFromView(onView(withId(field))).equals(text)) {
                waitForIdle();
                clickView(R.id.to_label);
                waitForIdle();
                waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
            }
        } else {
            onView(withId(field)).perform(appendTextInTextView(text), closeSoftKeyboard());
        }
    }

    public void removeAddressClickingX (int address) {
        BySelector selector = By.clazz("android.widget.MultiAutoCompleteTextView");
        waitForIdle();
        clickView(R.id.to_label);
        waitForIdle();
        int boxBottom = 0;
        int rightX = 0;
        boolean clicked = false;
        while (!clicked) {
            for (UiObject2 multiTextView : device.findObjects(selector)) {
                boxBottom = multiTextView.getVisibleBounds().bottom;
                rightX = multiTextView.getVisibleBounds().right;
                int centerY = (multiTextView.getVisibleBounds().bottom - multiTextView.getVisibleBounds().top) * address / (address + 1) + multiTextView.getVisibleBounds().top;
                while (
                        0.9 <= Color.valueOf(getPixelColor(rightX, centerY)).green()
                        ) {
                    rightX--;
                }
                while (0.9 >= Color.valueOf(getPixelColor(rightX, centerY)).red() ||
                        0.9 >= Color.valueOf(getPixelColor(rightX, centerY)).green() ||
                        0.9 >= Color.valueOf(getPixelColor(rightX, centerY)).blue()) {
                    rightX--;
                }
                while (0.9 <= Color.valueOf(getPixelColor(rightX, centerY)).red() &&
                        0.9 <= Color.valueOf(getPixelColor(rightX, centerY)).green() &&
                        0.9 <= Color.valueOf(getPixelColor(rightX, centerY)).blue()) {
                    rightX--;
                }
                while (0.9 >= Color.valueOf(getPixelColor(rightX, centerY)).red() ||
                        0.9 >= Color.valueOf(getPixelColor(rightX, centerY)).green() ||
                        0.9 >= Color.valueOf(getPixelColor(rightX, centerY)).blue()) {
                    rightX--;
                }
                device.click(rightX, centerY);
            }
            waitForIdle();
            clickView(R.id.to_label);
            waitForIdle();
            for (UiObject2 multiTextView : device.findObjects(selector)) {
                if (boxBottom != multiTextView.getVisibleBounds().bottom || rightX != multiTextView.getVisibleBounds().right) {
                    clicked = true;
                }
            }
        }
    }

    public void checkOwnKey(String mainKeyID, boolean isTheSame) {
        checkOwnKeyInJSON(mainKeyID, isTheSame);
        checkOwnKeyInDB(mainKeyID, isTheSame);
    }

    public void checkOwnKeyInJSON(String mainKeyID, boolean isTheSame) {
        try {
            if ((json.getJSONObject("attributes").getJSONObject("from_decrypted").get("fpr").equals(mainKeyID)) != isTheSame) {
                fail("Own Key: " + mainKeyID + " // Key JSON file: " + json.getJSONObject("attributes").getJSONObject("from_decrypted").get("fpr"));
            }
        } catch (JSONException e) {
            Timber.e("Cannot read the Key from the JSON file");
        }
    }

    public void checkOwnKeyInDB(String mainKeyID, boolean isTheSame) {
        String newMainKeyID = getOwnKeyFromDB("management.db", "identity", "user_id");
        if ((mainKeyID.equals(newMainKeyID)) != isTheSame) {
            fail("Old own key: " + mainKeyID + " /// New own key: " + newMainKeyID);
        }
    }

    public void checkKeyIsInTheJSON(String key) {
        getJSONObject("keys");
        waitForIdle();
        if (!jsonArray.toString().contains(key)) {
            fail("The key " + key + " is not in the JSON file");
        }
    }

    public String getOwnKeyFromDB(String db, String table, String column) {
        Cursor cursor = getCursorFromDB(db, table, column, "pEp_own_userId");
        return cursor.getString(cursor.getColumnIndex("main_key_id"));
    }

    public Cursor getCursorFromDB(String db, String table, String column, String valueToFindInColumn) {
        String keys = "";
        try {
            File directory = getExportFolder();
            String[] directoryPath = directory.list();
            SQLiteDatabase database = openOrCreateDatabase(directory.getAbsolutePath() + "/" + directoryPath[0] + "/management.db", null);
            Cursor cursor = database.rawQuery("SELECT * FROM " + table, null);
            if (cursor != null) {
                cursor.moveToFirst();
                while (true) {
                    if (cursor.getString(cursor.getColumnIndex(column)).equals(valueToFindInColumn)) {
                        return cursor;
                    }
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            removeDBFolder();
            fail("Failed to read DB when trying to find: " + table + "/" + column);
        }
        return null;
    }


    public void checkValueIsInDB(String table, String column, String value) {
        exportDB();
        boolean rightValue = false;
        String valueInDB = "";
        try {
            File directory = getExportFolder();
            String[] directoryPath = directory.list();
            SQLiteDatabase database = openOrCreateDatabase(directory.getAbsolutePath() + "/" + directoryPath[0] + "/management.db", null);
            Cursor cursor = database.rawQuery("SELECT * FROM " + table, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        if (cursor.getString(cursor.getColumnIndex(column)).equals(value)) {
                            rightValue = true;
                        } else {
                            valueInDB += cursor.getString(cursor.getColumnIndex(column)) + " // ";
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            removeDBFolder();
            fail("Failed to read DB when trying to find: " + table + "/" + column + "/" + value);
        }
        removeDBFolder();
        if (!rightValue) {
            fail("Column " + column + " is " + valueInDB + " and it should be " + value);
        }
    }

    public void resetMyOwnKey(){
        selectFromScreen(stringToID("privacy_preferences"));
        selectFromScreen(stringToID("advanced"));
        scrollToView(resources.getString(stringToID("reset")));
        selectFromScreen(stringToID("reset"));
        waitForIdle();
        pressOKButtonInDialog();
        waitForIdle();
    }

    public void exportDB() {
        selectFromScreen(stringToID("privacy_preferences"));
        selectFromScreen(stringToID("advanced"));
        scrollToView(resources.getString(stringToID("support_settings_title")));
        selectFromScreen(stringToID("support_settings_title"));
        removeDBFolder();
        waitForIdle();
        selectFromScreen(stringToID("export_pep_support_data_preference_title"));
        waitForIdle();
        selectButtonFromScreen(stringToID("export_action"));
        waitForIdle();
        selectButtonFromScreen(stringToID("okay_action"));
    }

    public void removeDBFolder() {
        File directory= getExportFolder();
        if (directory.exists()) {
            try {
                FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (directory.exists()) {
            fail("Cannot remove the old DB");
        }
    }

    private FileWriter readDBFile() {
        File directory= getExportFolder();
        String[] directoryPath = directory.list();
        if (directoryPath.length > 1) {
            fail("There are more than 1 DB");
        }
        FileWriter dbFile = null;
        try {
            dbFile = new FileWriter(directory.getAbsolutePath()+"/"+directoryPath[0] + "/management.db");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dbFile;
    }

    private File getExportFolder() {
        return new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS)
                        + "/" +
                        ExportPlanckSupportDataPresenterKt.SUPPORT_EXPORT_TARGET_SUBFOLDER
                        + "/");
    }



    private void attachFiles(String fileName, String extension, int total) {
        for (int fileNumber = 0; fileNumber < total; fileNumber++) {
            Instrumentation.ActivityResult fileForActivityResultStub = createFileForActivityResultStub(fileName + fileNumber + extension);
            try {
                intending(not(isInternal())).respondWith(fileForActivityResultStub);
            } catch (Exception ex) {
                Timber.e("Intending: " +ex);
            }
            waitForIdle();
            onView(withId(R.id.add_attachment)).perform(click());
            waitForIdle();
            onView(withId(R.id.attachments)).check(matches(hasDescendant(withText(fileName + fileNumber + extension))));
        }
    }

    public void attachFile(String fileName) {
        do {
            waitForIdle();
            onView(withId(R.id.add_attachment)).perform(click());
            waitForIdle();
        } while (!textExistsOnScreenTextView(fileName));
        waitUntilIdle();
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
        File extStorageDirectory = context.getExternalFilesDir(null);
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
                if (!file.canRead()) {
                    file = new File(context.getExternalFilesDir(null), fileName);
                }
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
                waitForIdle();
                waitUntilIdle();
                intending(not(isInternal()))
                        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, insertFileIntoIntentAsData(fileName)));
                return;
            } catch (Exception ex) {
                Timber.i("Cannot insert file as data");
            }
        }
    }

    public String readFile (String folder, String fileName) {
        StringBuilder text = new StringBuilder();
        File file = new File(folder);
        try {
            FileInputStream fin = new FileInputStream(file + "/" + fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fin);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            while ((receiveString = bufferedReader.readLine()) != null) {
                text.append(receiveString);
            }
            fin.close();
        } catch (Exception e) {
            Timber.e("Error reading " + fileName + ", trying again");
        } finally {
            file.delete();
        }
        return text.toString();
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

        if (!fileLocation.exists()) {
            fileLocation = new File(context.getExternalFilesDir(null), fileName);
            Uri uri = FileProvider.getUriForFile(context, APP_ID + ".provider", fileLocation);
            resultData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            resultData.setType("*/*");
            resultData.setData(uri);
        }
        return resultData;
    }

    public void sendMessage() {
        clickView(R.id.send);
    }

    public void pressBack() {
        waitForIdle();
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
        onView(isRoot()).perform(ViewActions.pressBack());
        waitForIdle();
    }

    void removeLastAccount() {
        longClick("accounts_list");
        try {
            selectRemoveAccount();
            clickAcceptButton();
        } catch (Exception ex) {
            Timber.i("Cannot select/accept remove account");
        }
    }

    public void setupAccountIfNeeded() {
        skipTutorialAndAllowPermissionsIfNeeded();
        if (UtilsPackage.exists(Espresso.onView(withId(R.id.other_method_sign_in_button_card)))) {
            Espresso.onView(withId(R.id.other_method_sign_in_button_card)).perform(ViewActions.click());
        }
        if(exists(onView(withText(R.string.account_setup_basics_title)))) {
            setupAccountAutomatically(false);
        }
    }

    public void setupAccountAutomatically(boolean withSync) {
        setupEmailAndPassword();
        onView(withId(R.id.next)).perform(click());
        TestUtils.waitForIdle();
        waitUntilViewDisplayed(R.id.account_name);
        onView(withId(R.id.account_name)).perform(replaceText("test"));
        if(!withSync && viewIsDisplayed(R.id.pep_enable_sync_account)) {
            onView(withId(R.id.pep_enable_sync_account)).perform(click());
            waitForIdle();
        }
        onView(withId(R.id.done)).perform(click());
        waitForIdle();
    }

    private void setupEmailAndPassword() {
        TestUtils.waitForIdle();
        onView(allOf(withId(R.id.next), withText(R.string.next_action))).check(matches(isDisplayed()));
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches(withText(R.string.account_setup_basics_title)));

        String pass = BuildConfig.PLANCK_TEST_EMAIL_PASSWORD;
        String accountEmail = BuildConfig.PLANCK_TEST_EMAIL_ADDRESS;
        onView(withId(R.id.account_email)).perform(replaceText(accountEmail));
        TestUtils.waitForIdle();
        onView(withId(R.id.account_password)).perform(replaceText(pass));
        TestUtils.waitForIdle();
    }

    public void goToSettingsAndRemoveAllAccounts() {
        selectFromMenu(R.string.action_settings);
        removeAllAccounts();
    }

    public void goToSettingsAndRemoveAllAccountsIfNeeded() {
        if(!exists(onView(withText(R.string.account_setup_basics_title)))) {
            goToSettingsAndRemoveAllAccounts();
        }
    }

    public void removeAllAccounts() {
        Preferences preferences = Preferences.getPreferences(ApplicationProvider.getApplicationContext());
        while(!preferences.getAccounts().isEmpty()) {
            removeAccountAtPosition(0);
            doWait(5000);
        }
    }

    private void doWait(int millis) {
        try {
            sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void removeAccountAtPosition(int position) {
        waitForIdle();
        int[] accountListSize = new int[2];
        onView(withId(R.id.accounts_list)).perform(UtilsPackage.saveSizeInInt(accountListSize, 0));
        onView(withRecyclerView(R.id.accounts_list).atPosition(position))
                .perform(scrollTo(), ViewActions.longClick());
        waitForIdle();
        selectFromScreen(R.string.remove_account_action);
        waitForIdle();

        clickAcceptButton();
        waitForIdle();
        if(accountListSize[0] > 1) {
            do {
                onView(withId(R.id.accounts_list))
                        .perform(UtilsPackage.saveSizeInInt(accountListSize, 1));
                waitForIdle();
            } while (accountListSize[1] != accountListSize[0] - 1);
        } else {
            waitUntilViewDisplayed(onView(withText(R.string.account_setup_basics_title)));
        }
    }

    public void skipTutorialAndAllowPermissionsIfNeeded() {
        skipTutorial();
        clickContinueAndAllowPermisions();
    }

    private void skipTutorial() {
        try {
            waitForIdle();
            if(exists(onView(withId(R.id.skip)))) {
                onView(withId(R.id.skip)).perform(click());
            }
            waitForIdle();
        } catch (Exception ignoredException) {
            Timber.i("Ignored", "Ignored exception");
        }
    }

    private void clickContinueAndAllowPermisions() {
        try {
            waitForIdle();
            if(exists(onView(withId(R.id.action_continue)))) {
                onView(withId(R.id.action_continue)).perform(click());
                allowPermissions();
            }
            waitForIdle();
        } catch (Exception ignoredException) {
            Timber.i("Ignored", "Ignored exception");
        }
    }

    public void goBackAndRemoveAccount() {
        goBackAndRemoveAccount(false);
    }

    public void goBackAndRemoveAccount(boolean discardMessage) {
        Activity currentActivity = getCurrentActivity();
        while (true) {
            try {
                waitForIdle();
                removeLastAccount();
                return;
            } catch (Exception ex) {
                while (currentActivity == getCurrentActivity()) {
                    pressBack();
                    waitForIdle();
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
        waitForIdle();
        doWaitForObject("android.widget.Button");
        onView(withText(R.string.okay_action)).perform(click());
        waitForIdle();
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
                waitForIdle();
                pressBack();
                waitForIdle();
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
        int position;
        swipeUpScreen();
        for (int start = 0; start < total; start++) {
            int size = device.findObjects(selector).size();
            while (size == 0) {
                size = device.findObjects(selector).size();
            }
            UiObject2 uiObject = device.findObject(By.res(BuildConfig.APPLICATION_ID + ":id/attachment"));
            position = -1;
            for (UiObject2 frameLayout : device.findObjects(selector)) {
                waitForIdle();
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
                            waitForIdle();
                            pressBack();
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
        waitForIdle();
        int view = intToID(viewId);
        removeTextFromTextView(view);
    }

    public void removeTextFromTextView(int view) {
        waitForIdle();
        while (!exists(onView(withId(view)))) {
            waitForIdle();
        }
        onView(withId(view)).perform(closeSoftKeyboard());
        onView(withId(view)).perform(click());
        //clickTextView(viewId);
        while (!(hasValueEqualTo(onView(withId(view)), " ")
                || hasValueEqualTo(onView(withId(view)), ""))) {
            try {
                waitForIdle();
                waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                waitForIdle();
                onView(withId(view)).perform(click());
            } catch (Exception ex) {
                pressBack();
                Timber.i("Cannot remove text from field " + view + ": " + ex.getMessage());
            }
        }
    }

    public void removeTextFromTextView(int viewId, String target) {
        waitForIdle();
        int size = target.length();
        for (int i  = 0; i < size; i ++) {
            try {
                waitForIdle();
                waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                waitForIdle();
            } catch (Exception ex) {
                pressBack();
                Timber.i("Cannot remove text from field " + viewId + ": " + ex.getMessage());
            }
        }
    }

    public void clickTextView(String viewId) {
        while (true) {
            try {
                UiObject2 list = device.findObject(By.res(APP_ID, viewId));
                Rect bounds = list.getVisibleBounds();
                device.click(bounds.left - 1, bounds.centerY());
                return;
            } catch (Exception ex) {
                waitForIdle();
                Timber.i("Cannot click " + viewId + ": " + ex.getMessage());
            }
        }
    }

    void testStatusEmpty() {
        assertMessageStatus(
                Rating.pEpRatingUndefined,
                true,
                false,
                false
        );
    }

    void testStatusMailAndListMail(BasicMessage inputMessage, BasicIdentity expectedIdentity) {
        fillMessage(inputMessage, false);
        assertMessageStatus(expectedIdentity.getRating(), false);
        goBack(false);
    }

    void checkStatus(Rating rating, String status) {
        assertMessageStatus(rating, status);
    }

    public void assertStatus(Rating rating) {
        waitForIdle();
        while (!viewIsDisplayed(R.id.securityStatusIcon)) {
            waitForIdle();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        int value = rating.value;
        int color = PlanckUIUtils.getRatingColorRes(Rating.getByInt(value), true);
        assertsIconColor("securityStatusIcon", color);
        assertSecurityStatusText(rating);
    }
    public void assertMessageStatus(Rating rating, String status){
        int statusColor;
        //clickStatus();
        while (!viewIsDisplayed(R.id.toolbar)) {
            waitForIdle();
        }
        //onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        /*while (!viewIsDisplayed(R.id.pEpTitle)) {
            waitForIdle();
        }*/
        //waitForToolbar();
        statusColor = getSecurityStatusDrawableColor(rating);
        if (statusColor == -10) {
            if (viewIsDisplayed(R.id.actionbar_message_view)) {
                fail("Wrong Status, it should be empty");
            }
        } else {
            int value = rating.value;
            int color = PlanckUIUtils.getRatingColorRes(Rating.getByInt(value), true);

            //int color = R.color.compose_unsecure_delivery_warning;
            assertsIconColor("securityStatusIcon", color);
            viewIsDisplayed(R.id.securityStatusIcon);
            assertSecurityStatusText(rating);
        }
        if (!exists(onView(withId(R.id.send)))) {
            goBack(false);
        }
    }

    public void assertMessageStatus(Rating status, boolean clickableExpected){
        assertMessageStatus(status, true, clickableExpected, true);
    }

    public void assertMessageStatus(
            Rating status,
            boolean enabledForThisMessage,
            boolean clickableExpected,
            boolean visible
    ){
        int statusColor;

        waitForToolbar();
        if(!enabledForThisMessage) {
            onView(withId(R.id.securityStatusText)).check(matches(withText(R.string.pep_rating_forced_unencrypt)));
        }

        statusColor = getSecurityStatusDrawableColor(status);
        if (statusColor == -10) {
            if (viewIsDisplayed(R.id.actionbar_message_view)) {
                fail("Wrong Status, it should be empty");
            }
        } else {
            if (R.drawable.planck_status_green != statusColor
                    && R.drawable.planck_status_red != statusColor
                    && R.drawable.pep_status_yellow != statusColor
                    && R.drawable.enterprise_status_unsecure != statusColor
            ) {
                fail("Wrong Status color");
            }
            if (visible) {
                waitUntilViewDisplayed(R.id.securityStatusText);
            }

            if(!enabledForThisMessage) {
                onView(withId(R.id.securityStatusText)).check(matches(withText(R.string.pep_rating_forced_unencrypt)));
                onView(withId(R.id.securityStatusText)).check(matches(withTextColor(R.color.planck_no_color)));
            }
            else {
                assertSecurityStatusText(status);
            }
        }

        if (visible) {
            clickStatus();
            if(clickableExpected) {
                waitForIdle();
                waitForToolbar();
                checkToolbarColor(getPlanckStatusDueColor(status));
                waitForIdle();
                pressBack();
            }
        }
    }

    public void assertSecurityStatusText(Rating status) {
        int value = status.value;
        String firstLineText = getTextFromView(onView(withId(R.id.securityStatusText)));
        ViewInteraction secondLine = onView(withId(R.id.securityStatusSecondLine));
        String emptySpace = "";
        String secondLineText = exists(secondLine)
                ? getTextFromView(onView(withId(R.id.securityStatusSecondLine)))
                : "";
        if (!secondLineText.equals("")) {
            emptySpace = " ";
        }
        if (value == -1) {
            value = 10;
        }
        assertEquals(
                getResourceString(R.array.pep_title, value),
                firstLineText + emptySpace + secondLineText
        );
    }

    private int getSecurityStatusDrawableColor(Rating rating){
        int color;
        if (rating == null) {
            color = -10;
        } else if (PlanckUtils.isRatingUnsecure(rating)) {
            color = R.drawable.enterprise_status_unsecure;
        } else if (rating.value == Rating.pEpRatingMistrust.value) {
            color = R.drawable.planck_status_red;
        } else if (rating.value >= Rating.pEpRatingTrusted.value) {
            color = R.drawable.planck_status_green;
        } else if (rating.value == Rating.pEpRatingReliable.value) {
            color = R.drawable.pep_status_yellow;
        } else {
            color = -10;
        }
        return color;
    }

    private int getPlanckStatusDueColor(Rating rating) {
        int color;
        if (rating == null) {
            color = -10;
        } else if (rating.value != pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value) {
            color = R.color.planck_no_color;
        } else if (rating.value == pEpRatingMistrust.value) {
            color = R.color.planck_red;
        } else if (rating.value >= Rating.pEpRatingTrusted.value) {
            color = R.color.planck_green;
        } else if (rating.value == Rating.pEpRatingReliable.value) {
            color = R.color.planck_yellow;
        } else {
            color = -10;
        }
        return color;
    }

    private int getSecurityStatusIconColor (Rating rating){
        int color;
        if (rating == null) {
            color = -10;
        } else if (rating.value != pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value) {
            color = -10;
        } else if (rating.value == pEpRatingMistrust.value) {
            color = R.color.planck_red;
        } else if (rating.value >= Rating.pEpRatingReliable.value) {
            color = R.color.planck_green;
        } else if (rating.value < Rating.pEpRatingReliable.value) {
            color = R.color.planck_yellow;
        } else {
            color = -10;
        }
        return color;
    }

    public void clickStatus() {
        waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        if (viewIsDisplayed(R.id.securityStatusText)) {
            waitForIdle();
            onView(withId(R.id.securityStatusText)).check(matches(isDisplayed()));
            waitForIdle();
            while (viewIsDisplayed(onView(withId(R.id.securityStatusText)))) {
                try {
                    onView(withId(R.id.securityStatusText)).perform(click());
                } catch (Exception exception) {
                    waitForIdle();
                }
            }
        }
        waitForIdle();
    }

    public void goBackAndSaveAsDraft (){
        goBack(true);
    }

    public void goBackFromMessageCompose(boolean saveAsDraft) {
        goBack(saveAsDraft);
    }

    private void goBack (boolean saveAsDraft) {
        try {
            waitForIdle();
            if (!viewIsDisplayed(R.id.message_content)) {
                onView(withId(R.id.toolbar)).perform(closeSoftKeyboard());
                waitForIdle();
            }
        } catch (Exception ex) {
            Timber.i("Ignored exception: " + ex);
        }
        waitForIdle();
        pressBack();
        waitForIdle();
        if (saveAsDraft) {
            onView(withText(R.string.save_draft_action)).perform(click());
        }
        try {
            onView(withText(R.string.discard_action)).perform(click());
        } catch (Exception noDiscard) {
            Timber.i("Cannot discard the message");
        }
        waitForIdle();
    }

    public void assertsTextsOnScreenAreEqual(int resourceOnScreen, int comparedWith) {
        BySelector selector = By.clazz("android.widget.TextView");
        String textOnScreen = "Text not found on the Screen";
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains(resources.getString(resourceOnScreen))) {
                    waitForIdle();
                    textOnScreen = object.getText();
                    waitForIdle();
                    break;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
        pressBack();
        onView(withId(R.id.toolbar)).check(matches(valuesAreEqual(textOnScreen, resources.getString(comparedWith))));
    }

    public void assertsTextExistsOnScreen (String textToCompare) {
        BySelector selector = By.clazz("android.widget.TextView");
        boolean exists = false;
        waitForIdle();
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains(textToCompare)) {
                    exists = true;
                    waitForIdle();
                    break;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
        if (!exists) {
            fail("Cannot find " + textToCompare + " on the screen");
        }
    }

    public void assertsIconColor (String colorId, int expectedColor) {
        BySelector selector = By.clazz("android.widget.ImageView");
        for (int i = 0; i < 1500; i ++) {
            waitForIdle();
        }
        for (UiObject2 object : device.findObjects(selector)) {
            if (object.getResourceName() != null && object.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/" + colorId)) {
                waitForIdle();
                int iconColor = iconColor(object);
                expectedColor = ContextCompat.getColor(context, expectedColor);
                if (iconColor != expectedColor) {
                        fail("Wrong icon color: Expected color is " + String.format("#%06X", (0xFFFFFF & expectedColor)) + " but icon color is " + String.format("#%06X", (0xFFFFFF & iconColor)));
                    break;
                }
            }
        }
    }

    private int iconColor (UiObject2 object) {
        int x = (object.getVisibleBounds().right - object.getVisibleBounds().left)*2/5 + object.getVisibleBounds().left;
        int color = getPixelColor(x, object.getVisibleCenter().y);
        return color;
    }
    public void setWifi (boolean enable) throws IOException {
        if (enable) {
            device.executeShellCommand("svc wifi enable");
        } else {
            device.executeShellCommand("svc wifi disable");
        }
    }
    public void summonThreads () {
        clickStatus();
        pressBack();
    }

    public int stringToID(String text){
        return resources.getIdentifier(text, "string", BuildConfig.APPLICATION_ID);
    }

    public int pluralsStringToID(String text){
        return resources.getIdentifier(text, "plurals", BuildConfig.APPLICATION_ID);
    }

    public int intToID(String text){
        return resources.getIdentifier(text, "id", BuildConfig.APPLICATION_ID);
    }

    public int colorToID(String color){
        return resources.getIdentifier(color, "color", BuildConfig.APPLICATION_ID);
    }

    public void checkStatusText(String text) {
        waitForIdle();
        while (!viewIsDisplayed(R.id.toolbar) || !viewIsDisplayed(R.id.toolbar_container)) {
            waitForIdle();
        }
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        while (true) {
            waitForIdle();
            if (BuildConfig.IS_OFFICIAL) {
                if (!(viewIsDisplayed(onView(withId(R.id.securityStatusText))))) {
                    fail("Status is not shown");
                }
            }
            if (exists(onView(withId(R.id.toolbar))) && viewIsDisplayed(R.id.toolbar) && viewIsDisplayed(R.id.toolbar_container)) {
                waitForIdle();
                String statusText = getTextFromView(onView(withId(R.id.securityStatusText))) + " " + getTextFromView(onView(withId(R.id.securityStatusSecondLine)));
                if (!statusText.contains(text)) {
                    fail("Status are not the same. It is " + statusText + " and it should be " + text);
                }
                return;
            }
        }
    }

    public void checkPrivacyTextColor(int color) {
        waitForIdle();
        while (!viewIsDisplayed(R.id.toolbar) || !viewIsDisplayed(R.id.toolbar_container)) {
            waitForIdle();
        }
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        while (true) {
            waitForIdle();
            if (BuildConfig.IS_OFFICIAL) {
                if (!(viewIsDisplayed(onView(withId(R.id.securityStatusText))))) {
                    fail("Status is not shown");
                }
            }
            if (exists(onView(withId(R.id.toolbar))) && viewIsDisplayed(R.id.toolbar) && viewIsDisplayed(R.id.toolbar_container)) {
                waitForIdle();
                onView(withId(R.id.securityStatusText)).check(matches(withTextColor(color)));
                //checkUpperToolbar(color);
                return;
            }
        }
    }

    public String getStatusRating(Rating [] statusRating, String status) {
        switch (status){
            case "Undefined":
                statusRating[0] = Rating.pEpRatingUndefined;
                break;
            case "CannotDecrypt":
                statusRating[0] = Rating.pEpRatingCannotDecrypt;
                break;
            case "pEpRatingHaveNoKey":
                statusRating[0] = Rating.pEpRatingHaveNoKey;
                break;
            case "NotEncrypted":
                statusRating[0] = Rating.pEpRatingUnencrypted;
                break;
            case "WeaklyEncrypted":
                statusRating[0] = Rating.pEpRatingUnreliable;
                break;
            case "MediaKey":
                statusRating[0] = Rating.pEpRatingMediaKeyProtected;
                break;
            case "Encrypted":
                statusRating[0] = Rating.pEpRatingReliable;
                break;
            case "Trusted":
                statusRating[0] = Rating.pEpRatingTrusted;
                break;
            case "pEpRatingTrustedAndAnonymized":
                statusRating[0] = Rating.pEpRatingTrustedAndAnonymized;
                break;
            case "pEpRatingFullyAnonymous":
                statusRating[0] = Rating.pEpRatingFullyAnonymous;
                break;
            case "Dangerous":
                statusRating[0] = Rating.pEpRatingMistrust;
                break;
            case "Broken":
                statusRating[0] = Rating.pEpRatingB0rken;
                break;
            case "UnderAttack":
                statusRating[0] = Rating.pEpRatingUnderAttack;
                break;
        }
        return status;
    }

    public void checkBadgeStatus(String status, int messageFromList) {
        Rating[] statusRating = new Rating[1];
        int currentMessage = 1;
        waitForIdle();
        getStatusRating(statusRating, status);
        int statusColor = getSecurityStatusIconColor(statusRating[0]);
        boolean assertedBadgeColor = false;
        BySelector selector = By.clazz("android.widget.ImageView");
        while (!assertedBadgeColor) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/privacyBadge")) {
                        if (currentMessage != messageFromList) {
                            currentMessage++;
                        } else {
                            int pixel = iconColor(object);
                            if (pixel != ContextCompat.getColor(context, statusColor)) {
                                //ContextCompat.getColor(context, statusColor)
                                fail("Wrong color: Expected badge color is " + String.format("#%06X", (0xFFFFFF & statusColor)) + " but icon color is " + String.format("#%06X", (0xFFFFFF & pixel)));
                            }
                            assertedBadgeColor = true;
                            break;
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find text on screen: " + ex);
                }
            }
        }
    }

    public int getPixelColor (int x, int y) {
        View currentViewActivity = getCurrentActivity().getWindow().getDecorView().getRootView();
        currentViewActivity.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(currentViewActivity.getDrawingCache());
        int pixel = bitmap.getPixel(x, y);
        currentViewActivity.setDrawingCacheEnabled(false);
        return pixel;
    }

    public int getNextHorizontalColoredXPixelToTheRight(int X, int Y) {
        int color;
        do {
            color = getPixelColor(X, Y);
            X = X + 1;
        } while (Color.valueOf(color).green() == 1.0 && Color.valueOf(color).blue() == 1.0 && Color.valueOf(color).red() == 1.0);
        return X - 1;
    }

    public int getNextHorizontalWhiteXPixelToTheRight(int X, int Y) {
        int color;
        do {
            color = getPixelColor(X, Y);
            X = X + 1;
        } while (Color.valueOf(color).green() != 1.0 || Color.valueOf(color).blue() != 1.0 || Color.valueOf(color).red() != 1.0);
        return X - 1;
    }

    public int getNextVerticalWhiteYPixelToTheTop(int X, int Y) {
        int color;
        do {
            color = getPixelColor(X, Y);
            Y = Y - 1;
        } while (Color.valueOf(color).green() != 1.0 || Color.valueOf(color).blue() != 1.0 || Color.valueOf(color).red() != 1.0);
        return Y + 1;
    }

    public int getNextVerticalColoredYPixelToTheTop(int X, int Y) {
        int color;
        do {
            color = getPixelColor(X, Y);
            Y = Y - 1;
        } while (Color.valueOf(color).green() == 1.0 && Color.valueOf(color).blue() == 1.0 && Color.valueOf(color).red() == 1.0);
        return Y + 1;
    }

    public int getNextVerticalWhiteYPixelToTheBottom(int X, int Y) {
        int color;
        do {
            color = getPixelColor(X, Y);
            Y = Y + 1;
        } while (Color.valueOf(color).green() != 1.0 || Color.valueOf(color).blue() != 1.0 || Color.valueOf(color).red() != 1.0);
        return Y - 1;
    }

    public int getNextVerticalColoredYPixelToTheBottom(int X, int Y) {
        int color;
        do {
            color = getPixelColor(X, Y);
            Y = Y + 1;
        } while (Color.valueOf(color).green() == 1.0 && Color.valueOf(color).blue() == 1.0 && Color.valueOf(color).red() == 1.0);
        return Y - 1;
    }

    public int getNextHorizontalWhiteXPixelToTheLeft(int X, int Y) {
        int color;
        do {
            color = getPixelColor(X, Y);
            X = X - 1;
        } while (Color.valueOf(color).green() != 1.0 || Color.valueOf(color).blue() != 1.0 || Color.valueOf(color).red() != 1.0);
        return X + 1;
    }

    public void checkBadgeColor(int color, int messageFromList) {
        waitForIdle();
        onView(withId(R.id.privacyBadge)).check(matches(withTextColor(color)));
    }

    public void openHamburgerMenu () {
        waitForIdle();
        if (!viewIsDisplayed(onView(withId(R.id.navigation_bar_folders_layout)))) {
            while (!exists(onView(withContentDescription("Open navigation drawer")))) {
                waitForIdle();
            }
            onView(withContentDescription("Open navigation drawer")).perform(click());
            waitForIdle();
        }
    }

    public void typeTextToForceRatingCalculation(int view) {
        waitForIdle();
        try {
            onView(withId(view)).perform(click());
            onView(withId(view)).perform(click(), closeSoftKeyboard());
            onView(withId(view)).perform(click());
            onView(withId(view)).perform(click(), closeSoftKeyboard());
            onView(withId(view)).perform(typeText(" "), closeSoftKeyboard());
            waitForIdle();
            if (getTextFromView(onView(withId(view))).contains(" ")) {
                device.pressKeyCode(KeyEvent.KEYCODE_DEL);
            }
            waitForIdle();

        } catch (Exception ex) {
            Timber.i("Toolbar is not closed yet");
        }
    }

    public static void waitForToolbar() {
        for (int waitLoop = 0; waitLoop < 1000; waitLoop++) {
            waitForIdle();
            while (!viewIsDisplayed(R.id.toolbar)) {
                waitForIdle();
            }
            waitForIdle();
            onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
            waitForIdle();
        }
    }

    private void checkUpperToolbar (int color){
        int colorFromResource = PlanckColorUtils.makeColorTransparent(
                ContextCompat.getColor(InstrumentationRegistry.getInstrumentation().getTargetContext(), color));
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(colorFromResource), Color.green(colorFromResource), Color.blue(colorFromResource), hsv);
        hsv[2] = hsv[2]*0.9f;
        color = Color.HSVToColor(hsv);
        int upperToolbarColor = getCurrentActivity().getWindow().getStatusBarColor();
        org.junit.Assert.assertEquals("Text", upperToolbarColor, color);
        if (upperToolbarColor != color) {
            fail("Upper toolbar color is wrong");
        }
    }

    public void selectFromMenu(int viewId){
        waitForIdle();
        while (true) {
            try {
                openOptionsMenu();
                selectFromScreen(viewId);
                waitForIdle();
                return;
            } catch (Exception ex) {
                Timber.i("Toolbar is not closed yet");
            }
        }
    }

    public void selectFromStatusPopupMenu(int itemId) {
        waitForIdle();
        onView(withId(R.id.actionbar_message_view)).perform(ViewActions.longClick());
        waitForIdle();
        selectFromPopupMenu(itemId);
        waitForIdle();
    }

    public void selectFromPopupMenu(int itemId) {
        onView(withText(itemId)).inRoot(isPopupWindow()).perform(click());
    }

    public Matcher<Root> isPopupWindow() {
        return isPlatformPopup();
    }

    public static void waitForIdle() {
        device.waitForIdle();
        Espresso.onIdle();
        waitUntilIdle();
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

    public boolean textExistsOnScreen (String text) {
        waitForIdle();
        BySelector selector = By.clazz("android.widget.TextView");
        try {
            for (UiObject2 view : device.findObjects(selector)) {
                if (view.getText() != null) {
                    if (view.getText().equals(text) || view.getText().contains("(" + text + ")")) {
                        return true;
                    }
                }
            }
        } catch (Exception exception) {
            return false;
        }
        return false;
    }

    private boolean textExistsOnScreenTextView(String text) {
        boolean viewExists = false;
        waitForIdle();
        BySelector selector = By.clazz("android.widget.TextView");
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
            waitForIdle();
        }
    }

    private void goBackToOriginalApp() {
        while (!APP_ID.equals(device.getCurrentPackageName())) {
            waitForIdle();
            Espresso.onIdle();
            device.pressBack();
            waitForIdle();
            Espresso.onIdle();
        }
    }

    public static void openOptionsMenu() {
        while (true) {
            try {
                waitForIdle();
                onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
                waitForIdle();
                openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
                waitForIdle();
            } catch (Exception ex) {
                Timber.i("Cannot open menu");
                return;
            }
        }
    }

    public void selectSwitchButton(int resource) {
        BySelector selector = By.clazz("android.widget.TextView");
        while (true) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().equals(resources.getString(resource))) {
                        try {
                            waitForIdle();
                            Espresso.onIdle();
                            object.longClick();
                            waitForIdle();
                            Espresso.onIdle();
                            return;
                        } catch (Exception ex1) {
                            waitForIdle();
                            Espresso.onIdle();
                            return;
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find text on screen: " + ex);
                }
            }
        }
    }

    public void selectFromScreen(int resource) {
        waitForIdle();
        String text = resources.getString(resource);
        int textCharacters = text.length();
        BySelector selector = By.clazz("android.widget.TextView");
        while (true) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().substring(0, textCharacters).equals(text)) {
                            waitForIdle();
                            object.longClick();
                            waitForIdle();
                            return;
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find text " + text +" on the screen: " + ex);
                }
            }
            swipeUpScreen();
            waitForIdle();
        }
    }

    public void selectFromScreen(String text) {
        BySelector selector = By.clazz("android.widget.TextView");
        while (true) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().equals(text)) {
                        try {
                            while (object.getText().equals(text)) {
                                waitForIdle();
                                Espresso.onIdle();
                                object.longClick();
                                waitForIdle();
                                Espresso.onIdle();
                            }
                            waitForIdle();
                            Espresso.onIdle();
                            return;
                        } catch (Exception ex1) {
                            waitForIdle();
                            Espresso.onIdle();
                            return;
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find text on screen: " + ex);
                }
            }
        }
    }

    public void clickTextOnScreen(String text) {
        clickText(text);
    }

    public void clickTextOnScreen(int resource) {
        clickText(resources.getString(resource));
    }

    public void clickText(String text) {
        BySelector selector = By.clazz("android.widget.TextView");
        while (true) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().equals(text)) {
                        try {
                            waitForIdle();
                            object.longClick();
                            waitForIdle();
                            return;
                        } catch (Exception ex1) {
                            waitForIdle();
                            return;
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find text on screen: " + ex);
                }
            }
        }
    }

    public String getFingerprint() {
        BySelector selector = By.clazz("android.widget.TextView");
        boolean isFingerprint = false;
        while (true) {
            for (UiObject2 object : device.findObjects(selector)) {
                if (isFingerprint) {
                    return object.getText().trim().replace("\n", "").replace(" ", "");
                }
                if (object.getText().equals(resources.getString(stringToID("pgp_key_import_confirmation_fingerprint_label")))) {
                    isFingerprint = true;
                }
            }
        }
    }

    public void waitForKeyImport() {
        BySelector selector = By.clazz("android.widget.TextView");
        boolean isFingerprint = false;
        while (true) {
            waitForIdle();
            try {
                for (UiObject2 object : device.findObjects(selector)) {
                    waitForIdle();
                    if (object.getText().equals(resources.getString(stringToID("key_import_success")))) {
                        waitForIdle();
                        return;
                    }
                }
            } catch (Exception noImport) {
                Timber.i("Key not imported yet");
            }
        }
    }

    public void selectButtonFromScreen(String text) {
        BySelector selector = By.clazz("android.widget.Button");
        while (true) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().equals(text)) {
                        try {
                            while (object.getText().equals(text)) {
                                waitForIdle();
                                object.longClick();
                                waitForIdle();
                            }
                            waitForIdle();
                            return;
                        } catch (Exception ex1) {
                            waitForIdle();
                            return;
                        }
                    }
                } catch (Exception ex) {
                    waitForIdle();
                    Timber.i("Cannot find button on screen: " + ex);
                }
            }
        }
    }

    public void selectButtonFromScreen(int resource) {
        selectButtonFromScreen(resources.getString(resource).toUpperCase());
    }


    void doWait(String viewId) {
        UiObject2 waitForView = device
                .wait(Until.findObject(By.res(APP_ID, viewId)),
                        150000);
        assertThat(waitForView, notNullValue());
    }

    public void waitForView (int view) {
        while (true) {
            try {
                while (!exists(onView(withId(view)))) {
                    waitForIdle();
                }
                return;
            } catch (Exception viewIsNotDisplayed) {
                Timber.i("View is not displayed, waiting for view...");
            }
        }
    }

    public void doWaitForResource(int resource) {
        waitForIdle();
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

    public void waitUntilViewDisplayed(int viewId) {
        boolean displayed = false;
        while(!displayed) {
            displayed = viewIsDisplayed(viewId);
            device.waitForIdle();
        }
    }

    public void waitUntilViewDisplayed(ViewInteraction viewInteraction) {
        waitUntilViewDisplayed(viewInteraction, 0L);
    }

    private boolean waitUntilViewDisplayed(ViewInteraction viewInteraction, long timeout) {
        long initialTime = System.currentTimeMillis();
        boolean displayed = false;
        while(!displayed) {
            displayed = viewIsDisplayed(viewInteraction);
            device.waitForIdle();
            if(timeout > 0 && System.currentTimeMillis() - initialTime > timeout) {
                return false;
            }
        }
        return true;
    }

    private void doWaitForIdlingListViewResource(int resource){
        IdlingResource idlingResourceListView;
        waitForIdle();
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

    void doWaitForAlertDialog(IntentsTestRule<SplashActivity> intent, int displayText) {
        onView(withId(intent.getActivity().getResources()
                .getIdentifier("alertTitle", "id", "android")))
                .inRoot(isDialog())
                .check(matches(withText(displayText)))
                .check(matches(isDisplayed()));
    }

    public void doWaitForAlertDialog(int displayText) {
        doWaitForAlertDialog(displayText, false, 0L);
    }

    public void doWaitForDialog (String name, int displayText) {
        doWaitForAlertDialog(displayText, false, 0L, name);
    }
    private boolean doWaitForAlertDialog(int displayText, boolean isOwnPackage, long timeout) {
        return doWaitForAlertDialog(displayText, isOwnPackage, timeout, "alertTitle");
    }

    private boolean doWaitForAlertDialog(int displayText, boolean isOwnPackage, long timeout, String name) {
        String packageName = isOwnPackage
                ? context.getPackageName()
                : "android";
        waitForIdle();
        int id = context.getResources().getIdentifier(name, "id", packageName);
        ViewInteraction dialogHeaderViewInteraction = onView(withId(id)).inRoot(isDialog());
        if(!waitUntilViewDisplayed(dialogHeaderViewInteraction, timeout)) {
            return false;
        }

        onView(withText(displayText)).check(matches(isDisplayed()));
        waitForIdle();
        return true;
    }

    public void doWaitForNextAlertDialog(boolean isOwnPackage) {
        String packageName = isOwnPackage
            ? context.getPackageName()
            : "android";
        waitForIdle();
        int id = context.getResources().getIdentifier("alertTitle", "id", packageName);
        waitUntilViewDisplayed(onView(withId(id)).inRoot(isDialog()));
        waitForIdle();
    }

    public void waitForUiObject2 (String textInTheObject, String resourceName, BySelector selector) {
        waitForIdle();
        while (true) {
            for (UiObject2 textView : device.findObjects(selector)) {
                try {
                    if (textView.getResourceName().equals(resourceName) && textView.getText().equals(textInTheObject)) {
                        waitForIdle();
                        return;
                    }
                } catch (Exception nullView) {
                    waitForIdle();
                    Timber.i(textInTheObject + " is not ready yet: " + nullView.getMessage());
                }
            }
            waitForIdle();
        }
    }

    public void pressOKButtonInDialog () {
        BySelector selector = By.clazz("android.widget.Button");
        while (true) {
            for (UiObject2 button : device.findObjects(selector)) {
                if (button.getResourceName().equals("android:id/button1")) {
                    button.click();
                    waitForIdle();
                    return;
                }
            }
        }
    }

    String getResourceString(int id, int position) {
        return resources.getStringArray(id)[position];
    }

    public void clickMessageStatus() {
        selectFromMenu((stringToID("pep_title_activity_privacy_status")));
        //clickView(R.id.securityStatusText);
    }

    public void goBackToMessageList(){
        boolean backToMessageCompose = false;
        if (viewIsDisplayed(R.id.fab_button_compose_message)){
            backToMessageCompose = true;
        }
        while (!backToMessageCompose){
            goBack(false);
            waitForIdle();
            if (exists(onView(withId(android.R.id.list)))) {
                clickFolder(resources.getString(stringToID("special_mailbox_name_inbox")));
                return;
            }
            waitForIdle();
            if (viewIsDisplayed(R.id.fab_button_compose_message)){
                backToMessageCompose = true;
            }
        }
    }

    public void goToInboxFolder() {
        goToFolder(resources.getString(R.string.special_mailbox_name_inbox));
    }

    public void goToSentFolder() {
        goToFolder(resources.getString(R.string.special_mailbox_name_sent_fmt, ""));
    }

    public void goToDraftsFolder() {
        goToFolder(resources.getString(R.string.special_mailbox_name_drafts_fmt, ""));
    }

    public void goToFolder(String folder) {
        openHamburgerMenu();
        waitForIdle();
        ViewInteraction folderInteraction = checkFolderInDrawerToFindName(
                folder,
                allOf(
                        withId(R.id.unified_inbox_text),
                        withParent(withId(R.id.unified_inbox))
                )
        );
        if (folderInteraction == null) {
            folderInteraction = checkFolderInDrawerToFindName(
                    folder,
                    allOf(
                            withId(R.id.all_messages_text),
                            withParent(withId(R.id.all_messages_container))
                    )
            );
        }

        int folders = getListSize(R.id.navigation_folders);
        int index = 0;
        while (folderInteraction == null && index < folders) {
            folderInteraction = checkFolderInDrawerToFindName(
                    folder,
                    withRecyclerView(R.id.navigation_folders)
                            .atPositionOnView(index, R.id.folder_name)
            );
            ViewInteraction clickerInteraction = onView(withRecyclerView(R.id.navigation_folders)
                    .atPositionOnView(index, R.id.showchildrenclicker));
            if (viewIsDisplayed(clickerInteraction)) {
                clickerInteraction.perform(click());
                waitForIdle();
                folders = getListSize(R.id.navigation_folders);
            }
            index ++;
        }

        if (folderInteraction != null) {
            folderInteraction.perform(click());
        } else {
            fail("Folder " + folder +  " not found in navigation drawer");
        }
    }

    private ViewInteraction checkFolderInDrawerToFindName(
            String folder,
            Matcher<View> viewMatcher)
    {
        try {
            return onView(viewMatcher)
                    .check(matches(withText(CoreMatchers.endsWith(folder))));
        } catch (AssertionFailedError e) {
            return null;
        }
    }

    private void waitForTextOnScreen(String text) {
        boolean textIsOk = false;
        do {
            waitForIdle();
            try {
                textIsOk = getTextFromTextViewThatContainsText(text).contains(resources.getString(R.string.special_mailbox_name_sent));
            } catch (Exception e) {
                Timber.i("Text is not on the screen");
            }
        } while (!textIsOk);
    }

    public boolean waitForMessageAndClickIt() {
        Timber.i("MessageList antes: " + messageListSize[0] + " " + messageListSize[1]);
        waitForNewMessage();
        Timber.i("MessageList despues: " + messageListSize[0] + " " + messageListSize[1]);
        return clickLastMessage();
    }

    public String longText() {
        return "Lorem ipsum dolor sit amet consectetur adipiscing elit lectus neque, nulla eros ullamcorper phasellus egestas sagittis ridiculus cursus montes, morbi taciti hendrerit sed metus nam nascetur velit. Placerat nascetur congue risus mollis felis in nisl, fames arcu nunc nostra ultricies taciti, massa conubia rutrum commodo augue vivamus. Quisque aliquam sem nostra purus inceptos velit cubilia arcu, netus aliquet sodales at a ad consequat magna odio, dui duis suscipit orci nulla tellus massa." +
                "Mus urna dis enim curabitur erat nisi aenean imperdiet porttitor nulla ad velit, rutrum senectus congue morbi nisl duis pretium augue volutpat et ac vulputate auctor, sodales mi sociosqu facilisis convallis habitant tempor tortor massa at lectus. Sed aliquet sapien sollicitudin fusce cubilia felis consequat malesuada justo lacinia tincidunt viverra, magnis arcu commodo maecenas cum purus potenti massa himenaeos odio. Natoque sodales mauris proin gravida malesuada, faucibus lacinia neque pellentesque, habitant nisl porta velit.";
    }

    public String longWord() {
        String word = "ji";
        for (int i = 0; i < 10; i++) {
            word += word;
        }
        return word;
    }

    public String specialCharacters() {
        return "¡¢£¤¥§¨©ª«¬®¯°±´µ¶·¸º»¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÑÕØßàáâãäåæçñõ÷øÿ₫¦²³¼½¾ÐðÞþ×αΓβΛγΣπΠσΩς<>≠′≤″≥∂≡∫≈∑∞∏√•€™†‡◊‰←↑→↓♠♣♥♦‹›☺☻♀♂♪♫►ΦΘε~бвгґѓдђєжийлљњћќўфцчџшщъыьэюяスーパーオファーお客様に限り貸切可";
    }

    public void  insertTextNTimes (String messageText, int repetitionsOfTheText) {
        waitForIdle();
        BySelector selector = By.clazz("android.widget.EditText");
        UiObject2 uiObject = device.findObject(By.res(BuildConfig.APPLICATION_ID + ":id/message_content"));
        UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
        for (UiObject2 object : device.findObjects(selector)) {
            if (object.getResourceName().equals(uiObject.getResourceName())) {
                while (!exists(onView(withId(R.id.message_content)))) {
                    waitForIdle();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    while (!object.getText().contains(messageText)) {
                        waitForIdle();
                        object.click();
                        String finalMessageText = messageText;
                        for (int i = 0; i < repetitionsOfTheText; i++) {
                            finalMessageText = finalMessageText + messageText;
                        }
                        waitForIdle();
                        String finalMessageTextTemp = finalMessageText;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                setClipboard(finalMessageTextTemp);
                            }
                        });
                        //for (int i = 0; i < repetitionsOfTheText; i++) {
                            waitForIdle();
                            pasteClipboard();
                            waitForIdle();
                            Thread.sleep(2000);
                            //scroll.swipe(Direction.DOWN, 1f);
                            waitForIdle();
                        //}
                        object.click();
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot fill long text: " + ex.getMessage());
                }
            }
        }
        for (int i = 0; i < repetitionsOfTheText / 4; i++) {
            scroll.swipe(Direction.DOWN, 1f);
        }
        waitForIdle();
        scrollUpToSubject();
    }

    public boolean clickLastMessage() {
        boolean messageClicked = false;
        boolean encrypted = false;
        while (!messageClicked) {
            waitForIdle();
            if (!viewIsDisplayed(R.id.openCloseButton)) {
                try {
                    swipeDownMessageList();
                    waitForIdle();
                    while (viewIsDisplayed(R.id.message_list)) {
                        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
                        messageClicked = true;
                        waitForIdle();
                    }
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
                waitForIdle();
            } else {
                messageClicked = true;
            }
        }
        if (viewIsDisplayed(R.id.error_message)) {
            encrypted = true;
        }
        try {
            readAttachedJSONFile();
        } catch (Exception noJSON) {
            Timber.i("There are no JSON files attached");
        }
        waitForIdle();
        return encrypted;
    }

    public void clickMessageAtPosition(int position) {
        boolean messageClicked = false;
        while (!messageClicked) {
            waitForIdle();
            if (!viewIsDisplayed(R.id.openCloseButton)) {
                try {
                    swipeDownMessageList();
                    waitForIdle();
                    while (viewIsDisplayed(R.id.message_list) || messageClicked) {
                        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(position - 1).perform(click());
                        messageClicked = true;
                        waitForIdle();
                        waitUntilIdle();
                    }
                    if (viewIsDisplayed(R.id.fab_button_compose_message)) {
                        try {
                            messageClicked = false;
                            while (exists(onView(withId(R.id.delete)))) {
                                waitForIdle();
                                pressBack();
                                waitForIdle();
                            }
                        } catch (Exception ex) {
                            Timber.i("Last message has been clicked");
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("No message found");
                }
                waitForIdle();
            } else {
                messageClicked = true;
            }
        }
        try {
            onView(withText(R.string.cancel_action)).perform(click());
        } catch (NoMatchingViewException ignoredException) {
            Timber.i("Ignored exception. Email is not encrypted");
        }
        try {
            readAttachedJSONFile();
        } catch (Exception noJSON) {
            Timber.i("There are no JSON files attached");
        }
        waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        waitForIdle();
    }

    public void longClickMessageAtPosition(int position) {
        waitForIdle();
        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(position - 1).perform(ViewActions.longClick());
    }

    public void emptyFolder (String folderName) {
        waitForIdle();
        File dir = new File(Environment.getExternalStorageDirectory()+"/" + folderName + "/");
        if (!dir.exists()) {
            dir = new File(context.getExternalFilesDir(null)+"/" + folderName + "/");
        }
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (String aChildren : children) {
                new File(dir, aChildren).delete();
            }
        }
    }

    private void readAttachedJSONFile() {
        emptyFolder("Download");
        try {
            onView(withId(R.id.to)).perform(closeSoftKeyboard());
        } catch (Exception ex) {
            Timber.i("Cannot close keyboard");
        }
        downloadJSon();
    }

    private void downloadJSon() {
        waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        for (int i=0; i<8; i++){
            waitForIdle();
            onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
            swipeUpScreen();
        }
        json = null;
        waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        waitForIdle();
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains("results.json")) {
                    waitForIdle();
                    while (json == null) {
                        try {
                            Thread.sleep(2000);
                            downloadAttachedFile("results.json");
                            waitForIdle();
                            json = getJSON();
                        } catch (Exception ex) {
                            swipeUpScreen();
                            boolean jsonExists = false;
                                try {
                                    waitForIdle();
                                    if (object.getText().contains("results.json")) {
                                        jsonExists = true;
                                    }
                                } catch (Exception json) {
                                    Timber.i("Cannot find json file on the screen: " + json);
                                }
                            if (!jsonExists) {
                                waitForIdle();
                                return;
                            }
                        }
                    }
                    return;
                } else {
                    swipeUpScreen();
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
    }

    public static void saveJSON() {
        json = getJSON();
    }

    private static JSONObject getJSON(){
        try {
            File directory = new File(Environment.getExternalStorageDirectory().toString() + "/Download/");
            String js = null;
            waitForIdle();
            File[] listOfFiles = directory.listFiles();
            assert listOfFiles != null;
            if (listOfFiles.length > 0) {
               js  = readJsonFile(listOfFiles[0].getName());
            }
            JSONObject jsonObject = new JSONObject(js);
            return jsonObject;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static void downloadAttachedFile(String fileName) {
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains(fileName)) {
                    waitForIdle();
                    onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
                    waitForIdle();
                    object.getParent().getChildren().get(0).click();
                    waitForIdle();
                    onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
                    return;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
    }

    public void expandFolderFromNavigationMenu (String folderName) {
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().equals(folderName)) {
                    waitForIdle();
                    object.getParent().getChildren().get(1).longClick();
                    waitForIdle();
                    //object.getParent().getChildren().get(1).click();
                    waitForIdle();
                    return;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
    }

    /*public void expandFolderFromNavigationMenu (String folderName) {
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().equals(folderName)) {
                    waitForIdle();
                    object.getParent().getChildren().get(1).longClick();
                    waitForIdle();
                    //object.getParent().getChildren().get(1).click();
                    waitForIdle();
                    return;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
    }*/

    public JSONObject returnJSON (){
        return json;
    }

    public void waitForNewMessages(int totalMessages) {
        for (int waitMessage = 0; waitMessage < totalMessages; waitMessage++){
            waitForNewMessage();
        }
    }

    public void waitForNMessageInTheLIst(int messages) {
        waitForIdle();
        while (!exists(onView(withId(R.id.message_list)))){
            waitForIdle();
        }
        while (true) {
            try {
                waitForIdle();
                swipeDownMessageList();
                waitForIdle();
                onView(withId(R.id.message_list)).check(matches(isDisplayed()));
                onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 0));
                if (messages + 1 == messageListSize[0]){
                    getMessageListSize();
                    return;
                }
                Thread.sleep(2000);
            } catch (Exception ex) {
                Timber.i("Waiting for new message : " + ex);
            }
        }

    }

    public void pressShowPicturesButton() {
        while (true) {
            try {
                BySelector selector = By.clazz("android.widget.TextView");
                waitForIdle();
                for (UiObject2 textView : device.findObjects(selector)) {
                    if (textView.getText() != null && textView.getText().contains("SHOW PICTURES")) {
                        textView.click();
                        return;
                    }
                }
                swipeDownScreen();
            } catch (Exception noButton) {
                Timber.i("Cannot find SHOW PICTURES button");
            }
        }
    }

    public void waitForNewMessage() {
        boolean newEmail = false;
        waitForIdle();
        while (!exists(onView(withId(R.id.message_list)))) {
            waitForIdle();
        }
        //doWaitForResource(R.id.message_list);
        //doWaitForIdlingListViewResource(R.id.message_list);
        //onView(withId(R.id.message_list)).check(matches(isDisplayed()));
        waitForIdle();
        while (!newEmail) {
            try {
                waitForIdle();
                swipeDownMessageList();
                waitForIdle();
                if (viewIsDisplayed(R.id.message_list)) {
                    onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 1));
                    if (messageListSize[1] > messageListSize[0]) {
                        newEmail = true;
                        waitForIdle();
                    }
                }
            } catch (Exception ex) {
                Timber.i("Waiting for new message : " + ex);
            }
        }
        getMessageListSize();
    }

    public void assertThereAreXMessages(int numberOfMessages) {
        waitForIdle();
        while (!exists(onView(withId(R.id.message_list)))) {
            waitForIdle();
        }
        onView(withId(R.id.message_list)).check(matches(isDisplayed()));
        waitForIdle();
        onView(withId(R.id.message_list)).check(matches(isDisplayed()));
        onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 1));
        if (messageListSize[1] != numberOfMessages) {
            fail("Wrong number of messages");
        }
        getMessageListSize();
    }

    public void getMessageListSize() {
        waitForIdle();
        if (getTextFromView(onView(withId(R.id.actionbar_title_first))).equals(resources.getString(R.string.special_mailbox_name_inbox))) {
            swipeDownMessageList();
            waitForIdle();
            while (exists(onView(withId(R.id.message_list)))) {
                try {
                    waitForIdle();
                    onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 0));
                    return;
                } catch (Exception ex) {
                    Timber.i("Cannot find view message_list: " + ex.getMessage());
                }
            }
            waitForIdle();
        }
    }

    public int getListSize() {
        return messageListSize[0];
    }

    public void swipeDownMessageList() {
        while (true) {
            try {
                Thread.sleep(2000);
                waitForIdle();
                onView(withId(R.id.message_list)).perform(swipeDown());
                waitForIdle();
                Thread.sleep(2000);
                return;
            } catch (Exception e) {
                Timber.i("Cannot swipe down");
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
                    waitForIdle();
                    onView(withText(R.string.cancel_action)).perform(click());
                } catch (NoMatchingViewException ignoredException) {
                    Timber.i("Ignored exception");
                }
                try {
                    waitForIdle();
                    onView(withId(R.id.delete)).perform(click());
                } catch (NoMatchingViewException ignoredException) {
                    emptyList = true;
                }
                waitForIdle();
                if (exists(onView(withId(android.R.id.message)))) {
                    emptyList = false;
                }
            }
        }
    }

    public void clickFirstMessage(){
        while (!viewIsDisplayed(R.id.message_list)) {
            waitForIdle();
        }
        while ((exists(onView(withId(R.id.message_list))) || viewIsDisplayed(R.id.message_list))
         && (!viewIsDisplayed(R.id.openCloseButton))){
            try{
                    waitForIdle();
                    swipeDownMessageList();
                    waitForIdle();
                    getMessageListSize();
                    if (viewIsDisplayed(R.id.openCloseButton)) {
                        return;
                    }
                    else {
                        waitForIdle();
                        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
                        waitForIdle();
                        readAttachedJSONFile();
                    }
            } catch (Exception ex){
                Timber.i("Cannot find list: " + ex);
            }
        }
        waitForIdle();
    }

    public void checkToolbarColor(int color) {
        waitForIdle();
        while (!exists(onView(withId(R.id.toolbar)))) {
            doWaitForResource(R.id.toolbar);
            waitForIdle();
        }
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        waitForIdle();
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(color)));
    }

    void goBackToMessageListAndPressComposeMessageButton() {
        boolean backToMessageList = false;
        Activity currentActivity = getCurrentActivity();
        while (!backToMessageList){
            try {
                pressBack();
                waitForIdle();
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

    public void setClipboard(String textToCopy) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            clipboard.clearPrimaryClip();
        } catch (Throwable tr) {
            Timber.i("");
        }
        ClipData clip = ClipData.newPlainText("", textToCopy);
        clipboard.setPrimaryClip(clip);
        while (clipboard.getPrimaryClip() == null || clipboard.getPrimaryClip().toString().equals("")) {
            device.waitForIdle();
            try {
                clipboard.setPrimaryClip(clip);
            } catch (Throwable th) {
                Timber.i("");
            }
        }
    }

    public void pasteClipboard() {
        waitForIdle();
        UiDevice.getInstance(getInstrumentation())
                .pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_MASK);
        waitForIdle();
    }

    public void compareMessageBody(String cucumberBody) {
        String [] body;
        while (!viewIsDisplayed(R.id.to)) {
            waitForIdle();
        }
        swipeUpScreen();
        waitUntilIdle();
        doWaitForResource(R.id.message_container);
        while (true) {
            waitForIdle();
            if (exists(onView(withId(R.id.message_container)))) {
                onView(withId(R.id.message_container)).check(matches(isDisplayed()));
                if (cucumberBody.equals("Rating/DecodedRating")) {
                    body = new String[2];
                    body[0] = "Rating | 6";
                    body[1] = "Decoded Rating | PEP_rating_reliable";
                } else {
                    body = new String[1];
                    body[0] = cucumberBody.substring(0, cucumberBody.length() - 1);
                }
                compareTextWithWebViewText(body[0]);
                return;
            } else if (exists(onView(withId(R.id.message_content)))) {
                onView(withId(R.id.message_content)).check(matches(isDisplayed()));
                String[] text = getTextFromView(onView(withId(R.id.message_content))).split("--");
                if (text[0].contains(cucumberBody)) {
                    return;
                } else {
                    waitForIdle();
                    onView(withId(R.id.toolbar_container)).check(matches(isDisplayed()));
                    fail("Error: BODY TEXT=" + text[0] + " ---*****--- TEXT TO COMPARE=" + cucumberBody);
                }
            }
        }
    }

    public void compareMessageBodyLongText(String cucumberBody) {
        onView(withId(R.id.toolbar_container)).check(matches(isDisplayed()));
        swipeUpScreen();
        waitUntilIdle();
        BySelector selector = By.clazz("android.widget.EditText");
        UiObject2 uiObject = device.findObject(By.res(BuildConfig.APPLICATION_ID + ":id/message_content"));
        for (UiObject2 object : device.findObjects(selector)) {
            if (object.getResourceName().equals(uiObject.getResourceName())) {
                waitForIdle();
                onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
                waitForIdle();
                String messageBody = object.getText().replaceAll("\n", "");
                if (!messageBody.contains(cucumberBody)) {
                    fail("Error: body text != textToCompare --> bodyText = " + object.getText() + " ************  !=  *********** textToCompare = " +cucumberBody);
                }
                return;
            } else {
                waitForIdle();
                onView(withId(R.id.toolbar_container)).check(matches(isDisplayed()));
            }
        }
        waitUntilIdle();
    }

    private void compareTextWithWebViewText(String textToCompare) {
        boolean bodyRead = false;
        UiObject2 wb;
        String[] webViewText = new String[1];
        waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        waitForIdle();
        while (!bodyRead) {
            try {
                waitForIdle();
                wb = device.findObject(By.clazz("android.webkit.WebView"));
                wb.click();
                swipeUpScreen();
                while (webViewText[0] == null) {
                    waitForIdle();
                    Timber.i("Trying to find webView text");
                    if (wb.getChildren().get(0).getText() != null) {
                        webViewText = wb.getChildren().get(0).getText().split("\n");
                    } else if (wb.getChildren().get(0).getChildren().get(0).getContentDescription() != null) {
                        webViewText = wb.getChildren().get(0).getChildren().get(0).getContentDescription().split("\n");
                    }
                    bodyRead = true;
                }
            } catch (Exception ex) {
                Timber.i("Cannot find webView: " + ex.getMessage());
            }
        }
        if (!webViewText[0].contains(textToCompare)) {
            fail("Message Body text is different");
        }
    }

    public void rotateDevice() {
        try {
            waitForIdle();
            device.setOrientationLeft();
            waitForIdle();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            device.setOrientationNatural();
            for (int i = 0; i < 5; i++) {
                waitForIdle();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startActivity() {
        device.pressHome();
        final String launcherPackage = getLauncherPackageName();
        //assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return context.checkCallingOrSelfPermission(ANIMATION_PERMISSION);
    }

    public void checkBoxOnScreenChecked(int resource, boolean check) {
        boolean textViewFound = false;
        BySelector selector = By.clazz("android.widget.TextView");
        while (!textViewFound) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().contains(resources.getString(resource))) {
                        waitForIdle();
                        UiObject2 checkbox = object.getParent().getParent().getChildren().get(1).getChildren().get(0);
                        if (checkbox.isChecked() != check){
                            waitForIdle();
                            checkbox.longClick();
                            waitForIdle();
                        }
                        if (checkbox.isChecked() == check) {
                            waitForIdle();
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

    public void assertCheckBox(int resource, boolean check) {
        boolean textViewFound = false;
        BySelector selector = By.clazz("android.widget.TextView");
        while (!textViewFound) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().contains(resources.getString(resource))) {
                        waitForIdle();
                        UiObject2 checkbox = object.getParent().getParent().getChildren().get(1).getChildren().get(0);
                        if (checkbox.isChecked() == check) {
                            return;
                        } else {
                            textViewFound = true;
                        }
                    }
                } catch (Exception ex){
                    Timber.i("Cannot find CheckBox on screen: " + ex);
                }
            }
        }
        fail("CheckBox " +  resources.getString(resource) + " is not " + check);
    }

    public void getScreenShot() {
        waitForIdle();
        File imageDir = new File(Environment.getExternalStorageDirectory().toString());
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }
        device.takeScreenshot(new File("$IMAGE_DIR$index $className ${action}.png"), 0.5f, 25);
    }

    public void scrollUpToSubject (){
        scrollUpToID(R.id.subject, true);
    }

    public void scrollDownToSubject (){
        scrollUpToID(R.id.subject, false);
    }

    private void scrollUpToID (int id, boolean up) {
        UiObject2 scroll;
        do {
            try {
                scroll = device.findObject(By.clazz("android.widget.ScrollView"));
                waitForIdle();
                if (up){
                    scroll.swipe(Direction.DOWN, 1.0f);
                } else {
                    scroll.swipe(Direction.UP, 1.0f);
                }
                waitForIdle();
            } catch (Exception e) {
                pressBack();
            }
        } while (!viewIsDisplayed(id));
        onView(withId(id)).check(matches(isCompletelyDisplayed()));
        onView(withId(id)).perform(click());
    }

    public void scrollDownToView (int view) {
        while (!viewIsDisplayed(view)) {
            try {
                waitForIdle();
                onView(withId(R.id.message_list)).perform(swipeUp());
                waitForIdle();
            } catch (Exception e) {
                Timber.i("Cannot swipe down");
            }
        }
    }

    public void scrollToCheckBoxAndSetIt(boolean isChecked, int view) {
        scrollToView(resources.getString(view));
        if (isChecked) {
            checkBoxOnScreenChecked(view, false);
        }
        checkBoxOnScreenChecked(view, true);
        if (!isChecked) {
            checkBoxOnScreenChecked(view, false);
        }
    }

    public void scrollToCheckBoxAndAssertIt(boolean isChecked, int view) {
        scrollToView(resources.getString(view));
        assertCheckBox(view, isChecked);
    }

    public void scrollToViewAndClickIt(int view) {
        scrollToView(resources.getString(view));
        selectFromScreen(view);
    }

    public void scrollToView (String text){
        waitForIdle();
        UiObject textView = device.findObject(new UiSelector().text(text).className("android.widget.TextView"));
        waitForIdle();
        Espresso.onIdle();
        try {
            textView.dragTo(1000,1000,40);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        waitForIdle();
    }

    public void scrollUpNavigation (){
        waitForIdle();
        UiObject textView = device.findObject(new UiSelector().text("Inbox").className("android.widget.TextView"));
        waitForIdle();
        try {
            textView.dragTo(1000,100,40);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        waitForIdle();
    }

    public void scrollUpToView (int view){
        UiObject2 scroll;
        do {
            try {
                scroll = device.findObject(By.clazz("android.widget.ScrollView"));
                waitForIdle();
                scroll.swipe(Direction.DOWN, 1.0f);
                waitForIdle();
            } catch (Exception e) {
                pressBack();
            }
        } while (!viewIsDisplayed(view));
        onView(withId(view)).check(matches(isCompletelyDisplayed()));
    }

    public void setCheckBox(String resourceText, boolean checked) {
        BySelector selector = By.clazz("android.widget.CheckBox");
        while (true) {
            for (UiObject2 checkbox : device.findObjects(selector)) {
                try {
                    if (checkbox.getText().contains(resourceText)) {
                        waitForIdle();
                        if (checkbox.isChecked() != checked){
                            waitForIdle();
                            checkbox.longClick();
                            waitForIdle();
                        }
                        if (checkbox.isChecked() == checked) {
                            waitForIdle();
                            return;
                        }
                    }
                } catch (Exception ex){
                    Timber.i("Cannot find checkbox in screen: " + ex);
                }
            }
        }
    }

    public void selectItemFromDialogListView (int item, boolean boxChecked) {
        BySelector selector = By.clazz("android.widget.ListView");
        waitForIdle();
        while (true) {
            for (UiObject2 listView : device.findObjects(selector)) {
                try {
                    if (listView.getResourceName().equals("android:id/select_dialog_listview")
                            || listView.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/select_dialog_listview")) {
                        if (listView.getChildren().get(item).isChecked() != boxChecked) {
                            listView.getChildren().get(item).click();
                        } else {
                            boolean existsOKButton = false;
                            selector = By.clazz("android.widget.Button");
                            for (UiObject2 button : device.findObjects(selector)) {
                                if (button.getResourceName().equals("android:id/button1")) {
                                    existsOKButton = true;
                                }
                            }
                            if (!existsOKButton) {
                                pressBack();
                            }
                        }
                        waitForIdle();
                        return;
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find item in the dialog list: " + ex);
                }
            }
        }
    }

    public void assertItemFromDialogListViewIsSelected(int item, boolean isSelected) {
        BySelector selector = By.clazz("android.widget.ListView");
        boolean itemSelected = true;
        waitForIdle();
        while (itemSelected) {
            for (UiObject2 listView : device.findObjects(selector)) {
                try {
                    if (listView.getResourceName().equals("android:id/select_dialog_listview")
                            || listView.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/select_dialog_listview")) {
                        if (listView.getChildren().get(item).isChecked() != isSelected) {
                            itemSelected = false;
                        } else {
                            pressBack();
                            return;
                        }
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find dialog list or item in the list: " + ex);
                }
            }
        }
        fail("Item " + item + " of the list is not " + isSelected);
    }

    public void setTimeInRadialPicker (int hour) {
        BySelector selector = By.clazz("android.view.View");
        waitForIdle();
        while (true) {
            for (UiObject2 clock : device.findObjects(selector)) {
                try {
                    if (clock.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/radial_picker")) {
                        clock.getChildren().get(hour).click();
                        return;
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find the time in the screen: " + ex);
                }
            }
        }
    }

    public void checkTimeInRadialPickerIsSelected (int hour) {
        BySelector selector = By.clazz("android.widget.RelativeLayout");
        boolean timeAssertDone = false;
        waitForIdle();
        while (!timeAssertDone) {
            for (UiObject2 time : device.findObjects(selector)) {
                try {
                    if (time.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/time_header")) {
                        if (!time.getChildren().get(0).getText().contains((String.valueOf(hour)))){
                            fail("Time is " + time.getText() + " and it should be " + hour);
                        }
                        timeAssertDone = true;
                        break;
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find the time in the screen: " + ex);
                }
            }
        }
        selector = By.clazz("android.view.View");
        waitForIdle();
        while (true) {
            for (UiObject2 clock : device.findObjects(selector)) {
                try {
                    if (clock.getResourceName().equals(BuildConfig.APPLICATION_ID + ":id/radial_picker")) {
                        if (!clock.getChildren().get(hour).isSelected()) {
                            fail("Wrong time selected");
                        }
                        return;
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find the time in the screen: " + ex);
                }
            }
        }
    }

    public void goToDisplayAndAssertSettings () {
        selectFromScreen(stringToID("display_preferences"));
        selectFromScreen(stringToID("font_size_settings_title"));
        selectFromScreen(stringToID("font_size_account_list"));
        selectFromScreen(stringToID("font_size_account_name"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_account_description"));
        assertItemFromDialogListViewIsSelected(2, true);
        pressBack();
        selectFromScreen(stringToID("font_size_folder_list"));
        selectFromScreen(stringToID("font_size_folder_name"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_folder_status"));
        assertItemFromDialogListViewIsSelected(2, true);
        pressBack();
        selectFromScreen(stringToID("font_size_message_list"));
        selectFromScreen(stringToID("font_size_message_list_subject"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_list_sender"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_list_date"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_list_preview"));
        assertItemFromDialogListViewIsSelected(2, true);
        pressBack();
        selectFromScreen(stringToID("font_size_message_view"));
        selectFromScreen(stringToID("font_size_message_list_sender"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_view_to"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_view_cc"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_list_subject"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_view_date"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("font_size_message_view_additional_headers"));
        assertItemFromDialogListViewIsSelected(2, true);
        pressBack();
        selectFromScreen(stringToID("font_size_message_compose"));
        selectFromScreen(stringToID("font_size_message_compose_input"));
        assertItemFromDialogListViewIsSelected(2, true);
        pressBack();
        pressBack();
        scrollToCheckBoxAndAssertIt(false, stringToID("animations_title"));
        scrollToView(resources.getString(R.string.accountlist_preferences));
        scrollToCheckBoxAndAssertIt(false, stringToID("measure_accounts_title"));
        scrollToCheckBoxAndAssertIt(false, stringToID("count_search_title"));
        scrollToView(resources.getString(stringToID("folderlist_preferences")));
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_folderlist_wrap_folder_names_label"));
        scrollToView(resources.getString(stringToID("messagelist_preferences")));
        scrollToViewAndClickIt(stringToID("global_settings_preview_lines_label"));
        assertItemFromDialogListViewIsSelected(3, true);
        scrollToCheckBoxAndAssertIt(false, stringToID("global_settings_flag_label"));
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_checkbox_label"));
        scrollToCheckBoxAndAssertIt(false, stringToID("global_settings_show_correspondent_names_label"));
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_sender_above_subject_label"));
        //scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_show_contact_name_label"));
        scrollToCheckBoxAndAssertIt(false, stringToID("global_settings_show_contact_picture_label"));
        //scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_colorize_missing_contact_pictures_label"));
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_background_as_unread_indicator_label"));
        scrollToCheckBoxAndAssertIt(false, stringToID("global_settings_threaded_view_label"));
        scrollToView(resources.getString(stringToID("messageview_preferences")));
        selectFromScreen(stringToID("global_settings_messageview_visible_refile_actions_title"));
        assertItemFromDialogListViewIsSelected(0, false);
        selectFromScreen(stringToID("global_settings_messageview_visible_refile_actions_title"));
        assertItemFromDialogListViewIsSelected(1, true);
        selectFromScreen(stringToID("global_settings_messageview_visible_refile_actions_title"));
        assertItemFromDialogListViewIsSelected(2, true);
        selectFromScreen(stringToID("global_settings_messageview_visible_refile_actions_title"));
        assertItemFromDialogListViewIsSelected(3, true);
        selectFromScreen(stringToID("global_settings_messageview_visible_refile_actions_title"));
        assertItemFromDialogListViewIsSelected(4, true);
        //pressOKButtonInDialog();
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_messageview_autofit_width_label"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_messageview_fixedwidth_label"));
        pressBack();
    }

    public void goToInteractionAndAssertSettings () {
        selectFromScreen(stringToID("interaction_preferences"));
        scrollToCheckBoxAndAssertIt(true, stringToID("gestures_title"));
        scrollToViewAndClickIt(stringToID("volume_navigation_title"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("volume_navigation_title"));
        assertItemFromDialogListViewIsSelected(1, true);
        scrollToView(resources.getString(R.string.global_settings_messageiew_after_delete_behavior_title));
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_messageview_return_to_list_label"));
        scrollToCheckBoxAndAssertIt(false, stringToID("global_settings_messageview_show_next_label"));
        scrollToViewAndClickIt(stringToID("global_settings_confirm_actions_title"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("global_settings_confirm_actions_title"));
        assertItemFromDialogListViewIsSelected(1, true);
        scrollToViewAndClickIt(stringToID("global_settings_confirm_actions_title"));
        assertItemFromDialogListViewIsSelected(2, false);
        scrollToViewAndClickIt(stringToID("global_settings_confirm_actions_title"));
        assertItemFromDialogListViewIsSelected(3, true);
        scrollToViewAndClickIt(stringToID("global_settings_confirm_actions_title"));
        assertItemFromDialogListViewIsSelected(4, false);
        scrollToViewAndClickIt(stringToID("global_settings_confirm_actions_title"));
        assertItemFromDialogListViewIsSelected(5, false);
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        scrollToCheckBoxAndAssertIt(true, stringToID("start_integrated_inbox_title"));
        pressBack();
    }

    public void goToNotificationsAndAssertSettings () {
        selectFromScreen(stringToID("notifications_title"));
        scrollToCheckBoxAndAssertIt(true, stringToID("quiet_time"));
        //scrollToCheckBoxAndAssertIt(false, stringToID("quiet_time_notification"));
        scrollToViewAndClickIt(stringToID("quiet_time_starts"));
        checkTimeInRadialPickerIsSelected(1);
        pressOKButtonInDialog();
        scrollToViewAndClickIt(stringToID("quiet_time_ends"));
        checkTimeInRadialPickerIsSelected(4);
        pressOKButtonInDialog();
        scrollToViewAndClickIt(stringToID("global_settings_notification_quick_delete_title"));
        assertItemFromDialogListViewIsSelected(2, true);
        scrollToViewAndClickIt(stringToID("global_settings_lock_screen_notification_visibility_title"));
        assertItemFromDialogListViewIsSelected(3, true);
        pressBack();
    }

    public void goToPrivacyAndAssertSettings () {
        selectFromScreen(stringToID("privacy_preferences"));
        scrollToCheckBoxAndAssertIt(true, stringToID("pep_passive_mode"));
        //scrollToCheckBoxAndAssertIt(true, stringToID("pep_forward_warning"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        //scrollToViewAndClickIt(stringToID("master_key_management"));
        //pressBack();
        scrollToView(resources.getString(stringToID("pep_sync")));
        scrollToView(resources.getString(stringToID("pep_sync_folder")));
        scrollToCheckBoxAndAssertIt(false, stringToID("pep_subject_protection"));
        scrollToView(resources.getString(stringToID("blacklist_title")));
        scrollToCheckBoxAndAssertIt(true, stringToID("global_settings_privacy_hide_timezone"));
        pressBack();
    }

    public void goToAdvancedAndAssertSettings () {
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        scrollToViewAndClickIt(stringToID("background_ops_label"));
        assertItemFromDialogListViewIsSelected(1, true);
        //checkItemFromDialogListViewIsSelected(1, true);
        //selectItemFromDialogListView(1, true);
        //scrollToCheckBoxAndAssertIt(false, stringToID("debug_enable_debug_logging_title"));
        scrollToCheckBoxAndAssertIt(true, stringToID("debug_enable_sensitive_logging_title"));
        pressBack();
    }

    private void aboutMenu () {
        openOptionsMenu();
        selectFromScreen(stringToID("about_action"));
        String aboutText = getTextFromView(onView(withId(R.id.aboutText)));
        String librariesText = getTextFromView(onView(withId(R.id.librariesText)));
        String[][] shortTextInAbout = new String[3][2];
        shortTextInAbout[0] = resources.getString(stringToID("app_authors_fmt")).split("%");
        shortTextInAbout[1] = resources.getString(stringToID("app_libraries")).split("%");
        shortTextInAbout[2] = resources.getString(stringToID("app_copyright_fmt")).split("%");
        if (!aboutText.contains(shortTextInAbout[0][0])
                || !librariesText.contains(shortTextInAbout[1][0])
                || !aboutText.contains(shortTextInAbout[2][0])) {
            fail("Wrong text in About");
        }
        pressBack();
    }

        public void goToDisplayAndChangeSettings () {
        selectFromScreen(stringToID("display_preferences"));
        selectFromScreen(stringToID("font_size_settings_title"));
        selectFromScreen(stringToID("font_size_account_list"));
        selectFromScreen(stringToID("font_size_account_name"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_account_description"));
        selectItemFromDialogListView(2, true);
        pressBack();
        selectFromScreen(stringToID("font_size_folder_list"));
        selectFromScreen(stringToID("font_size_folder_name"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_folder_status"));
        selectItemFromDialogListView(2, true);
        pressBack();
        selectFromScreen(stringToID("font_size_message_list"));
        selectFromScreen(stringToID("font_size_message_list_subject"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_list_sender"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_list_date"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_list_preview"));
        selectItemFromDialogListView(2, true);
        pressBack();
        selectFromScreen(stringToID("font_size_message_view"));
        selectFromScreen(stringToID("font_size_message_list_sender"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_view_to"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_view_cc"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_list_subject"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_view_date"));
        selectItemFromDialogListView(2, true);
        selectFromScreen(stringToID("font_size_message_view_additional_headers"));
        selectItemFromDialogListView(2, true);
        //selectFromScreen(stringToID("font_size_message_view_content"));
        //selectItemFromDialogListView(2);
        pressBack();
        selectFromScreen(stringToID("font_size_message_compose"));
        selectFromScreen(stringToID("font_size_message_compose_input"));
        selectItemFromDialogListView(2, true);
        pressBack();
        pressBack();
        scrollToCheckBoxAndSetIt(false, stringToID("animations_title"));
        scrollToView(resources.getString(R.string.accountlist_preferences));
        scrollToCheckBoxAndSetIt(false, stringToID("measure_accounts_title"));
        scrollToCheckBoxAndSetIt(false, stringToID("count_search_title"));
        scrollToView(resources.getString(stringToID("folderlist_preferences")));
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_folderlist_wrap_folder_names_label"));
        scrollToView(resources.getString(stringToID("messagelist_preferences")));
        scrollToViewAndClickIt(stringToID("global_settings_preview_lines_label"));
        selectItemFromDialogListView(3, true);
        scrollToCheckBoxAndSetIt(false, stringToID("global_settings_flag_label"));
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_checkbox_label"));
        scrollToCheckBoxAndSetIt(false, stringToID("global_settings_show_correspondent_names_label"));
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_sender_above_subject_label"));
        //scrollToCheckBoxAndCheckIt(true, stringToID("global_settings_show_contact_name_label"));
        scrollToCheckBoxAndSetIt(false, stringToID("global_settings_show_contact_picture_label"));
        //scrollToCheckBoxAndCheckIt(true, stringToID("global_settings_colorize_missing_contact_pictures_label"));
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_background_as_unread_indicator_label"));
        scrollToCheckBoxAndSetIt(false, stringToID("global_settings_threaded_view_label"));
        scrollToView(resources.getString(stringToID("messageview_preferences")));
        selectFromScreen(stringToID("global_settings_messageview_visible_refile_actions_title"));
        selectItemFromDialogListView(0, false);
        selectItemFromDialogListView(1, true);
        selectItemFromDialogListView(2, true);
        selectItemFromDialogListView(3, true);
        selectItemFromDialogListView(4, true);
        pressOKButtonInDialog();
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_messageview_autofit_width_label"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_messageview_fixedwidth_label"));
        pressBack();
    }

    public void goToInteractionAndChangeSettings () {
        selectFromScreen(stringToID("interaction_preferences"));
        scrollToCheckBoxAndSetIt(true, stringToID("gestures_title"));
        scrollToViewAndClickIt(stringToID("volume_navigation_title"));
        selectItemFromDialogListView(0, true);
        selectItemFromDialogListView(1, true);
        pressOKButtonInDialog();
        scrollToView(resources.getString(R.string.global_settings_messageiew_after_delete_behavior_title));
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_messageview_return_to_list_label"));
        scrollToCheckBoxAndSetIt(false, stringToID("global_settings_messageview_show_next_label"));
        scrollToViewAndClickIt(stringToID("global_settings_confirm_actions_title"));
        selectItemFromDialogListView(0, true);
        selectItemFromDialogListView(1, true);
        selectItemFromDialogListView(2, false);
        selectItemFromDialogListView(3, true);
        selectItemFromDialogListView(4, false);
        selectItemFromDialogListView(5, false);
        pressOKButtonInDialog();
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        scrollToCheckBoxAndSetIt(true, stringToID("start_integrated_inbox_title"));
        pressBack();
    }

    public void goToNotificationsAndChangeSettings () {
        selectFromScreen(stringToID("notifications_title"));
        scrollToCheckBoxAndSetIt(true, stringToID("quiet_time"));
        scrollToCheckBoxAndSetIt(false, stringToID("quiet_time_notification"));
        scrollToViewAndClickIt(stringToID("quiet_time_starts"));
        setTimeInRadialPicker(1);
        pressOKButtonInDialog();
        selectFromScreen(stringToID("quiet_time_ends"));
        setTimeInRadialPicker(4);
        pressOKButtonInDialog();
        scrollToViewAndClickIt(stringToID("global_settings_notification_quick_delete_title"));
        selectItemFromDialogListView(2, true);
        scrollToViewAndClickIt(stringToID("global_settings_lock_screen_notification_visibility_title"));
        selectItemFromDialogListView(3, true);
        pressBack();
    }

    public void goToPrivacyAndChangeSettings () {
        selectFromScreen(stringToID("privacy_preferences"));
        scrollToCheckBoxAndSetIt(true, stringToID("pep_passive_mode"));
        scrollToCheckBoxAndSetIt(true, stringToID("pep_forward_warning"));
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        //scrollToViewAndClickIt(stringToID("master_key_management"));
        //pressBack();
        scrollToView(resources.getString(stringToID("pep_sync")));
        scrollToView(resources.getString(stringToID("pep_sync_folder")));
        scrollToCheckBoxAndSetIt(false, stringToID("pep_subject_protection"));
        scrollToView(resources.getString(stringToID("blacklist_title")));
        scrollToCheckBoxAndSetIt(true, stringToID("global_settings_privacy_hide_timezone"));
        pressBack();
    }

    public void goToAdvancedAndChangeSettings () {
        selectFromScreen(stringToID("account_settings_push_advanced_title"));
        scrollToViewAndClickIt(stringToID("background_ops_label"));
        selectItemFromDialogListView(1, true);
        //scrollToCheckBoxAndCheckIt(false, stringToID("debug_enable_debug_logging_title"));
        scrollToCheckBoxAndSetIt(true, stringToID("debug_enable_sensitive_logging_title"));
        pressBack();
    }

    public void changeGlobalSettings () {
        aboutMenu();
        goToDisplayAndChangeSettings();
        goToInteractionAndChangeSettings();
        goToNotificationsAndChangeSettings();
        goToPrivacyAndChangeSettings();
        goToAdvancedAndChangeSettings();
    }

    public void assertGloblaSettings () {
        goToDisplayAndAssertSettings();
        goToInteractionAndAssertSettings();
        goToNotificationsAndAssertSettings();
        goToPrivacyAndAssertSettings();
        goToAdvancedAndAssertSettings();
    }

    public void goToAccountSettingsGeneralAccountAndChangeSettings() {
        selectFromScreen(stringToID("account_settings_general_title"));
        scrollToViewAndClickIt(stringToID("account_settings_description_label"));
        introduceTextInDialogWindow("newname");
        //scrollToCheckBoxAndCheckIt(false, stringToID("account_settings_default_label"));
        scrollToViewAndClickIt(stringToID("account_settings_show_pictures_label"));
        selectItemFromDialogListView(2, true);
        scrollToViewAndClickIt(stringToID("advanced"));
        scrollToCheckBoxAndSetIt(false, stringToID("account_settings_mark_message_as_read_on_view_label"));
        pressBack();
    }

    public void introduceTextInDialogWindow (String text) {
        BySelector selector = By.clazz("android.widget.EditText");
        boolean accountNameChanged = false;
        waitForIdle();
        while (!accountNameChanged) {
            for (UiObject2 editText : device.findObjects(selector)) {
                try {
                    if (editText.getResourceName().equals("android:id/edit")) {
                        editText.setText(text);
                        accountNameChanged = true;
                        break;
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find account name: " + ex);
                }
            }
        }
        pressOKButtonInDialog();
    }

    public void goToAccountSettingsFetchingAccountAndChangeSettings () {
        selectFromScreen(stringToID("account_settings_sync"));
        scrollToViewAndClickIt(stringToID("account_settings_incoming_label"));
        pressBack();
        selectFromScreen(stringToID("advanced"));
        scrollToViewAndClickIt(stringToID("account_settings_mail_display_count_label"));
        selectItemFromDialogListView(5, true);
        scrollToViewAndClickIt(stringToID("account_settings_message_age_label"));
        selectItemFromDialogListView(5, true);
        scrollToViewAndClickIt(stringToID("account_settings_autodownload_message_size_label"));
        selectItemFromDialogListView(10, true);
        scrollToViewAndClickIt(stringToID("account_settings_mail_check_frequency_label"));
        selectItemFromDialogListView(5, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_sync_mode_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_push_mode_label"));
        selectItemFromDialogListView(0, true);
        scrollToCheckBoxAndSetIt(false, stringToID("account_settings_sync_remote_deletetions_label"));
        scrollToViewAndClickIt(stringToID("account_setup_incoming_delete_policy_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("account_setup_expunge_policy_label"));
        selectItemFromDialogListView(2, true);
        scrollToCheckBoxAndSetIt(false, stringToID("push_poll_on_connect_label"));
        scrollToViewAndClickIt(stringToID("account_setup_push_limit_label"));
        selectItemFromDialogListView(6, true);
        scrollToViewAndClickIt(stringToID("idle_refresh_period_label"));
        selectItemFromDialogListView(3, true);
        pressBack();
    }

    public void goToAccountSettingsSendingEmailAndChangeSettings () {
        selectFromScreen(stringToID("account_settings_composition"));
        scrollToViewAndClickIt(stringToID("account_settings_composition_label"));
        onView(withId(R.id.account_name)).perform(closeSoftKeyboard());
        pressBack();
        scrollToViewAndClickIt(stringToID("account_settings_identities_label"));
        pressBack();
        scrollToViewAndClickIt(stringToID("account_settings_message_format_label"));
        selectItemFromDialogListView(2, true);
        scrollToCheckBoxAndSetIt(true, stringToID("account_settings_always_show_cc_bcc_label"));
        scrollToCheckBoxAndSetIt(false, stringToID("account_settings_default_quoted_text_shown_label"));
        scrollToViewAndClickIt(stringToID("account_settings_outgoing_label"));
        pressBack();
        scrollToViewAndClickIt(stringToID("advanced"));
        scrollToViewAndClickIt(stringToID("account_settings_quote_style_label"));
        selectItemFromDialogListView(0, true);
        scrollToCheckBoxAndSetIt(true, stringToID("account_settings_reply_after_quote_label"));
        scrollToCheckBoxAndSetIt(true, stringToID("account_settings_strip_signature_label"));
        scrollToViewAndClickIt(stringToID("account_settings_quote_prefix_label"));
        introduceTextInDialogWindow("prefixtext");
        pressBack();
    }

    public void goToAccountSettingsDefaultFoldersAndChangeSettings () {
        selectFromScreen(stringToID("account_settings_folders"));
        scrollToViewAndClickIt(stringToID("account_setup_auto_expand_folder"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_display_mode_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_target_mode_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("archive_folder_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("drafts_folder_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("sent_folder_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("spam_folder_label"));
        selectItemFromDialogListView(0, true);
        scrollToViewAndClickIt(stringToID("trash_folder_label"));
        selectItemFromDialogListView(0, true);
        pressBack();
    }

    public void goToAccountSettingsNotificationsAndChangeSettings () {
        selectFromScreen(stringToID("notifications_title"));
        scrollToCheckBoxAndSetIt(true, stringToID("account_settings_notify_label"));
        scrollToCheckBoxAndSetIt(true, stringToID("account_notify_contacts_mail_only_label"));
        //scrollToViewAndClickIt(stringToID("account_settings_notification_open_system_notifications_label"));
        scrollToViewAndClickIt(stringToID("advanced"));
        scrollToViewAndClickIt(stringToID("account_settings_folder_notify_new_mail_mode_label"));
        selectItemFromDialogListView(4, true);
        scrollToCheckBoxAndSetIt(false, stringToID("account_settings_notify_self_label"));
        scrollToCheckBoxAndSetIt(false, stringToID("account_settings_notify_sync_label"));
        scrollToCheckBoxAndSetIt(true, stringToID("account_settings_notification_opens_unread_label"));
        pressBack();
    }

    public void goToAccountSettingsSearchAndChangeSettings () {
        selectFromScreen(stringToID("account_settings_search"));
        scrollToCheckBoxAndSetIt(true, stringToID("account_settings_remote_search_enabled"));
        scrollToViewAndClickIt(stringToID("account_settings_remote_search_num_label"));
        selectItemFromDialogListView(7, true);
        pressBack();
    }

    public void goToAccountSettingsPrivacyAndChangeSettings () {
        selectFromScreen(stringToID("privacy_preferences"));
        scrollToCheckBoxAndSetIt(true, stringToID("pep_enable_privacy_protection"));
        scrollToCheckBoxAndSetIt(false, stringToID("pep_mistrust_server_and_store_mails_encrypted"));
        scrollToViewAndClickIt(stringToID("advanced"));
        //scrollToCheckBoxAndCheckIt(false, stringToID("pep_sync_enable_account"));
        pressBack();
    }


    public void changeAccountSettings () {
        for (int account = 0; account < 3; account++) {
            selectAccountSettingsFromList(account);
            goToAccountSettingsGeneralAccountAndChangeSettings();
            goToAccountSettingsFetchingAccountAndChangeSettings();
            goToAccountSettingsSendingEmailAndChangeSettings();
            goToAccountSettingsDefaultFoldersAndChangeSettings();
            goToAccountSettingsNotificationsAndChangeSettings();
            goToAccountSettingsSearchAndChangeSettings();
            goToAccountSettingsPrivacyAndChangeSettings();
            pressBack();
        }
    }

    public void goToAccountSettingsGeneralAccountAndAssertSettings () {
        selectFromScreen(stringToID("account_settings_general_title"));
        scrollToViewAndClickIt(stringToID("account_settings_description_label"));
        assertTextInDialogWindow("newname");
        //scrollToCheckBoxAndAssertIt(false, stringToID("account_settings_default_label"));
        scrollToViewAndClickIt(stringToID("account_settings_show_pictures_label"));
        assertItemFromDialogListViewIsSelected(2, true);
        scrollToViewAndClickIt(stringToID("advanced"));
        scrollToCheckBoxAndAssertIt(false, stringToID("account_settings_mark_message_as_read_on_view_label"));
        pressBack();
    }

    public void assertTextInDialogWindow(String text) {
        BySelector selector = By.clazz("android.widget.EditText");
        boolean accountNameHasBeenChecked = false;
        boolean fail = false;
        waitForIdle();
        while (!accountNameHasBeenChecked) {
            for (UiObject2 editText : device.findObjects(selector)) {
                try {
                    if (editText.getResourceName().equals("android:id/edit")) {
                        if (!editText.getText().contains(text)) {
                            fail = true;
                        }
                        accountNameHasBeenChecked = true;
                        break;
                    }
                } catch (Exception ex) {
                    Timber.i("Cannot find account name: " + ex);
                }
            }
        }
        if (fail) {
            fail("Account name has not been modified");
        }
        pressOKButtonInDialog();
    }

    public void goToAccountSettingsFetchingAccountAndAssertSettings () {
        selectFromScreen(stringToID("account_settings_sync"));
        scrollToViewAndClickIt(stringToID("account_settings_incoming_label"));
        pressBack();
        scrollToViewAndClickIt(stringToID("advanced"));
        scrollToViewAndClickIt(stringToID("account_settings_mail_display_count_label"));
        assertItemFromDialogListViewIsSelected(5, true);
        scrollToViewAndClickIt(stringToID("account_settings_message_age_label"));
        assertItemFromDialogListViewIsSelected(5, true);
        scrollToViewAndClickIt(stringToID("account_settings_autodownload_message_size_label"));
        assertItemFromDialogListViewIsSelected(10, true);
        scrollToViewAndClickIt(stringToID("account_settings_mail_check_frequency_label"));
        assertItemFromDialogListViewIsSelected(5, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_sync_mode_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_push_mode_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToCheckBoxAndAssertIt(false, stringToID("account_settings_sync_remote_deletetions_label"));
        scrollToViewAndClickIt(stringToID("account_setup_incoming_delete_policy_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("account_setup_expunge_policy_label"));
        assertItemFromDialogListViewIsSelected(2, true);
        scrollToCheckBoxAndAssertIt(false, stringToID("push_poll_on_connect_label"));
        //scrollToViewAndClickIt(stringToID("account_setup_push_limit_label"));
        //assertItemFromDialogListViewIsSelected(6, true);
        scrollToViewAndClickIt(stringToID("idle_refresh_period_label"));
        assertItemFromDialogListViewIsSelected(3, true);
        pressBack();
    }

    public void goToAccountSettingsSendingEmailAndAssertSettings () {
        selectFromScreen(stringToID("account_settings_composition"));
        scrollToViewAndClickIt(stringToID("account_settings_message_format_label"));
        assertItemFromDialogListViewIsSelected(2, true);
        //scrollToCheckBoxAndAssertIt(true, stringToID("account_settings_always_show_cc_bcc_label"));
        scrollToCheckBoxAndAssertIt(false, stringToID("account_settings_default_quoted_text_shown_label"));
        scrollToViewAndClickIt(stringToID("advanced"));
        scrollToViewAndClickIt(stringToID("account_settings_quote_style_label"));
        selectItemFromDialogListView(0, true);
        scrollToCheckBoxAndAssertIt(true, stringToID("account_settings_reply_after_quote_label"));
        scrollToCheckBoxAndAssertIt(true, stringToID("account_settings_strip_signature_label"));
        scrollToViewAndClickIt(stringToID("account_settings_quote_prefix_label"));
        assertTextInDialogWindow("prefixtext");
        pressBack();
    }

    public void goToAccountSettingsDefaultFoldersAndAssertSettings () {
        selectFromScreen(stringToID("account_settings_folders"));
        scrollToViewAndClickIt(stringToID("account_setup_auto_expand_folder"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_display_mode_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("account_settings_folder_target_mode_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("archive_folder_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("drafts_folder_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("sent_folder_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("spam_folder_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        scrollToViewAndClickIt(stringToID("trash_folder_label"));
        assertItemFromDialogListViewIsSelected(0, true);
        pressBack();
    }

    public void goToAccountSettingsNotificationsAndAssertSettings () {
        selectFromScreen(stringToID("notifications_title"));
        scrollToCheckBoxAndAssertIt(true, stringToID("account_settings_notify_label"));
        scrollToCheckBoxAndAssertIt(true, stringToID("account_notify_contacts_mail_only_label"));
        //scrollToViewAndClickIt(stringToID("account_settings_notification_open_system_notifications_label"));
        scrollToViewAndClickIt(stringToID("advanced"));
        scrollToViewAndClickIt(stringToID("account_settings_folder_notify_new_mail_mode_label"));
        assertItemFromDialogListViewIsSelected(4, true);
        scrollToCheckBoxAndAssertIt(false, stringToID("account_settings_notify_self_label"));
        scrollToCheckBoxAndAssertIt(false, stringToID("account_settings_notify_sync_label"));
        scrollToCheckBoxAndAssertIt(true, stringToID("account_settings_notification_opens_unread_label"));
        pressBack();
    }

    public void goToAccountSettingsSearchAndAssertSettings () {
        selectFromScreen(stringToID("account_settings_search"));
        scrollToCheckBoxAndAssertIt(true, stringToID("account_settings_remote_search_enabled"));
        scrollToViewAndClickIt(stringToID("account_settings_remote_search_num_label"));
        assertItemFromDialogListViewIsSelected(7, true);
        pressBack();
    }

    public void goToAccountSettingsPrivacyAndAssertSettings () {
        selectFromScreen(stringToID("privacy_preferences"));
        scrollToCheckBoxAndAssertIt(true, stringToID("pep_enable_privacy_protection"));
        //scrollToCheckBoxAndAssertIt(false, stringToID("pep_mistrust_server_and_store_mails_encrypted"));
        scrollToViewAndClickIt(stringToID("advanced"));
        //scrollToCheckBoxAndAssertIt(false, stringToID("pep_sync_enable_account"));
        pressBack();
    }

    public void assertAccountSettings () {
        for (int account = 0; account < 3; account++) {
            selectAccountSettingsFromList(account);
            goToAccountSettingsGeneralAccountAndAssertSettings();
            goToAccountSettingsFetchingAccountAndAssertSettings();
            goToAccountSettingsSendingEmailAndAssertSettings();
            goToAccountSettingsDefaultFoldersAndAssertSettings();
            goToAccountSettingsNotificationsAndAssertSettings();
            goToAccountSettingsSearchAndAssertSettings();
            goToAccountSettingsPrivacyAndAssertSettings();
            pressBack();
        }
    }

    public void setTrustWords(String text) {
        trustWords = text;
    }

    public void checkDeviceIsSync(String deviceName, String firstDevice,
                                  String secondDevice, boolean syncThirdDevice) {
        if (firstDevice.equals(deviceName)) {
            checkSyncIsWorking_FirstDevice();
        } else if (secondDevice.equals(deviceName)) {
            checkSyncIsWorking_SecondDevice();
        } else if (syncThirdDevice) {
            waitForNewMessages(2);
        }
    }

    public void checkAccountIsNotProtected(String deviceName, String firstDevice,
                                  String secondDevice, boolean ThirdDevice) {
        if (firstDevice.equals(deviceName)) {
            checkIsNotProtected_FirstDevice();
        } else if (secondDevice.equals(deviceName)) {
            checkIsNotProtected_SecondDevice();
        } else if (ThirdDevice) {
            waitForNewMessages(2);
        }
    }

    public void checkDeviceIsNotSync(String deviceName, String firstDevice,
                                     String secondDevice, boolean syncThirdDevice) {
        if (firstDevice.equals(deviceName)) {
            checkSyncIsNotWorking_FirstDevice();
        } else if (secondDevice.equals(deviceName)) {
            checkSyncIsNotWorking_SecondDevice();
        } else if (syncThirdDevice) {
            waitForNewMessages(2);
        }
    }

    public String getKeySyncAccount (int account) {
        waitForIdle();
        readConfigFile();
        return testConfig.getKeySync_account(account);
    }

    public String getEmailAccount (int account) { return testConfig.getMail(account);}

    public static void getJSONObject(String object) {
        waitForIdle();
        switch (object) {
            case "keys":
                String keys = null;
                while (keys == null) {
                    try {
                        if (json == null) {
                            json = getJSON();
                        }
                        keys = json.getJSONObject("decryption_results").get(object).toString();
                    } catch (JSONException e) {
                        Timber.i("JSON file: " +e.getMessage());
                        e.printStackTrace();
                    }
                }
                if (!keys.contains("47220F5487391A9ADA8199FD8F8EB7716FA59050")) {
                    fail("Wrong key");
                }
                break;
            case "rating":
            case "rating_string":
                try {
                    if (json == null) {
                        json = getJSON();
                    }
                    rating = json.getJSONObject("decryption_results").get(object).toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "messageSubject":
            case "messageBody":
                try {
                    while (json == null) {
                        String js = readJsonFile("results.json");
                        if (js.equals("no json file")) {
                            return;
                        }
                        json = getJSON();
                    }
                    json = json.getJSONObject("attributes");
                    object = "decrypted";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            default:
                try {
                    if (json == null) {
                        json = getJSON();
                    }
                    json = json.getJSONObject(object);
                    Iterator x = json.keys();
                    jsonArray = new JSONArray();
                    while (x.hasNext()) {
                        jsonArray.put(json.get((String) x.next()));
                    }
                } catch (JSONException e) {
                    Timber.i("Cannot find json object");
                }
        }
        waitForIdle();
    }

    private static String readJsonFile(String fileName) {
        File directory = new File(Environment.getExternalStorageDirectory().toString() + "/Download/");
        swipeUpScreen();
        downloadAttachedFile(fileName);
        waitForIdle();
        File[] listOfFiles = directory.listFiles();
        File newFile = new File(directory, listOfFiles[0].getName());
        if(!newFile.exists())
        {
            return "no json file";
        }
        StringBuilder jsonText = new StringBuilder();
        try {
            FileInputStream fin = new FileInputStream(newFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fin);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            while ((receiveString = bufferedReader.readLine()) != null) {
                jsonText.append(receiveString);
            }
            fin.close();
        } catch (Exception e) {
            Timber.i("Error reading config file, trying again");
        }
        return jsonText.toString();
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

    public void dragWidget(int widgetToDrag, int x, int y) {
        switch (widgetToDrag) {
            case 1:
                device.drag(x, y, device.getDisplayWidth() / 6, device.getDisplayHeight() * 3 / 5, 30);
                waitForIdle();
                clickTextOnScreen("Unified Inbox");
                break;
            case 2:
                device.drag(x, y, device.getDisplayWidth() / 2, device.getDisplayHeight() / 3, 30);
                break;
            case 3:
                device.drag(x, y, device.getDisplayWidth() / 3, device.getDisplayHeight() * 3 / 5, 30);
                waitForIdle();
                clickTextOnScreen("Unified Inbox");
                break;
        }
    }

    public void verticalScreenScroll (boolean verticalLeftScroll, int start, int end) {
        waitForIdle();
        if (verticalLeftScroll) {
            device.drag(1, device.getDisplayHeight() * 3/4,
                    1, device.getDisplayHeight() * 1/2, 15);
        } else {
            device.swipe(device.getDisplayWidth() - 3, start,
                    device.getDisplayWidth() - 3, end, 15);
        }
        waitForIdle();
    }

    public String getAccountPassword () {
        while (testConfig.test_number.equals("-10")) {
            readConfigFile();
        }
        return testConfig.getPassword(Integer.parseInt(testConfig.test_number));
    }

    public String getAccountAddress (int account) {
        while (testConfig == null || testConfig.test_number.equals("-10")) {
            readConfigFile();
        }
        return testConfig.getMail(account);
    }

    public String getSyncAccount (int account) {
        while (testConfig == null || testConfig.test_number.equals("-10")) {
            readConfigFile();
        }
        return testConfig.getKeySync_account(account);
    }

    public String getFormatAccount () { return testConfig.format_test_account; }

    public String getPassphraseAccount() { return testConfig.getPassphrase_account(Integer.parseInt(testConfig.test_number) - 4);}

    public String getPassphrasePassword() { return testConfig.getPassphrase_password(Integer.parseInt(testConfig.test_number) - 4);}

    @NonNull
    private String getEmail() {
        return "test006@peptest.ch";
        //return BuildConfig.PLANCK_TEST_EMAIL_ADDRESS;
    }

    @NonNull
    private String getPassword() {
        return "";
        //return BuildConfig.PLANCK_TEST_EMAIL_PASSWORD;
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
        String[] mail;
        String[] password;
        String[] username;
        boolean[] trusted_server;
        String[] imap_server;
        String[] smtp_server;
        String[] imap_port;
        String[] smtp_port;
        int total = 3;
        String[] keySync_account;
        String[] keySync_password;
        String test_number;
        String[] passphrase_account;
        String[] passphrase_password;
        String format_test_account;
        String format_test_password;

        TestConfig(){
            this.mail = new String[total];
            this.password = new String[total];
            this.username = new String[total];
            this.trusted_server = new boolean[total];
            this.imap_server = new String[total];
            this.smtp_server = new String[total];
            this.imap_port = new String[total];
            this.smtp_port = new String[total];
            this.keySync_account = new String[2];
            this.keySync_password = new String[2];
            test_number = "-10";
            this.passphrase_account = new String[3];
            this.passphrase_password = new String[3];
            this.format_test_account = new String();
            this.format_test_password = new String();
        }

        void setMail(String mail, int account) { this.mail[account] = mail;}
        void setPassword(String password, int account) { this.password[account] = password;}
        void setUsername(String username, int account) { this.username[account] = username;}
        void setTrusted_server(boolean trusted_server, int account) { this.trusted_server[account] = trusted_server;}
        void setImap_server(String imap_server, int account) { this.imap_server[account] = imap_server;}
        void setSmtp_server(String smtp_server, int account) { this.smtp_server[account] = smtp_server;}
        void setImap_port(String imap_port, int account) { this.imap_port[account] = imap_port;}
        void setSmtp_port(String smtp_port, int account) { this.smtp_port[account] = smtp_port;}
        void setKeySync_account(String mail, int account) { this.keySync_account[account] = mail;}
        void setKeySync_password(String password, int account) { this.keySync_password[account] = password;}
        void settest_number(String number) { this.test_number = number;}
        void setPassphrase_account(String mail, int account) { this.passphrase_account[account] = mail;}
        void setPassphrase_password(String password, int account) { this.passphrase_password[account] = password;}
        void setFormat_test_account(String account) { this.format_test_account = account;}
        void setFormat_test_password(String password) { this.format_test_password = password;}

        String getMail(int account) { return mail[account];}
        String getPassword(int account) { return password[account];}
        String getUsername(int account) { return username[account];}
        boolean getTrusted_server(int account) { return trusted_server[account];}
        String getImap_server(int account) { return imap_server[account];}
        String getSmtp_server(int account) { return smtp_server[account];}
        String getImap_port(int account) { return imap_port[account];}
        String getSmtp_port(int account) { return smtp_port[account];}
        String getKeySync_account(int account) { return keySync_account[account];}
        String getKeySync_password(int account) { return keySync_password[account];}
        String gettest_number() { return test_number;}
        String getPassphrase_account(int account) { return passphrase_account[account];}
        String getPassphrase_password(int account) { return passphrase_password[account];}
        String getFormat_test_account() { return format_test_account;}
        String getFormat_test_password() { return  format_test_password;}
    }

    public String getString(int stringId) {
        return resources.getString(stringId);
    }

    public int getListSize(int listId) {
        GetListSizeAction listSize = new GetListSizeAction();
        device.waitForIdle();
        onView(withId(listId))
                .check(matches(isDisplayed()))
                .perform(listSize);
        return listSize.getSize();
    }

    public static class JDBCUtil {
        //JDBC and database properties.
        private static final String DB_DRIVER =
                "oracle.jdbc.driver.OracleDriver";
        private static final String DB_URL =
                "jdbc:oracle:thin:@localhost:1521:XE";
        private static final String DB_USERNAME = "system";
        private static final String DB_PASSWORD = "";

        public static Connection getConnection(){
            Connection conn = null;
            try{
                //Register the JDBC driver
                Class.forName(DB_DRIVER);

                //Open the connection
                conn = DriverManager.
                        getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

                if(conn != null){
                    System.out.println("Successfully connected.");
                }else{
                    System.out.println("Failed to connect.");
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return conn;
        }
    }
}
