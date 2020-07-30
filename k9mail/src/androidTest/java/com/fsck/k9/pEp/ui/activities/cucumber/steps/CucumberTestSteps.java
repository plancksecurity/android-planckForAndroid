
package com.fsck.k9.pEp.ui.activities.cucumber.steps;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.ui.activities.TestUtils;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Timer;
import java.util.TimerTask;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import foundation.pEp.jniadapter.Rating;
import timber.log.Timber;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.containstText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.getTextFromView;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.saveSizeInInt;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.viewIsDisplayed;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.waitUntilIdle;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
public class CucumberTestSteps {

    private static final String HOST = "@sq.pep.security";

    private boolean syncThirdDevice = false;

    private String[] bot;
    private int accounts = 3;
    private int accountSelected = 0;
    public String b ="";

    private String fileName = "";

    private UiDevice device;
    private TestUtils testUtils;
    private Instrumentation instrumentation;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;
    private Resources resources;
    private String trustWords;
    private final Timer timer = new Timer();
    private final int[] time = {0};
    @Rule
    public IntentsTestRule<MessageList> activityTestRule = new IntentsTestRule<>(MessageList.class, true, false);

    @Before
    public void setup() {
        if (testUtils == null) {
            instrumentation = InstrumentationRegistry.getInstrumentation();
            device = UiDevice.getInstance(instrumentation);
            testUtils = new TestUtils(device, instrumentation);
            testUtils.increaseTimeoutWait();
            espressoTestingIdlingResource = new EspressoTestingIdlingResource();
            IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
            bot = new String[9];
            resources = getTargetContext().getResources();
            //startTimer(2000);
            device.waitForIdle();
            if (testUtils.getCurrentActivity() == null) {
                //startTimer(350);
                testUtils.testReset = true;
                try {
                    activityTestRule.launchActivity(new Intent());
                    device.waitForIdle();
                } catch (Exception ex) {
                    Timber.i("Cannot launch activity");
                }
            }
        }
    }

    @After
    public void tearDown() {
        try {
            IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
        } catch (Exception ex) {
            Timber.i("Error in After: " + ex.getMessage());
        }
        if (!exists(onView(withId(R.id.available_accounts_title))) && exists(onView(withId(R.id.message_list)))) {
            testUtils.selectFromMenu(R.string.action_settings);
            device.waitForIdle();
            Espresso.onIdle();
        }
        while (!exists(onView(withId(R.id.available_accounts_title)))) {
            device.waitForIdle();
            if (exists(onView(withText(R.string.discard_action)))) {
                device.waitForIdle();
                onView(withText(R.string.discard_action)).perform(click());
            }
            testUtils.pressBack();
            device.waitForIdle();
        }
        device.waitForIdle();
        Espresso.onIdle();
        onView(withId(R.id.available_accounts_title)).check(matches(isDisplayed()));
        activityTestRule.finishActivity();
        device.waitForIdle();
    }

    @When(value = "^I created an account$")
    public void I_create_account() {
        device.waitForIdle();
        if (!exists(onView(withId(R.id.accounts_list))) && !exists(onView(withId(android.R.id.list)))) {
            testUtils.createAccount();
        } else if (exists(onView(withId(R.id.add_account_container)))){
            if (exists(onView(withId(R.id.accounts_list)))) {
                int[] accounts = new int[1];
                try {
                    onView(withId(R.id.accounts_list)).perform(saveSizeInInt(accounts, 0));
                } catch (Exception ex) {
                    Timber.i("Cannot get accounts list size: " + ex.getMessage());
                }
                if (accounts[0] == 0) {
                    testUtils.createNAccounts(testUtils.getTotalAccounts(), false, false);
                }
            }
        }
    }


    @When("^I enter (\\S+) in the messageTo field")
    public void I_fill_messageTo_field(String cucumberMessageTo) {
        timeRequiredForThisMethod(15);
        device.waitForIdle();
        while (!exists(onView(withId(R.id.to)))) {
            TestUtils.swipeUpScreen();
        }
        switch (cucumberMessageTo) {
            case "empty":
                cucumberMessageTo = "";
                testUtils.removeTextFromTextView("to");
                break;
            case "myself":
                cucumberMessageTo = getTextFromView(onView(withId(R.id.identity)));
                break;
            case "bot1":
                Timber.i("Filling message to bot1");
                cucumberMessageTo = bot[0] + "acc" + accountSelected + HOST;
                break;
            case "bot2":
                Timber.i("Filling message to bot2");
                cucumberMessageTo = bot[1] + "acc" + accountSelected + HOST;
                break;
            case "bot3":
                Timber.i("Filling message to bot3");
                cucumberMessageTo = bot[2] + "acc" + accountSelected + HOST;
                break;
            case "bot4":
                Timber.i("Filling message to bot4");
                cucumberMessageTo = bot[3] + "acc" + accountSelected + HOST;
                break;
            case "bot5":
                Timber.i("Filling message to bot5");
                cucumberMessageTo = bot[4] + "acc" + accountSelected + HOST;
                break;
            case "bot6":
                Timber.i("Filling message to bot6");
                cucumberMessageTo = bot[5] + "acc" + accountSelected + HOST;
                break;
            case "bot7":
                Timber.i("Filling message to bot7");
                cucumberMessageTo = bot[6] + "acc" + accountSelected + HOST;
                break;
            case "bot8":
                Timber.i("Filling message to bot8");
                cucumberMessageTo = bot[7] + "acc" + accountSelected + HOST;
                break;
            case "bot9":
                Timber.i("Filling message to bot4");
                cucumberMessageTo = bot[8] + "acc" + accountSelected + HOST;
                break;
        }
        if (!(getTextFromView(onView(withId(R.id.to))).equals("") || getTextFromView(onView(withId(R.id.to))).equals(" "))) {
            try {
                fillMessage(cucumberMessageTo);
            } catch (Exception ex) {
                Timber.i("Couldn't fill message: " + ex.getMessage());
            }
        } else {
            boolean filled = false;
            while (!filled) {
                try {
                    device.waitForIdle();
                    onView(withId(R.id.to)).check(matches(isDisplayed()));
                    onView(withId(R.id.to)).perform(closeSoftKeyboard());
                    device.waitForIdle();
                    fillMessage(cucumberMessageTo);
                    onView(withId(R.id.to)).perform(closeSoftKeyboard());
                    filled = true;
                } catch (Exception ex) {
                    Timber.i("Couldn't find view: " + ex.getMessage());
                }
            }
        }
        try {
            testUtils.typeTextToForceRatingCaltulation(R.id.subject);
            onView(withId(R.id.message_content)).perform(click(), closeSoftKeyboard());
            onView(withId(R.id.to)).check(matches(isDisplayed()));
        } catch (Exception ex) {
            Timber.i("Couldn't find view: " + ex.getMessage());
        }
    }

