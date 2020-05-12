package com.fsck.k9.pEp.ui.activities;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
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
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;

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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import foundation.pEp.jniadapter.Rating;
import timber.log.Timber;
import static android.content.ContentValues.TAG;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Checks.checkNotNull;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import androidx.test.platform.app.InstrumentationRegistry ;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.runner.lifecycle.Stage.RESUMED;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.appendTextInTextView;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.getTextFromView;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.hasValueEqualTo;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.saveSizeInInt;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.valuesAreEqual;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.viewIsDisplayed;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.viewWithTextIsDisplayed;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.waitUntilIdle;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withTextColor;
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
    private int[] messageListSize = new int[2];
    private int totalAccounts = -1;
    private int account = 0;

    public static final int TIMEOUT_TEST = FIVE_MINUTES * MINUTE_IN_SECONDS * SECOND_IN_MILIS;
    private TestConfig testConfig;
    public String[] botList;
    public boolean testReset = false;
    public static JSONObject json;
    public static JSONArray jsonArray;
    public static String rating;
    public String trustWords = "nothing";

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
        if (testConfig.getTrusted_server(account)) {
            device.waitForIdle();
            clickView(R.id.manual_setup);
            device.waitForIdle();
            while (true) {
                try {
                    while (exists(onView(withId(R.id.account_trust_server)))) {
                        setCheckBox("Trust server", true);
                        device.waitForIdle();
                        onView(withId(R.id.next)).perform(click());
                        device.waitForIdle();
                        return;
                    }
                    device.waitForIdle();
                    onView(withId(R.id.next)).perform(click());
                    device.waitForIdle();
                } catch (Exception eNext) {
                    Timber.i("Trust server not enabled yet");
                }
            }
        } else {
            clickNextButton();
        }
    }

    private void clickNextButton () {
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
        try {
            UiObject2 uiObject = device.findObject(By.res("security.pEp.debug:id/alertTitle"));
            while (uiObject.getText() != null) {
                pressBack();
                device.waitForIdle();
                onView(withId(R.id.next)).perform(click());
                device.waitForIdle();
                uiObject = device.findObject(By.res("security.pEp.debug:id/alertTitle"));
            }
        } catch (Exception ex) {
            Timber.i("Doesn't exist popup alert message");
        }
    }

    private void fillAccountAddress(String accountAddress) {
        while (getTextFromView(onView(withId(R.id.account_email))).equals("")) {
            try {
                device.waitForIdle();
                onView(withId(R.id.account_email)).perform(click());
                device.waitForIdle();
                onView(withId(R.id.account_email)).perform(typeText(accountAddress), closeSoftKeyboard());
            } catch (Exception ex) {
                Timber.i("Cannot fill account email: " + ex.getMessage());
            }
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
        while (!viewIsDisplayed(R.id.account_description)) {
            device.waitForIdle();
        }
        while (getTextFromView(onView(withId(R.id.account_description))).equals("")) {
            try {
                device.waitForIdle();
                onView(withId(R.id.account_description)).perform(click());
                device.waitForIdle();
                if (!description.equals("")) {
                    onView(withId(R.id.account_description)).perform(typeText(description), closeSoftKeyboard());
                } else {
                    onView(withId(R.id.account_description)).perform(typeText("TEST"), closeSoftKeyboard());
                }
                device.waitForIdle();
            } catch (Exception ex) {
                Timber.i("Cannot find account description field");
            }
        }
        while (getTextFromView(onView(withId(R.id.account_name))).equals("")) {
            try {
                device.waitForIdle();
                onView(withId(R.id.account_name)).perform(click());
                device.waitForIdle();
                if (!userName.equals("")) {
                    onView(withId(R.id.account_name)).perform(typeText(userName), closeSoftKeyboard());
                } else {
                    onView(withId(R.id.account_name)).perform(typeText("USER"), closeSoftKeyboard());
                }
                device.waitForIdle();
            } catch (Exception ex) {
                Timber.i("Cannot find account name field");
            }
        }
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
        while (!viewIsDisplayed(viewId)) {
            device.waitForIdle();
        }
        doWaitForResource(viewId);
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
        while (!buttonClicked) {
            if (exists(onView(withId(viewId))) || viewIsDisplayed(viewId)){
                device.waitForIdle();
                try {
                    onView(withId(viewId)).check(matches(isDisplayed()));
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

    public void createAccount() {
        createNewAccountWithPermissions();
        getMessageListSize();
    }

    public void readConfigFile() {
        File directory = new File(Environment.getExternalStorageDirectory().toString());
        File newFile = new File(directory, "test/test_config.txt");
        testConfig = new TestConfig();
        while (newFile.canRead() && (testConfig.getMail(0) == null || testConfig.getMail(0).equals(""))) {
            try {
                FileInputStream fin = new FileInputStream(newFile);
                InputStreamReader inputStreamReader = new InputStreamReader(fin);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                while ((receiveString = bufferedReader.readLine()) != null) {
                    String[] line = receiveString.split(" = ");
                    if (line.length > 1) {
                        switch (line[0]) {
                            case "mail":
                                testConfig.setMail(line[1], 0);
                                if (!testConfig.getMail(0).equals("")) {
                                    totalAccounts = 1;
                                }
                                break;
                            case "password":
                                testConfig.setPassword(line[1], 0);
                                break;case "username":
                                testConfig.setUsername(line[1], 0);
                                break;
                            case "trusted_server":
                                if (line[1].equals("true")) {
                                    testConfig.setTrusted_server(true, 0);
                                } else if (line[1].equals("false")){
                                    testConfig.setTrusted_server(false, 0);
                                } else {
                                    assertFailWithMessage("Trusted_server must be true or false");
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
                                    assertFailWithMessage("Password is empty");
                                }
                                break;case "username2":
                                testConfig.setUsername(line[1], 1);
                                break;
                            case "trusted_server2":
                                if (line[1].equals("true")) {
                                    testConfig.setTrusted_server(true, 1);
                                } else if (line[1].equals("false")){
                                    testConfig.setTrusted_server(false, 1);
                                } else {
                                    assertFailWithMessage("Trusted_server must be true or false");
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
                                    assertFailWithMessage("Password is empty");
                                }
                                break;case "username3":
                                testConfig.setUsername(line[1], 2);
                                break;
                            case "trusted_server3":
                                if (line[1].equals("true")) {
                                    testConfig.setTrusted_server(true, 2);
                                } else if (line[1].equals("false")){
                                    testConfig.setTrusted_server(false, 2);
                                } else {
                                    assertFailWithMessage("Trusted_server must be true or false");
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
                                if (!testConfig.getKeySync_account(0).equals("")) {
                                    totalAccounts = 1;
                                }
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
                            case "keysync_number":
                                testConfig.setKeySync_number(line[1]);
                                break;
                            default:
                                break;
                        }
                    }
                }
                fin.close();
            } catch (Exception e) {
                Timber.i("Error reading config file, trying again");
            }
        }
    }

    public void syncDevices () {
        while (!viewIsDisplayed(R.id.main_container) || !viewIsDisplayed(R.id.afirmativeActionButton)) {
            device.waitForIdle();
            Espresso.onIdle();
        }
        onView(withId(R.id.afirmativeActionButton)).perform(click());
        device.waitForIdle();
        Espresso.onIdle();
        trustWords = getTextFromView(onView(withId(R.id.trustwords)));
        onView(withId(R.id.afirmativeActionButton)).perform(click());
        while (!viewIsDisplayed(R.id.loading)) {
            device.waitForIdle();
            Espresso.onIdle();
        }
        while (viewIsDisplayed(R.id.loading)) {
            device.waitForIdle();
            Espresso.onIdle();
        }
        if (!viewIsDisplayed(R.id.afirmativeActionButton)) {
            assertFailWithMessage("Cannot sync devices");
        } else {
            onView(withId(R.id.afirmativeActionButton)).perform(click());
        }
    }

    public String keySync_number() { return testConfig.getKeySync_number();}

    public boolean keySyncAccountsExist () {
        return testConfig.getKeySync_password(0) != null
                && testConfig.getKeySync_password(0) != null
                && testConfig.getKeySync_account(1) != null
                && testConfig.getKeySync_password(1) != null;
    }

    public static void assertFailWithMessage(String message) {
        Assume.assumeTrue(message,false);
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
            botList = new String[9];
            int position = 0;
            while ( (receiveString = bufferedReader.readLine()) != null ) {
                botList[position++] = receiveString;
            }
            fin.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    private void createNewAccountWithPermissions(){
        testReset = false;
        boolean isKeySync = false;
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
            if (keySyncAccountsExist() && !keySync_number().equals("0")) {
                isKeySync = true;
            }
                while (exists(onView(withId(R.id.action_continue)))) {
                    try {
                        onView(withId(R.id.action_continue)).perform(click());
                        device.waitForIdle();
                    } catch (Exception ignoredException) {
                        Timber.i("Ignored", "Ignored exception");
                    }
                }
                Timber.i("Cuentas: " +getTotalAccounts());
                createNAccounts(getTotalAccounts(), isKeySync);
        } catch (Exception ex) {
            if (!exists(onView(withId(R.id.accounts_list)))) {
                readConfigFile();
                Timber.i("Ignored", "Exists account, failed creating new one");
            }
        }
    }

    public void clickHandShakeButton () {
        if (exists(onView(withId(R.id.buttonHandshake)))) {
            onView(withId(R.id.buttonHandshake)).perform(click());
            device.waitForIdle();
        }
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
    }

    public void goToHandshakeDialog (){
        device.waitForIdle();
        clickStatus();
        doWaitForResource(R.id.toolbar);
        clickHandShakeButton();
    }

    public void resetHandshake(){
        device.waitForIdle();
        try {
           onView(withId(R.id.recipientContainer)).perform(ViewActions.longClick());
            device.waitForIdle();
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ListView"));
            scroll.click();
        } catch (Exception ex) {
            Timber.e("Fail: " + ex.getMessage());
        }
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
    }

    public void createNAccounts (int n, boolean isKeySync) {
        try {
            for (; account < n; account++) {
                device.waitForIdle();
                while(exists(onView(withId(R.id.message_list)))) {
                    openOptionsMenu();
                    selectFromMenu(R.string.action_settings);
                    device.waitForIdle();
                }
                addAccount();
                if (isKeySync) {
                    fillAccountAddress(testConfig.getKeySync_account(Integer.parseInt(testConfig.keySync_number) - 1));
                    fillAccountPassword(testConfig.getKeySync_password(Integer.parseInt(testConfig.keySync_number) - 1));
                } else {
                    fillAccountAddress(testConfig.getMail(account));
                    fillAccountPassword(testConfig.getPassword(account));
                }
                if (!(testConfig.getImap_server(account) == null) && !(testConfig.getSmtp_server(account) == null)) {
                    manualAccount();
                } else {
                    automaticAccount();
                }
                try {
                    device.waitForIdle();
                    accountDescription(testConfig.getUsername(account), testConfig.getUsername(account));
                } catch (Exception e) {
                    Timber.i("Can not fill account description");
                }
            }
        } catch (Exception ex) {
            Timber.i("Ignored", "Exists account");
        }
    }

    public void addAccount () {
        try {
            swipeUpScreen();
            onView(withId(R.id.add_account_container)).perform(click());
            device.waitForIdle();
        } catch (Exception list) {
            Timber.i("Cannot add a new account");
        }
    }

    public void selectAccount (int accountToSelect) {
        while (true) {
            try {
                device.waitForIdle();
                onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                device.waitForIdle();
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    selectAccountFromList(accountToSelect);
                    getMessageListSize();
                    return;
                } else if (exists(onView(withId(R.id.accounts_list)))) {
                    selectAccountFromList(accountToSelect);
                } else if (exists(onView(withId(android.R.id.list)))) {
                    clickInbox();
                    return;
                } else if (!exists(onView(withId(R.id.available_accounts_title)))){
                    selectFromMenu(R.string.prefs_title);
                    selectAccountFromList(accountToSelect);
                    getMessageListSize();
                    return;
                } else {
                    device.waitForIdle();
                    onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                    device.waitForIdle();
                }
            } catch (Exception ex) {
                Timber.i("Cannot click account " +accountToSelect +": " + ex.getMessage());
                while (!exists(onView(withId(R.id.accounts_list)))) {
                    pressBack();
                    device.waitForIdle();
                }
            }
        }
    }

    public static void swipeDownScreen () {
        try {
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
            device.waitForIdle();
            scroll.swipe(Direction.DOWN, 1.0f);
            device.waitForIdle();
        } catch (Exception swipe) {
            Timber.i("Cannot do swipeDown");
        }
    }

    public static void swipeUpScreen () {
        try {
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
            device.waitForIdle();
            scroll.swipe(Direction.UP, 1.0f);
            device.waitForIdle();
        } catch (Exception swipe) {
            Timber.i("Cannot do swipeUp");
        }
    }

    private void selectAccountFromHamburgerMenu (int accountToSelect) {
        /*device.waitForIdle();
        openHamburgerMenu();
        clickView(R.id.nav_header_accounts);
        device.waitForIdle();
        onView(withId(R.id.navigation_accounts)).perform(RecyclerViewActions.actionOnItemAtPosition(accountToSelect, click()));
    */}

    private void selectAccountFromList (int accountToSelect) {
        while (!viewIsDisplayed(R.id.accounts_list)) {
            swipeUpScreen();
        }
        onView(withId(R.id.accounts_list)).check(matches(isCompletelyDisplayed()));
        goToTheInbox(accountToSelect);
        if (exists(onView(withId(R.id.message_list)))) {
            getMessageListSize();
        }
    }

    private void goToTheInbox (int accountToSelect){
        while (true) {
            device.waitForIdle();
            try {
                UiObject2 wb;
                wb = device.findObject(By.clazz("android.widget.ListView"));
                device.waitForIdle();
                wb.getChildren().get(accountToSelect).getChildren().get(1).click();
                clickInbox();
                return;
            } catch (Exception e) {
                Timber.i("Cannot click account from list: " + e.getMessage());
            }
            device.waitForIdle();
        }
    }

    private void clickInbox () {
        waitForToolbar();
        device.waitForIdle();
        while (true) {
            try {
                selectFromScreen(R.string.special_mailbox_name_inbox);
                device.waitForIdle();
                waitForToolbar();
                return;
            } catch (Exception noInbox) {
                Timber.i("No inbox to click: " + noInbox.getMessage());
            }
        }
    }

    public void clickSearch() {
        device.waitForIdle();
        onView(withId(R.id.search)).perform(click());
        device.waitForIdle();
    }

    private void fillAccountPassword(String accountPassword) {
        while (exists(onView(withId(R.id.account_password))) && getTextFromView(onView(withId(R.id.account_password))).equals("")) {
            try {
                device.waitForIdle();
                onView(withId(R.id.account_password)).perform(typeText(accountPassword), closeSoftKeyboard());
                device.waitForIdle();
                if (viewWithTextIsDisplayed(resources.getString(R.string.account_already_exists))) {
                    pressBack();
                    return;
                }
            } catch (Exception ex) {
                Timber.i("Cannot fill account password: " + ex.getMessage());
            }
        }
    }

    private void manualAccount() {
        while (!viewIsDisplayed(R.id.manual_setup)) {
            device.waitForIdle();
        }
        while (exists(onView(withId(R.id.manual_setup)))) {
            device.waitForIdle();
            onView(withId(R.id.manual_setup)).perform(click());
        }
        setupImapServer();
        setupSMTPServer();
        device.waitForIdle();
        /*while (exists(onView(withId(R.id.account_server)))) {
            device.waitForIdle();
        }*/
        while (!exists(onView(withId(R.id.account_description)))) {
            try {
                device.waitForIdle();
                onView(withId(R.id.next)).perform(click());
                device.waitForIdle();
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
        device.waitForIdle();
        onView(withId(R.id.account_server_label)).check(matches(isCompletelyDisplayed()));
        while (!viewIsDisplayed(R.id.account_server_label) &&
                !getTextFromView(onView(withId(R.id.account_server_label))).equals(server)) {
            device.waitForIdle();
        }
        while (!getTextFromView(onView(withId(R.id.account_server))).equals(accountServer) &&
            exists(onView(withId(R.id.account_server_label)))) {
            try {
                device.waitForIdle();
                while (!getTextFromView(onView(withId(R.id.account_server))).equals("")){
                    removeTextFromTextView("account_server");
                }
                device.waitForIdle();
                while (!getTextFromView(onView(withId(R.id.account_server))).equals(accountServer)) {
                    onView(withId(R.id.account_server)).perform(typeText(accountServer), closeSoftKeyboard());
                    device.waitForIdle();
                    onView(withId(R.id.account_server)).check(matches(isDisplayed()));
                    device.waitForIdle();
                }
                setupPort(testConfig.getImap_port(account));
                device.waitForIdle();
                while (exists(onView(withId(R.id.account_server_label)))) {
                    try {
                        onView(withId(R.id.next)).perform(click());
                        while (exists(onView(withId(R.id.account_server_label)))) {
                            device.waitForIdle();
                        }
                        waitUntilIdle();
                        if (exists(onView(withId(R.id.alertTitle)))) {
                            pressBack();
                            device.waitForIdle();
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
            device.waitForIdle();
            removeTextFromTextView("account_port");
            device.waitForIdle();
            onView(withId(R.id.account_port)).perform(click());
            onView(withId(R.id.account_port)).perform(typeText(port), closeSoftKeyboard());
            device.waitForIdle();
            onView(withId(R.id.account_port)).check(matches(isDisplayed()));
            device.waitForIdle();
        }
    }

    private void setupAccountSMTPServer(String accountServer, String server) {
        device.waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.account_server)).check(matches(isDisplayed()));
        while (!exists(onView(withId(R.id.account_server)))) {
            device.waitForIdle();
        }
        while (!getTextFromView(onView(withId(R.id.account_server))).equals("")){
            removeTextFromTextView("account_server");
        }
        onView(withId(R.id.account_server)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
        while (exists(onView(withId(R.id.account_server))) && !getTextFromView(onView(withId(R.id.account_server))).equals(accountServer)){
            try {
                device.waitForIdle();
                device.waitForIdle();
                while (!getTextFromView(onView(withId(R.id.account_server))).equals(accountServer)) {
                    onView(withId(R.id.account_server)).perform(typeText(accountServer), closeSoftKeyboard());
                    device.waitForIdle();
                }
                setupPort(testConfig.getSmtp_port(account));
                device.waitForIdle();
                while (viewIsDisplayed(R.id.account_server)) {
                    device.waitForIdle();
                    if (exists(onView(withId(R.id.alertTitle)))) {
                        pressBack();
                        device.waitForIdle();
                    }else if (!exists(onView(withId(R.id.account_server)))) {
                        return;
                    } else {
                        onView(withId(R.id.next)).perform(click());
                        while (viewIsDisplayed(R.id.account_server)) {
                            device.waitForIdle();
                        }
                    }
                }
            } catch (Exception e) {
                Timber.i("Cannot setup server: " + e.getMessage());
            }
        }
    }

    private void allowPermissions(){
        device.waitForIdle();
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
        }while (!viewIsDisplayed((R.id.action_continue)) && !viewIsDisplayed((R.id.account_email)));
    }

    private void allowPermissions(int index) {
        while (true){
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
                    return;
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
        device.waitForIdle();
        while (!viewIsDisplayed(R.id.to)) {
            device.waitForIdle();
        }
        UiObject2 list = null;
        Rect bounds = null;
        while (list == null || bounds == null) {
            try {
                device.waitForIdle();
                list = device.findObject(By.res(APP_ID, "to"));
                bounds = list.getVisibleBounds();
            } catch (Exception ex) {
                Timber.i("Cannot find view TO");
            }
        }
        if (!inputMessage.getTo().equals("")) {
            onView(withId(R.id.to)).perform(click(), closeSoftKeyboard());
            device.click(bounds.left - 1, bounds.centerY());
            device.waitForIdle();
            device.click(bounds.left - 1, bounds.centerY());
            device.waitForIdle();
            onView(withId(R.id.to)).perform(appendTextInTextView(inputMessage.getTo()), closeSoftKeyboard());

        }
        while (!getTextFromView(onView(withId(R.id.subject))).contains(inputMessage.getSubject())
                || !getTextFromView(onView(withId(R.id.message_content))).contains(inputMessage.getMessage())) {
            try {
                device.waitForIdle();
                device.waitForIdle();
                device.findObject(By.res(APP_ID, "subject")).click();
                device.waitForIdle();
                device.findObject(By.res(APP_ID, "message_content")).click();
                device.waitForIdle();
                onView(withId(R.id.subject)).perform(typeText(inputMessage.getSubject()), closeSoftKeyboard());
                device.waitForIdle();
                onView(withId(R.id.message_content)).perform(typeText(inputMessage.getMessage()), closeSoftKeyboard());
                device.waitForIdle();
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

    public void attachFile(String fileName) {
        do {
            device.waitForIdle();
            onView(withId(R.id.add_attachment)).perform(click());
            device.waitForIdle();
        } while (!textExistsOnScreenTextView(fileName));
        waitUntilIdle();
        onView(withId(R.id.attachments)).check(matches(hasDescendant(withText(fileName))));
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

    public StringBuilder readFile (String folder, String fileName) {
        StringBuilder text = new StringBuilder();
        File directory = new File(Environment.getExternalStorageDirectory().toString() + folder);
        File[] files = directory.listFiles();
        for (File fileOpen : files) {
            if (fileOpen.getName().equals(fileName)) {
                File file = new File(Environment.getExternalStorageDirectory().toString() + folder + fileOpen.getName());
                device.waitForIdle();
                try {
                    FileInputStream fin = new FileInputStream(fileOpen);
                    InputStreamReader inputStreamReader = new InputStreamReader(fin);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString;
                    while ((receiveString = bufferedReader.readLine()) != null) {
                        text.append(receiveString);
                    }
                    fin.close();
                } catch (Exception e) {
                    Timber.i("Error reading " + fileName + ", trying again");
                } finally {
                    file.delete();
                }
            }
        }
        return text;
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
        Espresso.onIdle();
        device.waitForIdle();
        waitUntilIdle();
        if (exists(onView(withId(R.id.toolbar)))) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
        onView(isRoot()).perform(ViewActions.pressBack());
        Espresso.onIdle();
        device.waitForIdle();
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
                pressBack();
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
        int position;
        swipeUpScreen();
        for (int start = 0; start < total; start++) {
            int size = device.findObjects(selector).size();
            while (size == 0) {
                size = device.findObjects(selector).size();
            }
            UiObject2 uiObject = device.findObject(By.res("security.pEp.debug:id/attachment"));
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

    private void longClick(String viewId) {
        UiObject2 list = device.findObject(By.res(APP_ID, viewId));
        Rect bounds = list.getVisibleBounds();
        device.swipe(bounds.centerX(), bounds.centerY(), bounds.centerX(), bounds.centerY(), 450);
    }

    public void removeTextFromTextView(String viewId) {
        device.waitForIdle();
        int view = intToID(viewId);
        while (!exists(onView(withId(view)))) {
            device.waitForIdle();
        }
        onView(withId(view)).perform(closeSoftKeyboard());
        onView(withId(view)).perform(click());
        clickTextView(viewId);
        while (!(hasValueEqualTo(onView(withId(view)), " ")
                || hasValueEqualTo(onView(withId(view)), ""))) {
            try {
                device.waitForIdle();
                device.waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                device.waitForIdle();device.pressKeyCode(KeyEvent.KEYCODE_DEL);
                device.waitForIdle();
                onView(withId(view)).perform(click());
            } catch (Exception ex) {
                pressBack();
                Timber.i("Cannot remove text from field " + viewId + ": " + ex.getMessage());
            }
        }
    }

    private void clickTextView (String viewId) {
        while (true) {
            try {
                UiObject2 list = device.findObject(By.res(APP_ID, viewId));
                Rect bounds = list.getVisibleBounds();
                device.click(bounds.left - 1, bounds.centerY());
                return;
            } catch (Exception ex) {
                device.waitForIdle();
                Timber.i("Cannot click " + viewId + ": " + ex.getMessage());
            }
        }
    }

    void testStatusEmpty() {
        checkStatus(Rating.pEpRatingUndefined);
        pressBack();
    }

    void testStatusMail(BasicMessage inputMessage, BasicIdentity expectedIdentity) {
        fillMessage(inputMessage, false);
        typeTextToForceRatingCaltulation(R.id.subject);
        checkStatus(expectedIdentity.getRating());
        pressBack();
    }

    void testStatusMailAndListMail(BasicMessage inputMessage, BasicIdentity expectedIdentity) {
        fillMessage(inputMessage, false);
        typeTextToForceRatingCaltulation(R.id.subject);
        checkStatus(expectedIdentity.getRating());
        onView(withText(expectedIdentity.getAddress())).check(doesNotExist());
        pressBack();
    }

    void checkStatus(Rating rating) {
        assertMessageStatus(rating);
    }

    public void assertMessageStatus(Rating status){
        int statusColor;
        clickStatus();
        while (!viewIsDisplayed(R.id.toolbar)) {
            device.waitForIdle();
        }
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        /*while (!viewIsDisplayed(R.id.pEpTitle)) {
            device.waitForIdle();
        }*/
        waitForToolbar();
        statusColor = getSecurityStatusIconColor(status);
        if (statusColor == -10) {
            if (viewIsDisplayed(R.id.actionbar_message_view)) {
                assertFailWithMessage("Wrong Status, it should be empty");
            }
        } else {
            if (R.id.securityStatusIcon != statusColor) {
                assertFailWithMessage("Wrong Status color");
            }
            onView(withId(R.id.securityStatusText)).check(matches(withText(getResourceString(R.array.pep_title, status.value))));
        }
        if (!exists(onView(withId(R.id.send)))) {
            goBack(false);
        }
    }

    private int getSecurityStatusIconColor (Rating rating){
        int color;
        if (rating == null) {
            color = -10;
        } else if (rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value) {
            color = -10;
        } else if (rating.value == Rating.pEpRatingMistrust.value) {
            color = R.drawable.pep_status_red;
        } else if (rating.value >= Rating.pEpRatingTrusted.value) {
            color = R.drawable.pep_status_green;
        } else if (rating.value == Rating.pEpRatingReliable.value) {
            color = R.drawable.pep_status_yellow;
        } else {
            color = -10;
        }
        return color;
    }

    public void clickStatus() {
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        if (viewIsDisplayed(R.id.securityStatusText)) {
            device.waitForIdle();
            onView(withId(R.id.securityStatusText)).check(matches(isDisplayed()));
            onView(withId(R.id.securityStatusText)).perform(click());
        }
        device.waitForIdle();
    }

    public void goBackAndSaveAsDraft (IntentsTestRule activity){
        goBack(true);
    }

    private void goBack (boolean saveAsDraft) {
        Activity currentActivity = getCurrentActivity();
        while (currentActivity == getCurrentActivity()){
            try {
                if (saveAsDraft) {
                    device.waitForIdle();
                    while (!viewIsDisplayed(R.id.message_content)) {
                        onView(withId(R.id.message_content)).perform(closeSoftKeyboard());
                        device.waitForIdle();
                    }
                }
                device.waitForIdle();
                pressBack();
                device.waitForIdle();
                if (saveAsDraft) {
                    onView(withText(R.string.save_draft_action)).perform(click());
                }
            } catch (Exception ex){
                Timber.i("Ignored exception: " + ex);
            }
        }
        device.waitForIdle();}

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
        pressBack();
        onView(withId(R.id.toolbar)).check(matches(valuesAreEqual(textOnScreen, resources.getString(comparedWith))));
    }

    public void assertsTextExistsOnScreen (String textToCompare) {
        BySelector selector = By.clazz("android.widget.TextView");
        boolean exists = false;
        device.waitForIdle();
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains(textToCompare)) {
                    exists = true;
                    device.waitForIdle();
                    break;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
        if (!exists) {
            assertFailWithMessage("Cannot find " + textToCompare + " on the screen");
        }
    }

    public void summonThreads () {
        clickStatus();
        pressBack();
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
        device.waitForIdle();
        while (!viewIsDisplayed(R.id.toolbar) || !viewIsDisplayed(R.id.toolbar_container)) {
            device.waitForIdle();
            waitUntilIdle();
            device.waitForIdle();
        }
        while (true) {
            waitUntilIdle();
            device.waitForIdle();
            if (exists(onView(withId(R.id.toolbar))) && viewIsDisplayed(R.id.toolbar) && viewIsDisplayed(R.id.toolbar_container)) {
                waitForToolbar();
                onView(withId(R.id.securityStatusText)).check(matches(withTextColor(color)));
                //checkUpperToolbar(color);
                return;
            }
        }
    }

    public void openHamburgerMenu () {
        device.waitForIdle();
        onView(withContentDescription("Open navigation drawer")).perform(click());
        device.waitForIdle();
    }

    public void typeTextToForceRatingCaltulation (int view) {
        device.waitForIdle();
        onView(withId(view)).perform(click(), closeSoftKeyboard());
        onView(withId(view)).perform(typeText(" "), closeSoftKeyboard());
        device.waitForIdle();
        if (getTextFromView(onView(withId(view))).contains(" ")) {
            device.pressKeyCode(KeyEvent.KEYCODE_DEL);
        }
        device.waitForIdle();
    }

    public static void waitForToolbar() {
        for (int waitLoop = 0; waitLoop < 1000; waitLoop++) {
            device.waitForIdle();
            Espresso.onIdle();
            while (!viewIsDisplayed(R.id.toolbar)) {
                device.waitForIdle();
            }
            device.waitForIdle();
            waitUntilIdle();
            onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
            device.waitForIdle();
            waitUntilIdle();
            Espresso.onIdle();
        }}

    private void checkUpperToolbar (int color){
        int colorFromResource = (ContextCompat.getColor(InstrumentationRegistry.getInstrumentation().getTargetContext(), color) & 0x00FFFFFF);
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(colorFromResource), Color.green(colorFromResource), Color.blue(colorFromResource), hsv);
        hsv[2] = hsv[2]*0.9f;
        color = Color.HSVToColor(hsv);
        int upperToolbarColor = getCurrentActivity().getWindow().getStatusBarColor();
        org.junit.Assert.assertEquals("Text", upperToolbarColor, color);
        if (upperToolbarColor != color) {
            assertFailWithMessage("Upper toolbar color is wrong");
        }
    }

    public void selectFromMenu(int viewId){
        device.waitForIdle();
        while (true) {
            try {
                openOptionsMenu();
                selectFromScreen(viewId);
                device.waitForIdle();
                Espresso.onIdle();
                return;
            } catch (Exception ex) {
                Timber.i("Toolbar is not closed yet");
            }
        }
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

    private boolean textExistsOnScreenTextView(String text) {
        boolean viewExists = false;
        device.waitForIdle();
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
            device.waitForIdle();
        }
    }

    private void goBackToOriginalApp() {
        while (!APP_ID.equals(device.getCurrentPackageName())) {
            pressBack();
        }
    }

    public void openOptionsMenu() {
        while (true) {
            try {
                device.waitForIdle();
                onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
                device.waitForIdle();
                openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
                device.waitForIdle();
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
                            device.waitForIdle();
                            Espresso.onIdle();
                            object.longClick();
                            device.waitForIdle();
                            Espresso.onIdle();
                            return;
                        } catch (Exception ex1) {
                            device.waitForIdle();
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
        BySelector selector = By.clazz("android.widget.TextView");
        while (true) {
            for (UiObject2 object : device.findObjects(selector)) {
                try {
                    if (object.getText().equals(resources.getString(resource))) {
                        try {
                            while (object.getText().equals(resources.getString(resource))) {
                                device.waitForIdle();
                                Espresso.onIdle();
                                object.longClick();
                                device.waitForIdle();
                                Espresso.onIdle();
                            }
                            device.waitForIdle();
                            Espresso.onIdle();
                            return;
                        } catch (Exception ex1) {
                            device.waitForIdle();
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

    void doWait(String viewId) {
        UiObject2 waitForView = device
                .wait(Until.findObject(By.res(APP_ID, viewId)),
                        150000);
        assertThat(waitForView, notNullValue());
    }

    public void doWaitForResource(int resource) {
        device.waitForIdle();
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

    public void clickMessageStatus() {
        clickView(R.id.securityStatusText);
    }

    public void goBackToMessageList(){
        boolean backToMessageCompose = false;
        if (viewIsDisplayed(R.id.fab_button_compose_message)){
            backToMessageCompose = true;
        }
        while (!backToMessageCompose){
            pressBack();
            device.waitForIdle();
            waitForToolbar();
            if (viewIsDisplayed(R.id.fab_button_compose_message)){
                backToMessageCompose = true;
            }
        }
    }

    public void goToFolder(String folder) {
        int hashCode = 0;
        BySelector textViewSelector;
        textViewSelector = By.clazz("android.widget.TextView");
        selectFromMenu(R.string.folders_title);
        device.waitForIdle();
        while (true) {
            for (UiObject2 textView : device.findObjects(textViewSelector)) {
                try {
                    if (textView.findObject(textViewSelector).getText() != null && textView.findObject(textViewSelector).getText().contains(folder)) {
                        textView.findObject(textViewSelector).longClick();
                        device.waitForIdle();
                        waitForToolbar();
                        if (hashCode == 0) {
                            hashCode = textView.findObject(textViewSelector).hashCode();
                        } else {
                            return;
                        }
                    }
                    device.waitForIdle();
                } catch (Exception e) {
                    Timber.i("View is not sent folder");
                    try {
                        if (getTextFromView(onView(withId(R.id.actionbar_title_first))).contains(folder)) {
                            return;
                        }
                    } catch (Exception noTitle) {
                        Timber.i("Title bar doesn't exist");
                    }
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

    public void waitForMessageAndClickIt() {
        Timber.i("MessageList antes: " + messageListSize[0] + " " + messageListSize[1]);
        waitForNewMessage();
        Timber.i("MessageList despues: " + messageListSize[0] + " " + messageListSize[1]);
        clickLastMessage();
    }

    public String longText() {
        return "Lorem ipsum dolor sit amet consectetur adipiscing elit lectus neque, nulla eros ullamcorper phasellus egestas sagittis ridiculus cursus montes, morbi taciti hendrerit sed metus nam nascetur velit. Placerat nascetur congue risus mollis felis in nisl, fames arcu nunc nostra ultricies taciti, massa conubia rutrum commodo augue vivamus. Quisque aliquam sem nostra purus inceptos velit cubilia arcu, netus aliquet sodales at a ad consequat magna odio, dui duis suscipit orci nulla tellus massa." +
                "Mus urna dis enim curabitur erat nisi aenean imperdiet porttitor nulla ad velit, rutrum senectus congue morbi nisl duis pretium augue volutpat et ac vulputate auctor, sodales mi sociosqu facilisis convallis habitant tempor tortor massa at lectus. Sed aliquet sapien sollicitudin fusce cubilia felis consequat malesuada justo lacinia tincidunt viverra, magnis arcu commodo maecenas cum purus potenti massa himenaeos odio. Natoque sodales mauris proin gravida malesuada, faucibus lacinia neque pellentesque, habitant nisl porta velit.";
    }

    public void clickLastMessage() {
        boolean messageClicked = false;
        while (!messageClicked) {
            device.waitForIdle();
            if (!viewIsDisplayed(R.id.openCloseButton)) {
                try {
                    swipeDownMessageList();
                    device.waitForIdle();
                    while (viewIsDisplayed(R.id.message_list)) {
                        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
                        messageClicked = true;
                        device.waitForIdle();
                        waitUntilIdle();
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
                device.waitForIdle();
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
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        device.waitForIdle();
        try {
            onView(withId(R.id.message_list)).perform(swipeUp());
        } catch (Exception noSwipe) {
            Timber.i("Cannot SwipeUp");
        }
        device.waitForIdle();
    }

    public void clickMessageAtPosition(int position) {
        boolean messageClicked = false;
        while (!messageClicked) {
            device.waitForIdle();
            if (!viewIsDisplayed(R.id.openCloseButton)) {
                try {
                    swipeDownMessageList();
                    device.waitForIdle();
                    while (viewIsDisplayed(R.id.message_list) || messageClicked) {
                        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(position - 1).perform(click());
                        messageClicked = true;
                        device.waitForIdle();
                        waitUntilIdle();
                    }
                    if (viewIsDisplayed(R.id.fab_button_compose_message)) {
                        try {
                            messageClicked = false;
                            while (exists(onView(withId(R.id.delete)))) {
                                device.waitForIdle();
                                pressBack();
                                device.waitForIdle();
                            }
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
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        device.waitForIdle();
    }

    public void emptyFolder (String folderName) {
        device.waitForIdle();
        File dir = new File(Environment.getExternalStorageDirectory()+"/" + folderName + "/");
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
        device.waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        for (int i=0; i<5; i++){
            device.waitForIdle();
            waitUntilIdle();
            onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
            swipeUpScreen();
        }
        json = null;
        device.waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
        waitUntilIdle();
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains("results.json")) {
                    device.waitForIdle();
                    while (json == null) {
                        try {
                            downloadAttachedFile("results.json");
                            device.waitForIdle();
                            String js = readJsonFile("results.json");
                            json = new JSONObject(js);
                        } catch (Exception ex) {
                            swipeUpScreen();
                            boolean jsonExists = false;
                                try {
                                    device.waitForIdle();
                                    if (object.getText().contains("results.json")) {
                                        jsonExists = true;
                                    }
                                } catch (Exception json) {
                                    Timber.i("Cannot find json file on the screen: " + json);
                                }
                            if (!jsonExists) {
                                device.waitForIdle();
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

    private static void downloadAttachedFile(String fileName) {
        BySelector selector = By.clazz("android.widget.TextView");
        for (UiObject2 object : device.findObjects(selector)) {
            try {
                if (object.getText().contains(fileName)) {
                    device.waitForIdle();
                    onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
                    device.waitForIdle();
                    object.getParent().getChildren().get(0).click();
                    device.waitForIdle();
                    waitForToolbar();
                    onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
                    return;
                }
            } catch (Exception ex){
                Timber.i("Cannot find text on screen: " + ex);
            }
        }
    }

    public JSONObject returnJSON (){
        return json;
    }

    public void waitForNewMessage() {
        boolean newEmail = false;
        device.waitForIdle();
        while (!exists(onView(withId(R.id.message_list)))){
            device.waitForIdle();
        }
        doWaitForResource(R.id.message_list);
        doWaitForIdlingListViewResource(R.id.message_list);
        onView(withId(R.id.message_list)).check(matches(isDisplayed()));
        while (!newEmail) {
            try {
                device.waitForIdle();
                swipeDownMessageList();
                device.waitForIdle();
                onView(withId(R.id.message_list)).check(matches(isDisplayed()));
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
        if (viewIsDisplayed(R.id.delete)) {
            pressBack();
            device.waitForIdle();
        }
    }

    public void getMessageListSize() {
        device.waitForIdle();
        swipeDownMessageList();
        device.waitForIdle();
        while (exists(onView(withId(R.id.message_list)))) {
            try {
                device.waitForIdle();
                onView(withId(R.id.message_list)).check(matches(isDisplayed()));
                onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 0));
                return;
            } catch (Exception ex) {
                Timber.i("Cannot find view message_list: " + ex.getMessage());
            }
        }
    }

    public void swipeDownMessageList() {
        while (true) {
            try {
                device.waitForIdle();
                onView(withId(R.id.message_list)).perform(swipeDown());
                device.waitForIdle();
                onView(withId(R.id.message_list)).perform(swipeDown());
                device.waitForIdle();
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
        while (!viewIsDisplayed(R.id.message_list)) {
            device.waitForIdle();
        }
        while ((exists(onView(withId(R.id.message_list))) || viewIsDisplayed(R.id.message_list))
         && (!viewIsDisplayed(R.id.openCloseButton))){
            try{
                    device.waitForIdle();
                    swipeDownMessageList();
                    device.waitForIdle();
                    getMessageListSize();
                    if (viewIsDisplayed(R.id.openCloseButton)) {
                        return;
                    }
                    else {
                        device.waitForIdle();
                        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
                        device.waitForIdle();
                    }
            } catch (Exception ex){
                Timber.i("Cannot find list: " + ex);
            }
        }
        device.waitForIdle();
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
                pressBack();
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

    public void setClipboard(String textToCopy) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(null, textToCopy);
        while (clipboard.getPrimaryClip() == null || clipboard.getPrimaryClip().toString().equals("")) {
            device.waitForIdle();
            clipboard.setPrimaryClip(clip);
            device.waitForIdle();
        }
    }

    public void pasteClipboard() {
        device.waitForIdle();
        UiDevice.getInstance(getInstrumentation())
                .pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_MASK);
        device.waitForIdle();
    }

    public void compareMessageBody(String cucumberBody) {
        String [] body;
        swipeUpScreen();
        waitUntilIdle();
        while (!viewIsDisplayed(R.id.message_content) || !viewIsDisplayed(R.id.message_container)) {
            device.waitForIdle();
        }
        doWaitForResource(R.id.message_container);
        while (true) {
            device.waitForIdle();
            if (exists(onView(withId(R.id.message_container)))) {
                onView(withId(R.id.message_container)).check(matches(isDisplayed()));
                if (cucumberBody.equals("Rating/DecodedRating")) {
                    body = new String[2];
                    body[0] = "Rating | 6";
                    body[1] = "Decoded Rating | PEP_rating_reliable";
                } else {
                    body = new String[1];
                    body[0] = cucumberBody;
                }
                compareTextWithWebViewText(body[0]);
                return;
            } else if (exists(onView(withId(R.id.message_content)))) {
                onView(withId(R.id.message_content)).check(matches(isDisplayed()));
                String[] text = getTextFromView(onView(withId(R.id.message_content))).split("--");
                if (text[0].equals(cucumberBody)) {
                    return;
                } else {
                    device.waitForIdle();
                    onView(withId(R.id.toolbar_container)).check(matches(isDisplayed()));
                    assertFailWithMessage("Error: BODY TEXT=" + text[0] + " ---*****--- TEXT TO COMPARE=" + cucumberBody);
                }
            }
        }
    }

    public void compareMessageBodyLongText(String cucumberBody) {
        onView(withId(R.id.toolbar_container)).check(matches(isDisplayed()));
        swipeUpScreen();
        waitUntilIdle();
        BySelector selector = By.clazz("android.widget.EditText");
        UiObject2 uiObject = device.findObject(By.res("security.pEp.debug:id/message_content"));
        for (UiObject2 object : device.findObjects(selector)) {
            if (object.getResourceName().equals(uiObject.getResourceName())) {
                device.waitForIdle();
                onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
                device.waitForIdle();
                if (!object.getText().contains(cucumberBody)) {
                    assertFailWithMessage("Error: body text != textToCompare --> bodyText = " + object.getText() + " ************  !=  *********** textToCompare = " +cucumberBody);
                }
                return;
            } else {
                device.waitForIdle();
                onView(withId(R.id.toolbar_container)).check(matches(isDisplayed()));
            }
        }
    }

    private void compareTextWithWebViewText(String textToCompare) {
        UiObject2 wb;
        String[] webViewText = new String[1];
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
        while (true) {
            try {
                device.waitForIdle();
                waitUntilIdle();
                wb = device.findObject(By.clazz("android.webkit.WebView"));
                wb.click();
                swipeUpScreen();
                webViewText = wb.getChildren().get(0).getText().split("\n");
            } catch (Exception ex) {
                Timber.i("Cannot find webView: " + ex.getMessage());
            }
            if (webViewText[0].equals(textToCompare)) {
                device.waitForIdle();
                return;
            } else {
                assertFailWithMessage("Message Body text is different");
            }
        }
    }

    public void startActivity() {
        device.pressHome();
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
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
                        device.waitForIdle();
                        UiObject2 checkbox = object.getParent().getParent().getChildren().get(1).getChildren().get(0);
                        if (checkbox.isChecked() != check){
                            device.waitForIdle();
                            checkbox.longClick();
                            device.waitForIdle();
                        }
                        if (checkbox.isChecked() == check) {
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

    public void scrollUpToSubject (){
        UiObject2 scroll;
        do {
            try {
                scroll = device.findObject(By.clazz("android.widget.ScrollView"));
                device.waitForIdle();
                scroll.swipe(Direction.DOWN, 1.0f);
                device.waitForIdle();
            } catch (Exception e) {
                pressBack();
            }
        } while (!viewIsDisplayed(R.id.subject));
        onView(withId(R.id.subject)).check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.subject)).perform(click());
    }

    public void scrollToCehckBoxAndCheckIt(boolean isChecked, int view) {
        scrollToView(resources.getString(view));
        if (isChecked) {
            checkBoxOnScreenChecked(view, false);
        }
        checkBoxOnScreenChecked(view, true);
        if (!isChecked) {
            checkBoxOnScreenChecked(view, false);
        }
    }

    public void scrollToViewAndClickIt(int view) {
        scrollToView(resources.getString(view));
        selectFromScreen(view);
    }

    public void scrollToView (String text){
        UiObject textView = device.findObject(new UiSelector().text(text).className("android.widget.TextView"));
            device.waitForIdle();
            Espresso.onIdle();
        try {
            textView.dragTo(500,500,30);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setCheckBox(String resourceText, boolean checked) {
        BySelector selector = By.clazz("android.widget.CheckBox");
        while (true) {
            for (UiObject2 checkbox : device.findObjects(selector)) {
                try {
                    if (checkbox.getText().contains(resourceText)) {
                        device.waitForIdle();
                        if (checkbox.isChecked() != checked){
                            device.waitForIdle();
                            checkbox.longClick();
                            device.waitForIdle();
                        }
                        if (checkbox.isChecked() == checked) {
                            device.waitForIdle();
                            return;
                        }
                    }
                } catch (Exception ex){
                    Timber.i("Cannot find text on screen: " + ex);
                }
            }
        }
    }

    public String getKeySyncAccount (int account) {
        return testConfig.getKeySync_account(account);
    }

    public static void getJSONObject(String object) {
        switch (object) {
            case "keys":
                String keys = null;
                while (keys == null) {
                    try {
                        keys = json.getJSONObject("decryption_results").get(object).toString();
                    } catch (JSONException e) {
                        Timber.i("JSON file: " +e.getMessage());
                        e.printStackTrace();
                    }
                }
                if (!keys.contains("47220F5487391A9ADA8199FD8F8EB7716FA59050")) {
                    assertFailWithMessage("Wrong key");
                }
                break;
            case "rating":
            case "rating_string":
                try {
                    rating = json.getJSONObject("decryption_results").get(object).toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "messageBody":
                try {
                    while (json == null) {
                        String js = readJsonFile("results.json");
                        json = new JSONObject(js);
                    }
                    json = json.getJSONObject("attributes");
                    object = "decrypted";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            default:
                try {
                    json = json.getJSONObject(object);
                    Iterator x = json.keys();
                    jsonArray = new JSONArray();
                    while (x.hasNext()) {
                        jsonArray.put(json.get((String) x.next()));
                    }
                } catch (JSONException e) {
                    Timber.i("");
                }
        }
    }

    private static String readJsonFile(String fileName) {
        File directory = new File(Environment.getExternalStorageDirectory().toString());
        File newFile = new File(directory, "Download/" + fileName);
        while (!newFile.exists()) {
            swipeUpScreen();
            downloadAttachedFile(fileName);
            waitUntilIdle();
            device.waitForIdle();
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
            } finally {
                newFile.delete();
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
        String keySync_number;

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
            keySync_number = "0";
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
        void setKeySync_number(String number) { this.keySync_number = number;}

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
        String getKeySync_number() { return keySync_number;}
    }
}