    @When("^I enter (\\S+) in the messageSubject field")
    public void I_fill_subject_field(String cucumberSubject) {
        timeRequiredForThisMethod(15);
        textViewEditor(cucumberSubject,"subject");
    }

    @When("^I enter (\\S+) in the messageBody field")
    public void I_fill_body_field(String cucumberBody) {
        timeRequiredForThisMethod(1);
        textViewEditor(cucumberBody, "message_content");
    }

    private void textViewEditor (String text, String viewName) {
        int viewId = testUtils.intToID(viewName);
        while (!exists(onView(withId(viewId)))) {
            device.waitForIdle();
            TestUtils.swipeDownScreen();
            device.waitForIdle();
        }
        switch (text) {
            case "empty":
                timeRequiredForThisMethod(30);
                testUtils.removeTextFromTextView(viewName);
                break;
            case "longText":
                timeRequiredForThisMethod(3000);
                device.waitForIdle();
                BySelector selector = By.clazz("android.widget.EditText");
                UiObject2 uiObject = device.findObject(By.res("security.pEp.debug:id/message_content"));
                UiObject2 scroll;
                for (UiObject2 object : device.findObjects(selector)) {
                    if (object.getResourceName().equals(uiObject.getResourceName())) {
                        while (!object.getText().contains(testUtils.longText())) {
                            try {
                                scroll = device.findObject(By.clazz("android.widget.ScrollView"));
                                device.waitForIdle();
                                object.click();
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        testUtils.setClipboard(testUtils.longText());
                                    }
                                });
                                for (int i = 0; i < 80; i++) {
                                    device.waitForIdle();
                                    scroll.swipe(Direction.UP, 1.0f);
                                    testUtils.pasteClipboard();
                                    device.waitForIdle();
                                }
                                object.click();
                            } catch (Exception ex) {
                                Timber.i("Cannot fill long text: " + ex.getMessage());
                            }
                        }
                    }
                }
                Espresso.onIdle();
                testUtils.scrollUpToSubject();
                return;
            default:
                timeRequiredForThisMethod(10);
                testUtils.scrollUpToSubject();
                while (!(containstText(onView(withId(viewId)), text))) {
                    try {
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                        onView(withId(viewId)).perform(click());
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                        onView(withId(viewId)).perform(typeTextIntoFocusedView(text), closeSoftKeyboard());
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                    } catch (Exception ex) {
                        if (viewIsDisplayed((viewId))) {
                            onView(withId(viewId)).perform(closeSoftKeyboard());
                            testUtils.pressBack();
                        }
                    }
                }
        }
    }


    @When("^I compare (\\S+) from json file with (\\S+)")
    public void I_compare_jsonfile_with_string(String name, String stringToCompare) {
        timeRequiredForThisMethod(10);
        TestUtils.getJSONObject(name);
        switch (name) {
            case "rating":
            case "rating_string":
                assertText(TestUtils.rating, stringToCompare);
                break;
            case "messageBody":
                if (stringToCompare.contains("longText")) {
                    stringToCompare = testUtils.longText();
                }
                assertTextInJSON(TestUtils.json, stringToCompare);
                break;
            default:
                assertTextInJSONArray(name, TestUtils.jsonArray, stringToCompare);
        }
    }

    @When("^I compare messageBody with (\\S+)")
    public void I_compare_body(String cucumberBody) {
        timeRequiredForThisMethod(10);
        testUtils.compareMessageBodyWithText(cucumberBody);
    }


    @When("^I wait for the message and click it$")
    public void I_wait_for_the_message_and_click_it() {
        timeRequiredForThisMethod(45);
        testUtils.waitForMessageAndClickIt();
    }

    @When("^I click the last message received$")
    public void I_click_the_last_message_received() {
        timeRequiredForThisMethod(10);
        testUtils.clickLastMessage();
    }

    @When("^I click message at position (\\d+)$")
    public void I_click_message_at_position(int position) {
        timeRequiredForThisMethod(10);
        testUtils.clickMessageAtPosition(position);
    }

    @When("^I confirm trust words match$")
    public void I_confirm_trust_words_match() {
        timeRequiredForThisMethod(80);
        TestUtils.getJSONObject("trustwords");
        testUtils.goToHandshakeDialog();
        confirmAllTrustWords(TestUtils.jsonArray);
    }

    @When("^I click mistrust words$")
    public void I_click_mistrust_words() {
        timeRequiredForThisMethod(30);
        testUtils.goToHandshakeDialog();
        onView(withId(R.id.rejectHandshake)).perform(click());
        device.waitForIdle();
        testUtils.pressBack();
    }

    @When("^I reset handshake$")
    public void I_reset_handshake() {
        timeRequiredForThisMethod(10);
        testUtils.selectFromMenu(R.string.pep_title_activity_privacy_status);
        onView(withId(R.id.button_identity_key_reset)).perform(click());
        device.waitForIdle();
        testUtils.pressBack();
    }

    @Then("^I check there is an extra key$")
    public void I_check_there_is_an_extra_key() {
        timeRequiredForThisMethod(80);
        TestUtils.getJSONObject("keys");
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.toolbar);
        if (!TestUtils.jsonArray.toString().contains("47220F5487391A9ADA8199FD8F8EB7716FA59050")) {
            TestUtils.assertFailWithMessage("Wrong extra key");
        }
    }

    @Then("^I check there is an extra key on Key Management$")
    public void I_check_there_is_an_extra_key_management() {
        timeRequiredForThisMethod(80);
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.toolbar);
        testUtils.assertsTextExistsOnScreen("4722 0F54 8739 1A9A DA81\n99FD 8F8E B771 6FA5 9050");
    }

    private void confirmAllTrustWords (JSONArray array) {
        checkTrustWords(array, "short");
        device.waitForIdle();
        onView(withId(R.id.trustwords)).perform(click());
        checkTrustWords(array, "long");
    }

    private void checkTrustWords(JSONArray array, String words) {
        BySelector selector = By.clazz("android.widget.ListView");
        int size = 1;
        for (int positionToClick = 0; positionToClick < size; positionToClick++) {
            device.waitForIdle();
            Espresso.onIdle();
            onView(withId(R.id.change_language)).perform(click());
            if (size == 1) {
                size = calculateNewSize(size, selector);
            }
            device.waitForIdle();
            Espresso.onIdle();
            selectLanguage(positionToClick, size, selector);
            if (words.equals("short")) {
                getTrustWords(R.id.shortTrustwords);
            } else {
                getTrustWords(R.id.longTrustwords);
            }
            assertTextInJSONArray(trustWords, array, words);
        }
    }

    private void assertTrustWords(JSONArray array, String words) {
        for (int position = 0; position < array.length(); position++) {
            try {
                if (trustWords.contains(((JSONObject) array.get(position)).get(words).toString())) {
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        TestUtils.assertFailWithMessage("Wrong Trust Words");
    }
    private void assertTextInJSONArray(String text, JSONArray array, String textToCompare) {
        for (int position = 0; position < array.length(); position++) {
            try {
                if (text.contains(((JSONObject) array.get(position)).get(textToCompare).toString())) {
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        TestUtils.assertFailWithMessage("Text is not in JSON");
    }

    private void assertTextInJSON(JSONObject json, String textToCompare) {
        device.waitForIdle();
        if (json.toString().contains(textToCompare)) {
            return;
        }
        TestUtils.assertFailWithMessage("json file doesn't contain the text: " + json.toString() + " ***TEXT*** : " + textToCompare);
    }

    private void assertText(String text, String textToCompare) {
        if (text.contains(textToCompare)) {
            return;
        }
        TestUtils.assertFailWithMessage("Texts are different");
    }

    private void confirmAllTrustWords (String webViewText) {
        BySelector selector = By.clazz("android.widget.CheckedTextView");
        int size = 1;
        for (int positionToClick = 0; positionToClick < size; positionToClick++) {
            device.waitForIdle();
            testUtils.selectFromMenu(R.string.settings_language_label);
            size = calculateNewSize(size, selector);
            device.waitForIdle();
            selectLanguage(positionToClick, size, selector);
            //getTrustWords();
            String []trustWordsSplited = trustWords.split("\\s+");
            checkWordIsInText(trustWordsSplited, webViewText);
        }
    }

    private  int calculateNewSize(int size, BySelector selector) {
        while (size <= 1) {
            device.waitForIdle();
            size = device.findObjects(selector).get(0).getChildren().size();
        }
        return size;
    }

    private void selectLanguage(int positionToClick, int size, BySelector selector) {
            device.waitForIdle();
            for (int position = 0; position < size; position++) {
                if (position == positionToClick) {
                    while (device.findObjects(selector).get(0).getChildren().size() <= 1){
                        device.waitForIdle();
                    }
                        try {
                            device.waitForIdle();
                            device.findObjects(selector).get(0).getChildren().get(position).longClick();
                            device.waitForIdle();
                        } catch (Exception ex) {
                            Timber.i("Cannot click language selected");
                        }
                    try {
                        device.waitForIdle();
                        onView(withId(android.R.id.button1)).perform(click());
                        device.waitForIdle();
                    } catch (Exception ex) {
                        Timber.i("Cannot find button1");
                    }
                }
            }
    }

    private void getTrustWords(int trustWordsId) {
        do {
            try {
                device.waitForIdle();
                trustWords = getTextFromView(onView(withId(trustWordsId)));
            } catch (Exception ex) {
                Timber.i("Cannot find trustWords: " + ex.getMessage());
            }
        } while (trustWords == null);
    }

    private void checkWordIsInText(String [] arrayToCompare, String webViewText) {
        for (String textToCompare : arrayToCompare) {
            if (!webViewText.contains(textToCompare)) {
                TestUtils.assertFailWithMessage("Text not found in Trustwords");
            }
        }
    }

    private String getWebviewText(){
        String webViewText = "empty";
        UiObject2 wb;
        boolean webViewLoaded = false;
        while (!webViewLoaded) {
            try {
                device.waitForIdle();
                waitUntilIdle();
                wb = device.findObject(By.clazz("android.webkit.WebView"));
                wb.click();
                TestUtils.swipeUpScreen();
                UiObject2 webViewTemporal;
                webViewTemporal = wb.getChildren().get(0);
                while (true) {
                    if (webViewTemporal.getText().contains("long")) {
                        webViewText = webViewTemporal.getText();
                        webViewLoaded = true;
                        device.waitForIdle();
                        break;
                    } else {
                        try {
                            webViewTemporal = webViewTemporal.getChildren().get(0);
                        } catch (Exception ex) {
                            webViewTemporal = wb.getChildren().get(1);
                        }
                    }
                }
            } catch (Exception ex) {
                Timber.i("Cannot find webView: " + ex.getMessage());
            }
            device.waitForIdle();
        }
        return webViewText;
    }

    @When("^I click stop trusting words$")
    public void I_click_stop_trusting_words() {
        timeRequiredForThisMethod(10);
        testUtils.goToHandshakeDialog();
        onView(withId(R.id.buttonHandshake)).perform(click());
        device.waitForIdle();
        testUtils.pressBack();
    }

    @When("^I click confirm trust words$")
    public void I_click_confirm_trust_words() {
        timeRequiredForThisMethod(10);
        testUtils.goToHandshakeDialog();
        while (!viewIsDisplayed(R.id.confirmHandshake)) {
            TestUtils.swipeUpScreen();
        }
        while (!exists(onView(withId(R.id.confirmHandshake)))) {
            device.waitForIdle();
            waitUntilIdle();
        }
        onView(withId(R.id.confirmHandshake)).check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.confirmHandshake)).perform(click());
        device.waitForIdle();
        testUtils.pressBack();
    }

    private int getTotalMessagesSize() {
        LocalStore localStore;
        int size = -1;
        while (size == -1) {
            try {
                localStore = getLocalStore();
                size = localStore.getMessageCount();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    private Account getAccount(String email) {
        email = email.replaceAll("\\s+","");
        for (Account account : Preferences.getPreferences(K9.app).getAccounts()) {
            if (account.getEmail().equalsIgnoreCase(email)) {
                return account;
            }
        }
        return null;
    }
    public LocalStore getLocalStore() throws MessagingException {
        Account ac = null;
        while (ac == null) {
            ac = getAccount(testUtils.getKeySyncAccount(0));
        }
        return LocalStore.getInstance(ac, K9.app);
    }

    @When("^I sync devices (\\S+) and (\\S+)$")
    public void I_sync_devices(String device1, String device2) {
        int inboxMessages = testUtils.getListSize();
        int totalMessages = getTotalMessagesSize();
        switch (testUtils.keySync_number()) {
            case "1":
                if (device1.equals("A") || device2.equals("A")) {
                    testUtils.syncDevices();
                }
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    testUtils.selectAccount(0);
                }
                break;
            case "2":
                if (device1.equals("B") || device2.equals("B")) {
                    testUtils.syncDevices();
                }
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    testUtils.selectAccount(0);
                }
                break;
            case "3":
                if (device1.equals("C") || device2.equals("C")) {
                    testUtils.syncDevices();
                }
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    testUtils.selectAccount(1);
                }
                break;
            default:
                Timber.i("Cannot sync this device");
                break;
        }
        testUtils.getMessageListSize();
        if (totalMessages >= getTotalMessagesSize()) {
            testUtils.assertFailWithMessage("There are more sync messages before sync than after sync");
        }
        if (inboxMessages != 0 && inboxMessages != testUtils.getListSize()) {
            testUtils.assertFailWithMessage("Sync messages went to wrong folder");
        }
        testUtils.getMessageListSize();
    }
    @When("^I create an account for sync on device C$")
    public void I_create_an_account_for_C() {
        switch (testUtils.keySync_number()) {
            case "1":
                testUtils.getMessageListSize();
                testUtils.composeMessageButton();
                testUtils.fillMessage(new TestUtils.BasicMessage("",
                                "C creates account",
                                "ready",
                                testUtils.getKeySyncAccount(1)),
                        false);
                while (exists(onView(withId(R.id.send)))) {
                    testUtils.clickView(R.id.send);
                }
                break;
            case "2":

                break;
            case "3":
                testUtils.waitForNewMessage();
                testUtils.createNAccounts(2, true, true);
                I_select_account("1");
                break;
            default:
                Timber.i("Cannot create");
                break;
        }
        testUtils.getMessageListSize();
        syncThirdDevice = true;
    }

    @When("^I check devices (\\S+) and (\\S+) are sync$")
    public void I_check_1_and_2_sync(String firstDevice, String secondDevice) {
        switch (testUtils.keySync_number()) {
            case "1":
                testUtils.checkDeviceIsSync("A", firstDevice, secondDevice, syncThirdDevice);
                break;
            case "2":
                testUtils.checkDeviceIsSync("B", firstDevice, secondDevice, syncThirdDevice);
                break;
            case "3":
                testUtils.checkDeviceIsSync("C", firstDevice, secondDevice, syncThirdDevice);
                break;
            default:
                TestUtils.assertFailWithMessage("Unknown Sync Device to check devices are sync");
                break;
        }
    }

    @When("^I check devices (\\S+) and (\\S+) are not sync$")
    public void I_check_A_B_are_not_sync(String firstDevice, String secondDevice) {
        switch (testUtils.keySync_number()) {
            case "1":
                testUtils.checkDeviceIsNotSync("A", firstDevice, secondDevice, syncThirdDevice);
                break;
            case "2":
                testUtils.checkDeviceIsNotSync("B", firstDevice, secondDevice, syncThirdDevice);
                break;
            case "3":
                testUtils.checkDeviceIsNotSync("C", firstDevice, secondDevice, syncThirdDevice);
                break;
            default:
                TestUtils.assertFailWithMessage("Unknown Sync Device to check devices are not sync");
                break;
        }
    }

    @When("^I disable sync on device (\\S+)$")
    public void I_disable_sync(String device) {
        if (device.equals("C")) {
            testUtils.setTrustWords("Disabled sync on device C");
        }
        switch (testUtils.keySync_number()) {
            case "1":
                if (device.equals("A")) {
                    testUtils.disableKeySync();
                }
                else {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "2":
                if (device.equals("B")) {
                    testUtils.disableKeySync();
                }
                else {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "3":
                if (device.equals("C")) {
                    testUtils.disableKeySync();
                }
                else {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                Timber.i("Unknown Device to disable sync");
                break;
        }
        while (!exists(onView(withId(R.id.message_list)))) {
            testUtils.pressBack();
        }
        testUtils.getMessageListSize();
    }

    @When("^I enable sync on device (\\S+)$")
    public void I_enable_sync(String device) {
        switch (testUtils.keySync_number()) {
            case "1":
                if (device.equals("A")) {
                    testUtils.enableKeySync();
                }
                break;
            case "2":
                if (device.equals("B")) {
                    testUtils.enableKeySync();
                }
                break;
            case "3":
                if (device.equals("C")) {
                    testUtils.enableAccountGlobalKeySync();
                }
                break;
            default:
                Timber.i("Unknown Device to enable sync");
                break;
        }
        testUtils.waitForIdle();
        if (!exists(onView(withId(R.id.message_list)))) {
            testUtils.pressBack();
        }
    }

    @When("^I keySync device C$")
    public void I_keySync_devices_C() {

    }

    @When("^I setup second account for devices A and B$")
    public void I_setup_second_account_for_devices_A_B() {

    }

    @When("^I remove from keySync device C$")
    public void I_remove_from_keySync_device_C() {

    }

    @When("^I reject trust words$")
    public void I_reject_trust_words() {
        timeRequiredForThisMethod(10);
        testUtils.clickStatus();
        while (!exists(onView(withId(R.id.rejectHandshake)))) {
            device.waitForIdle();
            waitUntilIdle();
        }
        onView(withId(R.id.rejectHandshake)).check(matches(isCompletelyDisplayed()));
        Espresso.onIdle();
        TestUtils.swipeUpScreen();
        onView(withId(R.id.rejectHandshake)).perform(click());
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
    }

    @When("^I check in the handshake dialog if the privacy status is (\\S+)$")
    public void I_check_pEp_status(String status) {
        timeRequiredForThisMethod(20);
        checkPrivacyStatus(status);
        device.waitForIdle();
    }

    private void checkPrivacyStatus(String status){
        Rating statusRating = null;
        BySelector selector = By.clazz("android.widget.ScrollView");
        while (!viewIsDisplayed(R.id.toolbar)) {
            device.waitForIdle();
            waitUntilIdle();
        }
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
            try {
                testUtils.typeTextToForceRatingCaltulation(R.id.subject);
            } catch (Exception ex) {
                for (UiObject2 object : device.findObjects(selector)) {
                    boolean actionPerformed = false;
                    while (!actionPerformed) {
                        try {
                            object.swipe(Direction.DOWN, 1);
                            actionPerformed = true;
                        } catch (Exception e) {
                            Timber.i("Couldn't swipe down view: " + e.getMessage());
                        }
                    }
                }
                try {
                    testUtils.typeTextToForceRatingCaltulation(R.id.subject);
                } catch (Exception e) {
                    Timber.i("Cannot find subject");
                }
            }
            device.waitForIdle();
            waitUntilIdle();
        switch (status){
            case "pEpRatingUndefined":
                statusRating = Rating.pEpRatingUndefined;
                break;
            case "pEpRatingCannotDecrypt":
                statusRating = Rating.pEpRatingCannotDecrypt;
                break;
            case "pEpRatingHaveNoKey":
                statusRating = Rating.pEpRatingHaveNoKey;
                break;
            case "pEpRatingUnencrypted":
                statusRating = Rating.pEpRatingUnencrypted;
                break;
            case "pEpRatingUnencryptedForSome":
                statusRating = Rating.pEpRatingUnencryptedForSome;
                break;
            case "pEpRatingUnreliable":
                statusRating = Rating.pEpRatingUnreliable;
                break;
            case "pEpRatingReliable":
                statusRating = Rating.pEpRatingReliable;
                break;
            case "pEpRatingTrusted":
                statusRating = null;
                status = "pep_green";
                break;
            case "pEpRatingTrustedAndAnonymized":
                statusRating = Rating.pEpRatingTrustedAndAnonymized;
                break;
            case "pEpRatingFullyAnonymous":
                statusRating = Rating.pEpRatingFullyAnonymous;
                break;
            case "pEpRatingMistrust":
                statusRating = null;
                status = "pep_red";
                break;
            case "pEpRatingB0rken":
                statusRating = Rating.pEpRatingB0rken;
                break;
            case "pEpRatingUnderAttack":
                statusRating = Rating.pEpRatingUnderAttack;
                break;
        }
        if (statusRating != null) {
            testUtils.assertMessageStatus(statusRating);
        } else {
            testUtils.checkToolbarColor(testUtils.colorToID(status));
        }
    }

    @And("^I select from message menu (\\S+)$")
    public void I_select_from_message_menu(String textToSelect){
        timeRequiredForThisMethod(15);
        testUtils.selectFromMenu(testUtils.stringToID(textToSelect));
        device.waitForIdle();
    }

    @And("^I disable protection from privacy status menu$")
    public void I_disable_protection_from_privacy_status_menu(){
        timeRequiredForThisMethod(15);
        testUtils.selectFromMenu(testUtils.stringToID("pep_title_activity_privacy_status"));
        testUtils.selectFromMenu(testUtils.stringToID("pep_force_unprotected"));
        testUtils.pressBack();
        device.waitForIdle();
    }

    @Then("^I open menu$")
    public void I_select_from_menu(){
        timeRequiredForThisMethod(10);
        device.waitForIdle();
        testUtils.openOptionsMenu();
    }

    @Then("^I select from screen (\\S+)$")
    public void I_select_from_screen(String textToSelect){
        timeRequiredForThisMethod(15);
        device.waitForIdle();
        testUtils.selectFromScreen(testUtils.stringToID(textToSelect));
        device.waitForIdle();
    }

    @Then("^I walk through app$")
    public void I_walk_through_app(){
        timeRequiredForThisMethod(15);
        testUtils.openOptionsMenu();
        testUtils.selectFromScreen(testUtils.stringToID("action_settings"));
        aboutMenu();
        walkThroughDisplay();
        walkThroughInteraction();
        walkThroughNotifications();
        walkThroughPrivacy();
        walkThroughAdvanced();
    }

    private void aboutMenu () {
        testUtils.openOptionsMenu();
        testUtils.selectFromScreen(testUtils.stringToID("about_action"));
        String aboutText = getTextFromView(onView(withId(R.id.aboutText)));
        String[][] shortTextInAbout = new String[3][2];
        shortTextInAbout[0] = resources.getString(testUtils.stringToID("app_authors_fmt")).split("%");
        shortTextInAbout[1] = resources.getString(testUtils.stringToID("app_libraries")).split("%");
        shortTextInAbout[2] = resources.getString(testUtils.stringToID("app_copyright_fmt")).split("%");
        if (!aboutText.contains(shortTextInAbout[0][0])
                || !aboutText.contains(shortTextInAbout[1][0])
                || !aboutText.contains(shortTextInAbout[2][0])) {
            TestUtils.assertFailWithMessage("Wrong text in About");
        }
        testUtils.pressBack();
    }

    private void walkThroughDisplay () {
        testUtils.selectFromScreen(testUtils.stringToID("display_preferences"));
        testUtils.selectFromScreen(testUtils.stringToID("settings_language_label"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_settings_title"));
        testUtils.selectFromScreen(testUtils.stringToID("font_size_account_list"));
        testUtils.selectFromScreen(testUtils.stringToID("font_size_account_name"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_account_description"));
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_folder_list"));
        testUtils.selectFromScreen(testUtils.stringToID("font_size_folder_name"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_folder_status"));
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_list"));
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_list_subject"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_list_sender"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_list_date"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_list_preview"));
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_view"));
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_list_sender"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_view_to"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_view_cc"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_list_subject"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_view_date"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_view_additional_headers"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_view_content"));
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_compose"));
        testUtils.selectFromScreen(testUtils.stringToID("font_size_message_compose_input"));
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("animations_title"));
        testUtils.scrollToView(resources.getString(R.string.accountlist_preferences));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("measure_accounts_title"));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("count_search_title"));
        testUtils.scrollToView(resources.getString(testUtils.stringToID("folderlist_preferences")));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_folderlist_wrap_folder_names_label"));
        testUtils.scrollToView(resources.getString(testUtils.stringToID("messagelist_preferences")));
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("global_settings_preview_lines_label"));
        testUtils.pressBack();
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_flag_label"));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_checkbox_label"));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_show_correspondent_names_label"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("global_settings_sender_above_subject_label"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("global_settings_show_contact_name_label"));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_show_contact_picture_label"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("global_settings_colorize_missing_contact_pictures_label"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("global_settings_background_as_unread_indicator_label"));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_threaded_view_label"));
        testUtils.scrollToView(resources.getString(testUtils.stringToID("messageview_preferences")));
        testUtils.selectFromScreen(testUtils.stringToID("global_settings_messageview_visible_refile_actions_title"));
        testUtils.pressBack();
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_messageview_autofit_width_label"));
        testUtils.selectFromScreen(testUtils.stringToID("account_settings_push_advanced_title"));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_messageview_fixedwidth_label"));
        testUtils.pressBack();
    }

    private void walkThroughInteraction() {
        testUtils.selectFromScreen(testUtils.stringToID("interaction_preferences"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("gestures_title"));
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("volume_navigation_title"));
        testUtils.pressBack();
        testUtils.scrollToView(resources.getString(R.string.global_settings_messageiew_after_delete_behavior_title));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("global_settings_messageview_return_to_list_label"));
        testUtils.scrollToCehckBoxAndCheckIt(true, testUtils.stringToID("global_settings_messageview_show_next_label"));
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("global_settings_confirm_actions_title"));
        testUtils.pressBack();
        testUtils.selectFromScreen(testUtils.stringToID("account_settings_push_advanced_title"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("start_integrated_inbox_title"));
        testUtils.pressBack();
    }

    private void walkThroughNotifications() {
        testUtils.selectFromScreen(testUtils.stringToID("notifications_title"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("quiet_time"));
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("global_settings_notification_quick_delete_title"));
        testUtils.pressBack();
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("global_settings_lock_screen_notification_visibility_title"));
        testUtils.pressBack();
        testUtils.pressBack();
    }

    private void walkThroughPrivacy() {
        testUtils.selectFromScreen(testUtils.stringToID("privacy_preferences"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("pep_passive_mode"));
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("pep_forward_warning"));
        testUtils.selectFromScreen(testUtils.stringToID("account_settings_push_advanced_title"));
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("master_key_management"));
        testUtils.pressBack();
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("master_key_management"));
        testUtils.pressBack();
        testUtils.scrollToCehckBoxAndCheckIt(false, testUtils.stringToID("global_settings_privacy_hide_timezone"));
        testUtils.pressBack();
    }

    private void walkThroughAdvanced() {
        testUtils.selectFromScreen(testUtils.stringToID("account_settings_push_advanced_title"));
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("settings_attachment_default_path"));
        testUtils.pressBack();
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("background_ops_label"));
        testUtils.pressBack();
    }

    @Then("^I remove account$")
    public void I_remove_account() {
        timeRequiredForThisMethod(25);
        testUtils.goBackAndRemoveAccount();
    }

    @Then("^I remove email address$")
    public void I_remove_email_address(){
        timeRequiredForThisMethod(20);
        device.waitForIdle();
        device.pressKeyCode(KeyEvent.KEYCODE_DEL);
        device.waitForIdle();
    }

    @Then("^I attach files to message$")
    public void I_attach_files_to_message() {
        timeRequiredForThisMethod(30);
        testUtils.fillMessage(new TestUtils.BasicMessage("", "", "", ""), true);
        testUtils.sendMessage();
        testUtils.testReset = true;
    }

    @Then("^I attach (\\S+)$")
    public void I_attach_file_to_message(String file) {
        timeRequiredForThisMethod(15);
        device.waitForIdle();
        Set_external_mock(file);
        testUtils.attachFile(fileName);
        device.waitForIdle();
        testUtils.testReset = true;
    }

    @Given("^Set external mock (\\S+)$")
    public void Set_external_mock(String mock){
        timeRequiredForThisMethod(10);
        int raw = 0;
        switch (mock){
            case "settings":
                raw = R.raw.settingsthemedark;
                fileName = "settings.k9s";
                break;
            case "settingsthemedark":
                raw = R.raw.settingsthemedark;
                fileName = "settingsthemedark.k9s";
                break;
            case "MSoffice":
                raw = R.raw.testmsoffice;
                fileName = "testmsoffice.docx";
                break;
            case "PDF":
                raw = R.raw.testpdf;
                fileName = "testpdf.pdf";
                break;
            case "masterKey":
                raw = R.raw.masterkeypro;
                fileName = "masterkey.asc";
                break;
            case "picture":
                raw = R.raw.testpicture;
                fileName = "testpicture.png";
        }
        while (true) {
            try {
                TestUtils.createFile(fileName, raw);
                device.waitForIdle();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @When("^I open privacy status$")
    public void I_click_message_status() {
        timeRequiredForThisMethod(10);
        testUtils.clickMessageStatus();
        device.waitForIdle();
    }

    @Then("^I click the send message button$")
    public void I_click_then_send_message_button() {
        timeRequiredForThisMethod(5);
        while (exists(onView(withId(R.id.send)))) {
            testUtils.clickView(R.id.send);
        }
    }

    @When("^I click compose message")
    public void I_click_message_compose_button() {
        timeRequiredForThisMethod(5);
        testUtils.composeMessageButton();
    }

    @When("^I run the tests")
    public void I_run_the_tests() {
        startTest(0);
    }


    @When("^I select account (\\S+)$")
    public void I_select_account(String account) {
        accountSelected = Integer.parseInt(account);
        while (testUtils.getTotalAccounts() == -1) {
            testUtils.readConfigFile();
        }
        if (!(accountSelected < testUtils.getTotalAccounts())) {
            skipTest("No more accounts");
        }
        startTest(accountSelected);
    }

    @When("^I remove account (\\S+)$")
        public void I_remove_account (String account) {
        int accountToRemove = Integer.parseInt(account);
        while (true) {
            try {
                device.waitForIdle();
                openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
                device.waitForIdle();
            } catch (Exception ex) {
                Timber.i("Cannot open menu");
                break;
            }
        }
        testUtils.selectFromMenu(R.string.action_settings);
        device.waitForIdle();
        while (true) {
            try {
                device.waitForIdle();
                if (exists(onView(withId(R.id.accounts_list)))) {
                    while (!viewIsDisplayed(R.id.accounts_list)) {
                        device.waitForIdle();
                    }
                    onView(withId(R.id.accounts_list)).check(matches(isCompletelyDisplayed()));
                    while (exists(onView(withId(R.id.accounts_list)))) {
                        device.waitForIdle();
                        onData(anything()).inAdapterView(withId(R.id.accounts_list)).atPosition(accountToRemove).perform(longClick());
                        device.waitForIdle();
                        BySelector selector = By.clazz("android.widget.TextView");
                        for (UiObject2 object : device.findObjects(selector)) {
                            if (object.getText().equals(resources.getString(R.string.remove_account_action))) {
                                object.click();
                                device.waitForIdle();
                                testUtils.clickAcceptButton();
                                return;
                            }
                        }

                    }
                }
            } catch (Exception ex) {
                Timber.i("Cannot remove account: " + ex);
            }
        }
    }

    private void skipTest (String text) {
        throw new cucumber.api.PendingException(text);
    }

    public void startTest(int accountToStart) {
        boolean botListFull = false;
        while (!botListFull) {
            botListFull = true;
            testUtils.readBotList();
            for (int bot = 0; bot < testUtils.botList.length; bot++) {
                if (testUtils.botList[bot] == null) {
                    botListFull = false;
                }
            }
        }
        bot = testUtils.botList;
        testUtils.selectAccount(accountToStart);
    }

    @And("^I click view (\\S+)$")
    public void I_click_view(String viewClicked){
        timeRequiredForThisMethod(10);
        device.waitForIdle();
        testUtils.clickView(testUtils.intToID(viewClicked));
        device.waitForIdle();
    }

    @And("^I search for (\\d+) (?:message|messages) with text (\\S+)$")
    public void I_click_search_and_search_for_text(int messages, String text){
        timeRequiredForThisMethod(25);
        testUtils.goBackToMessageList();
        int[] messageListSize = new int[1];
        device.waitForIdle();
        while (!exists(onView(withId(R.id.search)))) {
            device.waitForIdle();
            testUtils.pressBack();
        }
        testUtils.clickSearch();
        if (exists(onView(withId(R.id.search_clear)))) {
            try {
                onView(withId(R.id.search_clear)).perform(click());
                device.waitForIdle();
                onView(withId(R.id.search)).perform(click());
                device.waitForIdle();
            } catch (Exception e) {
                Timber.i("Cannot clear text in search box");
            }
        }
        device.waitForIdle();
        onView(withId(R.id.search_input)).perform(typeText(text));
        device.waitForIdle();
        onView(withId(R.id.search_input)).perform(pressImeActionButton(), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 0));
        if (messageListSize[0] - 1 != messages) {
            TestUtils.assertFailWithMessage("There are not " + messages + " messages in the list. There are: " + (messageListSize[0] - 1));
        }
    }

    @And("^I click search button$")
    public void I_click_search_button(){
        testUtils.clickSearch();
    }

    @And("^I click reply message$")
    public void I_click_reply_message(){
        timeRequiredForThisMethod(10);
        device.waitForIdle();
        while (!viewIsDisplayed(R.id.openCloseButton)) {
            device.waitForIdle();
        }
        onView(withId(R.id.openCloseButton)).check(matches(isDisplayed()));
        testUtils.clickView(testUtils.intToID("openCloseButton"));
        device.waitForIdle();
        device.waitForIdle();
        while (!viewIsDisplayed(R.id.message_content)) {
            device.waitForIdle();
        }
        testUtils.typeTextToForceRatingCaltulation(R.id.message_content);
    }

    @Then("^I send (\\d+) (?:message|messages) to (\\S+) with subject (\\S+) and body (\\S+)$")
    public void I_send_message_to_address(int totalMessages,String botName, String subject, String body) {
        String messageTo = "nothing";
        switch (botName){
            case "bot1":
                messageTo = bot[0] + "acc" + accountSelected + HOST;
                break;
            case "bot2":
                messageTo = bot[1] + "acc" + accountSelected + HOST;
                break;
            case "bot3":
                messageTo = bot[2] + "acc" + accountSelected + HOST;
                break;
            case "bot4":
                messageTo = bot[3] + "acc" + accountSelected + HOST;
                break;
            case "bot5":
                messageTo = bot[4] + "acc" + accountSelected + HOST;
                break;
            case "bot6":
                messageTo = bot[5] + "acc" + accountSelected + HOST;
                break;
            case "bot7":
                messageTo = bot[6] + "acc" + accountSelected + HOST;
                break;
            case "bot8":
                messageTo = bot[7] + "acc" + accountSelected + HOST;
                break;
            case "bot9":
                messageTo = bot[8] + "acc" + accountSelected + HOST;
                break;
        }
        for (int message = 0; message < totalMessages; message++) {
            device.waitForIdle();
            if (exists(onView(withId(R.id.fab_button_compose_message)))) {
                testUtils.composeMessageButton();
            }
            device.waitForIdle();
            testUtils.fillMessage(new TestUtils.BasicMessage("", subject, body, messageTo), false);
            device.waitForIdle();
            testUtils.sendMessage();
            device.waitForIdle();
            testUtils.waitForNewMessage();
        }
        device.waitForIdle();
    }

    private void fillMessage(String to){
        testUtils.fillMessage(new TestUtils.BasicMessage("", " ", " ", to), false);

    }

    @Then("^I wait for the new message$")
    public void I_wait_for_the_new_message(){
        timeRequiredForThisMethod(40);
        testUtils.waitForNewMessage();
    }

    @And("^I go to the sent folder$")
    public void I_go_to_the_sent_folder(){
        timeRequiredForThisMethod(25);
        device.waitForIdle();
        testUtils.goBackToMessageList();
        testUtils.goToFolder(resources.getString(R.string.special_mailbox_name_sent));
    }

    @And("^I enable passive mode$")
    public void I_enable_passive_mode(){
        timeRequiredForThisMethod(25);
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.action_settings);
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.settings_import_global_settings);
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.app_name);
        device.waitForIdle();
        testUtils.checkBoxOnScreenChecked(testUtils.stringToID("pep_passive_mode"), true);
    }

    @And("^I go to the drafts folder$")
    public void I_go_to_the_drafts_folder(){
        timeRequiredForThisMethod(25);
        device.waitForIdle();
        testUtils.goBackToMessageList();
        testUtils.goToFolder(resources.getString(R.string.special_mailbox_name_drafts));
    }

    @And("^I click the first message$")
    public void I_click_the_first_message(){
        timeRequiredForThisMethod(12);
        testUtils.clickFirstMessage();
    }

    @Given("^I create and remove (\\d+) accounts$")
    public void I_create_and_remove_accounts(int total){
        for (int account = 0; account < total; account++) {
            timeRequiredForThisMethod(100);
            testUtils.createAccount();
            testUtils.goBackAndRemoveAccount();
            device.waitForIdle();
        }
    }

    @Given("^I test (\\d+) threads with address (\\S+)$")
    public void I_summon_threads(int total, String address){
        try {
            for (int i = 0; i < total; i++) {
                I_fill_messageTo_field(address);
                testUtils.summonThreads();
                I_fill_messageTo_field("empty");
                testUtils.summonThreads();
            }
        } catch (Exception ex) {
            Timber.e("Error summoning");
        }
    }

    @Then("^I discard the message$")
    public void I_discard_the_message(){
        timeRequiredForThisMethod(10);
        device.waitForIdle();
        testUtils.pressBack();
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
    }

    @Given("^I press back$")
    public void I_press_back(){
        timeRequiredForThisMethod(2);
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
    }

    @And("^I go back to app$")
    public void I_go_back_to_app(){
        timeRequiredForThisMethod(15);
        testUtils.getActivityInstance();
    }

    public void externalAppRespondWithFile(int id) {
        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(Activity.RESULT_OK, insertFileIntoIntentAsData(id));
        intending(not(isInternal()))
                .respondWith(activityResult);
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

    @Then("^I check if the privacy status is (\\S+)$")
    public void I_check_toolBar_color_is(String color) {
        timeRequiredForThisMethod(10);
        try {
            TestUtils.swipeUpScreen();
            TestUtils.swipeDownScreen();
            TestUtils.swipeDownScreen();
            testUtils.typeTextToForceRatingCaltulation(R.id.subject);
        } catch (Exception ex) {
            Timber.i("Cannot find subject field");
        }
        device.waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        device.waitForIdle();
        checkPrivacyStatus(color);
        device.waitForIdle();
    }

    @And("^I go back to the Inbox$")
    public void I_go_back_to_the_Inbox(){
        timeRequiredForThisMethod(15);
        testUtils.goBackToMessageList();
    }

    @And("^I check color is (\\S+) at position (\\d+)$")
    public void I_check_color_is___at_position(String color, int position){
        timeRequiredForThisMethod(5);
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(position)).check(matches(withBackgroundColor(testUtils.colorToID(color))));
        device.waitForIdle();
    }

    @And("^I open attached files$")
    public void I_open_attached_files() {
        testUtils.emptyFolder("Download");
        openAttached();
        device.waitForIdle();
        File directory = new File(Environment.getExternalStorageDirectory().toString()+"/Download/");
        File[] files = directory.listFiles();
        byte[] buffer= new byte[8192];
        int count;
        for (File fileOpen : files) {
            timeRequiredForThisMethod(5);
            File file = new File(Environment.getExternalStorageDirectory().toString() + "/Download/" + fileOpen.getName());
            device.waitForIdle();
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                while ((count = bis.read(buffer)) > 0) {
                    digest.update(buffer, 0, count);
                }
                bis.close();
                byte[] hash = digest.digest();
                String shaCode = new BigInteger(1, hash).toString(16);
                String jsonObject = (testUtils.returnJSON()).getJSONObject("attachments_in").get("decrypted").toString();
                if (!jsonObject.contains(shaCode)) {
                    TestUtils.assertFailWithMessage("couldn't find shaCode in json file");
                }
            } catch (Exception ex) {
                Timber.i("Couldn't get SHA256 from file: " + file.getName());
            }
        }
        testUtils.emptyFolder("Download");
    }

    @And("^I open attached Master Key$")
    public void I_open_attached_Master_Key() {
        testUtils.emptyFolder("Download");
        String masterKeyText = "";
        String masterKeyText2;
        while (masterKeyText.equals("")) {
            openAttachedMasterKey();
            device.waitForIdle();
            try {
                masterKeyText = testUtils.readFile("/Download/", "masterkey.asc").toString();
            } catch (Exception e) {
                Timber.i("Trying to read masterkey.asc file: " + e.getMessage());
            }
        }
        TestUtils.createFile("masterkeyfile.asc", R.raw.masterkeypro);
        masterKeyText2 = testUtils.readFile("", "masterkeyfile.asc").toString();
        if (!masterKeyText.equals(masterKeyText2)) {
            TestUtils.assertFailWithMessage("Wrong Master key file");
        }
        testUtils.emptyFolder("Download");
        testUtils.emptyFolder("");
    }

    private void openAttached () {
        while (true) {
            try {
                while (!exists(onView(withId(R.id.attachments)))) {
                    TestUtils.swipeUpScreen();
                }
                onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                while (!viewIsDisplayed(R.id.attachments)) {
                    TestUtils.swipeUpScreen();
                }
                TestUtils.swipeUpScreen();
                BySelector layout = By.clazz("android.widget.LinearLayout");
                onView(withId(R.id.attachments)).check(matches(isCompletelyDisplayed()));
                for (UiObject2 object : device.findObjects(layout)) {
                    if (object.getResourceName() != null && object.getResourceName().equals("security.pEp.debug:id/attachments")) {
                        int size = object.getChildren().size();
                        for (int attachment = 0; attachment < size; attachment++) {
                            if (!object.getChildren().get(attachment).getChildren().get(2).getText().contains("results.json")) {
                                object.getChildren().get(attachment).getChildren().get(0).click();
                            }
                        }
                        Timber.i("");
                        return;
                    }
                }
            } catch (Exception ex) {
                Timber.i("Message Error: " + ex.getMessage());
            }
        }
    }

    private void openAttachedMasterKey () {
        while (true) {
            try {
                TestUtils.swipeUpScreen();
                onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                TestUtils.swipeUpScreen();
                BySelector layout = By.clazz("android.widget.LinearLayout");
                for (UiObject2 object : device.findObjects(layout)) {
                    if (object.getResourceName() != null && object.getResourceName().equals("security.pEp.debug:id/attachments") && object.getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(1).getText().contains("masterkey")) {
                        object.getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(3).click();
                        return;
                    }
                }
            } catch (Exception ex) {
                Timber.i("Message Error: " + ex.getMessage());
            }
        }
    }

    @Then("^I open Hamburger Menu$")
    public void I_open_hamburger_menu() {
        testUtils.openHamburgerMenu();
    }

    @Then("^I set checkbox (\\S+) to (true|false)$")
    public void I_set_checkbox_to(String resource, boolean checked){
        timeRequiredForThisMethod(5);
        testUtils.checkBoxOnScreenChecked(testUtils.stringToID(resource), checked);
    }

    @Then("^I go back and save as draft$")
    public void I_go_back_and_save_as_draft(){
        timeRequiredForThisMethod(10);
        testUtils.goBackAndSaveAsDraft(activityTestRule);
    }

    @Then("^I save as draft$")
    public void I_save_as_draft(){
        timeRequiredForThisMethod(10);
        testUtils.selectFromMenu(R.string.save_draft_action);
    }

    @And("^I compare texts on screen: (\\S+) and (\\S+)$")
    public void I_compare_texts_on_screen(String text1, String text2) {
        timeRequiredForThisMethod(5);
        testUtils.assertsTextsOnScreenAreEqual(testUtils.stringToID(text1), testUtils.stringToID(text2));
    }

    @And("^I check status color is (\\S+) at position (\\d+)$")
    public void I_check_color_at(String color, int position) {
        timeRequiredForThisMethod(5);
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(position)).check(matches(withBackgroundColor(testUtils.colorToID(color))));
    }

    @Then("^I click acceptButton$")
    public void iClickAcceptButton() {
        timeRequiredForThisMethod(5);
        device.waitForIdle();
        testUtils.clickAcceptButton();
        device.waitForIdle();
    }

    @And("^I do next thing$")
    public void iDoNextThing() {
        timeRequiredForThisMethod(5);
        onView(withId(R.id.accounts_list)).perform(ViewActions.click());
        device.waitForIdle();
        try{
            Assert.assertEquals(K9.Theme.LIGHT, K9.getK9Theme());
        }catch (AssertionFailedError exception){
            Timber.e("Theme is not light");
        }
    }
    @Then("^I wait (\\d+) seconds$")
    public void I_wait_seconds(int seconds) {
        timeRequiredForThisMethod(seconds);
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @And("^I save trustWords$")
    public void I_save_trustwords(){
        timeRequiredForThisMethod(10);
        device.waitForIdle();
        onView(withId(R.id.securityStatusText)).perform(click());
        device.waitForIdle();
        trustWords = getTextFromView(onView(withId(R.id.trustwords)));
        testUtils.pressBack();
        device.waitForIdle();
    }

    @Then("^I save test report$")
    public void I_save_report(){
        //IMPORTANT!!!!!!!!!!!!!!!!   Go to CucumberTestCase.java and modify plugin line before creating save_report.apk
        timeRequiredForThisMethod(30);
        try {
            SetDirectory();
        } catch (Exception ex){
            Timber.e("Error moving cucumber reports1: " + ex.getMessage());
        }
    }
    private void SetDirectory() {
        CopyAssets(); // Then run the method to copy the file.
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {


        } else if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Timber.e("Nope");
        }
    }

    private void CopyAssets() {
        File file = null;
            try {
                String extStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
                file = new File("/data/data/security.pEp.debug/cucumber-reports/", "cucumber.json");
                File file2 = new File(extStorageDirectory + "/cucumber.json");
                FileInputStream in = new FileInputStream(file);
                file2.createNewFile();
                OutputStream out = new FileOutputStream(file2);
                copyFile(in, out);
                file.delete();
                in.close();
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e("tag", e.getMessage());
            }
        if (!file.exists()) {
            Assume.assumeTrue("File cucumber.json doesn't exist",false);
        }
    }

    private void copyFile(InputStream in, OutputStream out) {
        byte[] buffer = new byte[1024];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception ex) {
            Timber.e("Error guardando");
        }
    }

    private void timeRequiredForThisMethod(int timeRequired) {
        time[0] = time[0] - timeRequired;
    }

    private void startTimer(int finalTime) {
        if (time[0] == 0) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    time[0]++;
                    Timber.i("Timeout: " + time[0] + "/" + finalTime);
                    if (activityTestRule == null) {
                        time[0] = 0;
                        TestUtils.assertFailWithMessage("Timeout. Couldn't finish the test");
                    } else if (time[0] > finalTime) {
                        try {
                            time[0] = 0;
                            TestUtils.assertFailWithMessage("Timeout. Couldn't finish the test");
                        } catch (Exception ex) {
                            Timber.e("Couldn't close the test");
                        }
                    }
                }
            }, 0, 1000);
        }
    }
}