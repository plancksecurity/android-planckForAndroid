
package com.fsck.k9.planck.ui.activities.cucumber.steps;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.planck.EspressoTestingIdlingResource;
import com.fsck.k9.planck.ui.activities.SplashActivity;
import com.fsck.k9.planck.ui.activities.TestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import foundation.pEp.jniadapter.Rating;
import timber.log.Timber;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.openLinkWithText;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.planck.ui.activities.TestUtils.json;
import static com.fsck.k9.planck.ui.activities.TestUtils.swipeDownList;
import static com.fsck.k9.planck.ui.activities.TestUtils.swipeDownScreen;
import static com.fsck.k9.planck.ui.activities.TestUtils.waitForIdle;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.containstText;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.getTextFromView;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.saveSizeInInt;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.viewIsDisplayed;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.waitUntilIdle;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withRecyclerView;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class CucumberTestSteps {

    private static final String HOST = "@bot.planck.dev";

    private boolean syncThirdDevice = false;

    private String[] bot;
    private final int accounts = 3;
    private int accountSelected = 0;
    private boolean pep_enable_privacy_protection = true;

    private String fileName = "";

    private UiDevice device;
    private TestUtils testUtils;
    private Instrumentation instrumentation;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;
    private Resources resources;
    private String trustWords;
    String fingerprint = "empty";
    private final Timer timer = new Timer();
    private final int[] time = {0};

    private ActivityScenario<SplashActivity> scenario;

    @Before
    public void setup() throws IOException {
        if (testUtils == null) {
            instrumentation = InstrumentationRegistry.getInstrumentation();
            device = UiDevice.getInstance(instrumentation);
            testUtils = new TestUtils(device, instrumentation);
            //testUtils.increaseTimeoutWait();
            espressoTestingIdlingResource = new EspressoTestingIdlingResource();
            IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
            bot = new String[9];
            resources = getApplicationContext().getResources();
            //startTimer(2000);
            //testUtils.testReset = true;
        }
        Intents.init();
        I_switch_wifi("on");
        try {
            waitForIdle();
            scenario = ActivityScenario.launch(SplashActivity.class);
            while (TestUtils.getCurrentActivity() == null || scenario == null) {
                try {
                    waitForIdle();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.e("TEST","Estoy en BeforeCatch: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        try {
            IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
        } catch (Exception ex) {
            Timber.i("Error in After: " + ex.getMessage());
        }
        try {
            if (exists(onView(withId(R.id.actionbar_title_first)))) {
                if (getTextFromView(onView(withId(R.id.actionbar_title_first))).equals(resources.getString(R.string.search_results))) {
                    testUtils.pressBack();
                    waitForIdle();
                }
            }
        } catch (Exception exception) {
            Timber.i("Action bar doesn't exist");
        }
        try {
            if (!exists(onView(withId(R.id.available_accounts_title))) && exists(onView(withId(R.id.message_list)))) {
                testUtils.selectFromMenu(R.string.action_settings);
                waitForIdle();
            }
        } catch (Exception exception) {
            Timber.i("App could be closed");
        }
        try {
            if (!exists(onView(withId(R.id.account_email)))) {
                if (!exists(onView(withId(R.id.available_accounts_title)))) {
                    waitForIdle();
                    if (exists(onView(withText(R.string.discard_action)))) {
                        waitForIdle();
                        onView(withText(R.string.discard_action)).perform(click());
                    }
                    if (BuildConfig.IS_ENTERPRISE) {
                        testUtils.pressBack();
                    }
                    waitForIdle();
                }
            }
        } catch (Exception exception) {
            Timber.i("App could be closed");
        }
        try {
            testUtils.clearAllRecentApps();
        } catch (Exception exception) {
            Timber.i("Intents.init was not called before Intents.release");
        }
        if (BuildConfig.IS_ENTERPRISE) {
            //RestrictionsManager.resetSettings();
        }
    }

    @When(value = "^I created an account$")
    public void I_create_account() {
        /*if (BuildConfig.IS_ENTERPRISE) {
            String account = testUtils.getAccountAddress(0);
            if (testUtils.test_number().equals("1") || testUtils.test_number().equals("2")) {
                account = testUtils.getSyncAccount(0);
            }
            I_set_string_setting("pep_enable_sync_account", "true");
            I_set_string_setting("account_description", "ThisIsUserName");
            I_set_string_setting("account_display_count", "50");
            I_set_string_setting("max_push_folders", "50");
            I_set_string_setting("account_remote_search_num_results", "50");
            I_set_string_setting("account_email_address", account);
            I_set_incoming_settings("peptest.ch", "SSL/TLS", 993, account);
            I_set_outgoing_settings("peptest.ch", "STARTTLS", 587, account);
        }*/
        waitForIdle();
        try {
            if (!exists(onView(withId(R.id.message_list)))) {
                testUtils.allowPermissions(1);
            }
        } catch (Exception exception) {
            testUtils.allowPermissions(1);
        }
        if (exists(onView(withId(R.id.passphrase)))) {
            testUtils.readConfigFile();
            while (getTextFromView(onView(withId(R.id.passphrase))).equals("")) {
                try {
                    waitForIdle();
                    onView(withId(R.id.passphrase)).perform(click());
                    waitForIdle();
                    onView(withId(R.id.passphrase)).perform(typeText(testUtils.getPassphrasePassword()), closeSoftKeyboard());
                } catch (Exception ex) {
                    Timber.i("Cannot fill account email: " + ex.getMessage());
                }
            }
            waitForIdle();
            onView(withId(R.id.afirmativeActionButton)).perform(click());
        }
        waitForIdle();
        if (!exists(onView(withId(R.id.accounts_list))) && !exists(onView(withId(android.R.id.list))) && !exists(onView(withId(R.id.message_list)))) {
            testUtils.createAccount();
        } else if (viewIsDisplayed(onView(withId(R.id.add_account_container)))) {
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

    @When("^I assert account (\\S+) is in the (\\S+) field")
    public void I_assert_account_in_field(String text, String field) {
        waitForIdle();
        int viewID = 0;
        switch (field) {
            case "CC":
                viewID = R.id.cc;
                break;
            case "BCC":
                viewID = R.id.bcc;
                break;
            case "messageTo":
                viewID = R.id.to;
                break;
            default:
                break;
        }
        text = accountAddress(text);
        testUtils.assertTextInView(text,viewID);
    }

    @When("^I enter max characters in the (\\S+) field")
    public void I_enter_max_text_in_field(String field) {
        int i = 0;
        try {
            for (; i < 1000000; i++) {
                I_enter_text_in_field("ABCDEFGH" + i + " ", field);
            }
        } catch (Exception ex) {
            fail("Only " + i + " loops in the field " + field);
        }
    }

        @When("^I enter (\\S+) in the (\\S+) field")
        public void I_enter_text_in_field(String text, String field) {
        waitForIdle();
        int viewID = 0;
        String resourceID = "";
        switch (field) {
            case "CC":
                viewID = R.id.cc;
                resourceID = "cc";
                break;
            case "BCC":
                viewID = R.id.bcc;
                resourceID = "bcc";
                break;
            case "messageTo":
                viewID = R.id.to;
                resourceID = "to";
                getBotsList();
                break;
            case "messageSubject":
                I_fill_subject_field(text);
                return;
            case "messageBody":
                I_fill_body_field(text);
                return;
            default:
                break;
        }
        text = accountAddress(text);
        if (viewIsDisplayed(R.id.recipient_expander)) {
            onView(withId(R.id.recipient_expander)).perform(click());
        }
        while (!viewIsDisplayed(viewID)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waitForIdle();
        }
        testUtils.typeTextInField(text, viewID, resourceID);
        try {
            testUtils.scrollDownToSubject();
        } catch (Exception ex) {
            Timber.i("Is not possible to scroll Down");
        }
        testUtils.typeTextToForceRatingCalculation(R.id.subject);
        onView(withId(R.id.toolbar)).perform(closeSoftKeyboard());
        if (field.equals("BCC")) {
            try {
                BySelector selector;
                selector = By.clazz("android.widget.EditText");
                for (UiObject2 textView : device.findObjects(selector)) {
                    if (textView.getResourceName().equals(BuildConfig.APPLICATION_ID+":id/subject")) {
                        textView.click();
                        waitForIdle();
                        textView.setText(" ");
                        waitForIdle();
                        break;
                    }
                }
                testUtils.typeTextToForceRatingCalculation(R.id.subject);
                onView(withId(R.id.toolbar)).perform(click(), closeSoftKeyboard());
                onView(withId(viewID)).check(matches(isDisplayed()));
            } catch (Exception ex) {
                Timber.i("Couldn't find view: " + ex.getMessage());
            }
        }
    }

    @When("^I enter (\\S+) in the message Subject field")
    public void I_fill_subject_field(String cucumberSubject) {
        timeRequiredForThisMethod(15);
        textViewEditor(cucumberSubject, "subject");
    }

    @When("^I enter (\\S+) in the message Body field")
    public void I_fill_body_field(String cucumberBody) {
        timeRequiredForThisMethod(1);
        textViewEditor(cucumberBody, "message_content");
    }

    @When("^I enter (\\d+) recipients in the messageTo field")
    public void I_fill_n_recipients(int recipients, boolean isBot) {
        timeRequiredForThisMethod(1);
        String address = "@any.mail";
        if (isBot) {
            address = "@bot.planck.dev";
        }
        UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
        for (int loop = 0; loop < recipients; loop++) {
            scroll.swipe(Direction.UP, 1f);
            scroll.swipe(Direction.UP, 1f);
            waitForIdle();
            testUtils.clickView(R.id.subject);
            waitForIdle();
            scroll.swipe(Direction.UP, 1f);
            scroll.swipe(Direction.UP, 1f);
            waitForIdle();
            testUtils.scrollDownToSubject();
            waitForIdle();
            onView(withId(R.id.to_label)).perform(click());
            waitForIdle();
            onView(withId(R.id.to)).perform(typeText(loop + "of" + recipients + "_" + System.currentTimeMillis() + address));
            waitForIdle();
        }
    }

    @When("^I enter (\\d+) unreliable recipients in the (\\S+) field")
    public void I_fill_recipients(int recipients, String field) {
        timeRequiredForThisMethod(1);
        String recipient = "recipients@email.pep";
        UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
        scroll.swipe(Direction.UP, 1f);
        String loopText = "firstaccountinthelist@this.is";
        testUtils.clickView(R.id.subject);
        waitForIdle();
        testUtils.clickView(R.id.to_label);
        waitForIdle();
        onView(withId(R.id.to)).perform(typeText(loopText));
        testUtils.typeTextToForceRatingCalculation(R.id.subject);
        loopText = "";
        for (int loop = 1; loop < recipients + 1; loop++) {
            waitForIdle();
            loopText = loopText + loop + "of" + recipients + recipient + "; ";
            waitForIdle();
        }
        testUtils.typeTextInField("", R.id.to, "to");
        testUtils.typeTextInField(BuildConfig.APPLICATION_ID, R.id.subject, "subject");
        waitForIdle();
        onView(withId(R.id.to)).perform(typeText(loopText));
        testUtils.typeTextToForceRatingCalculation(R.id.subject);
        waitForIdle();
        onView(withId(R.id.toolbar)).perform(closeSoftKeyboard());
        waitForIdle();
    }

    @When("^I stress Engine threads with (\\d+) recipients")
    public void I_stress_Engine(int recipients) {
        for (int loop = 0; loop < 10000; loop++) {
            for (int loop2 = 0; loop2 < 40; loop2++) {
                I_click_message_compose_button();
                I_enter_text_in_field("myself", "messageTo");
                I_fill_recipients(3, "messageTo");
                I_remove_address_clicking_X(3);
                I_remove_address_clicking_X(2);
                I_remove_address_clicking_X(1);
                I_fill_n_recipients(recipients, false);
                I_remove_unsecure_email_addresses();
                I_fill_n_recipients(recipients, true);
                I_fill_subject_field("Loop " + loop + " - " + loop2 + " - Recipients " + recipients);
                I_click_the_send_message_button();
                I_wait_for_the_new_message();
                I_remove_all_messages();
                //I_send_message_to_address(1, "bot1", "loop: " + loop2, "Body");
            }
        }
    }

    @When("^I paste (\\d+) recipients in the (\\S+) field")
    public void I_paste_n_recipients(int recipients, String field) {
        timeRequiredForThisMethod(1);
        String recipient = "filling@email.pep";
        for (int loop = 0; loop < recipients; loop++) {
            I_enter_text_in_field(recipients + recipient, field);
        }
    }

    private void textViewEditor(String text, String viewName) {
        int viewId = testUtils.intToID(viewName);
        String messageText;
        int endOfLongMessage;
        while (!exists(onView(withId(viewId)))) {
            waitForIdle();
            TestUtils.swipeDownScreen();
            waitForIdle();
        }
        switch (text) {
            case "empty":
                timeRequiredForThisMethod(30);
                testUtils.removeTextFromTextView(viewName);
                break;
            case "longWord":
                messageText = testUtils.longWord();
                endOfLongMessage = 2;
                testUtils.insertTextNTimes(messageText, endOfLongMessage);
                break;
            case "longText":
                messageText = testUtils.longText();
                endOfLongMessage = 80;
                testUtils.insertTextNTimes(messageText, endOfLongMessage);
                break;
            case "specialCharacters":
                testUtils.insertTextNTimes(testUtils.specialCharacters(), 1);
                break;
            case "longSubject":
                text = testUtils.longText();
            default:
                timeRequiredForThisMethod(10);
                testUtils.scrollUpToSubject();
                while (!(containstText(onView(withId(viewId)), text))) {
                    try {
                        waitForIdle();
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                        waitForIdle();
                        onView(withId(viewId)).perform(click());
                        waitForIdle();
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                        waitForIdle();
                        onView(withId(viewId)).perform(typeTextIntoFocusedView(text), closeSoftKeyboard());
                        waitForIdle();
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                        waitForIdle();
                    } catch (Exception ex) {
                        if (viewIsDisplayed((viewId))) {
                            onView(withId(viewId)).perform(closeSoftKeyboard());
                            testUtils.pressBack();
                        }
                    }
                }
        }
    }

    @When("^I check insecurity warnings are not there")
    public void I_check_insecurity_warnings_are_not_there() {
        waitForIdle();
        if (viewIsDisplayed(onView(withId(R.id.snackbar_text)))) {
            fail("Is showing the Alert message and it shouldn't be there");
        }
    }


    @When("^I check unsecure warnings are there")
    public void I_check_unsecure_warnings_are_there() {
        // This method requires 2 or more Unsecure recipients to check the red color in the recipients and in the "+X"
        if (!viewIsDisplayed(onView(withId(R.id.user_action_banner)))) {
            fail("Is not showing the Alert message");
        }
        String unsecureText = resources.getQuantityString(testUtils.pluralsStringToID("compose_unsecure_delivery_warning"), 2);
        unsecureText = unsecureText.substring(4);
        if (!getTextFromView(onView(withId(R.id.user_action_banner))).contains(unsecureText)) {
            fail("The text in the Alert message is not correct");
        }
        if (!getTextFromView(onView(withId(R.id.to))).contains("+")) {
            fail("There is only 1 address or less and should be 2 or more");
        }

        BySelector selector;
        selector = By.clazz("android.widget.MultiAutoCompleteTextView");
        waitForIdle();
        for (UiObject2 multiTextView : device.findObjects(selector)) {
            int startingPointX = multiTextView.getVisibleBounds().left;
            int endPointX = multiTextView.getVisibleBounds().left + 100;
            int centerY = multiTextView.getVisibleCenter().y;
            boolean isRed = false;
            for (int x = startingPointX; x < endPointX; x++) {
                if (Color.valueOf(testUtils.getPixelColor(x, centerY)).blue() <= 0.8 &&
                        Color.valueOf(testUtils.getPixelColor(x, centerY)).green() <= 0.8) {
                    if (Color.valueOf(testUtils.getPixelColor(x, centerY)).red() >= 0.8 &&
                            Color.valueOf(testUtils.getPixelColor(x, centerY)).blue() <= 0.3 &&
                            Color.valueOf(testUtils.getPixelColor(x, centerY)).green() <= 0.3) {
                        isRed = true;
                        break;
                    }
                }
            }
            if (!isRed) {
                fail("Border color in the field TO is not red");
            }
            startingPointX = multiTextView.getVisibleBounds().centerX();
            endPointX = multiTextView.getVisibleBounds().centerX() + 100;
            centerY = multiTextView.getVisibleCenter().y;
            isRed = false;
            for (int x = startingPointX; x < endPointX; x++) {
                if (Color.valueOf(testUtils.getPixelColor(x, centerY)).blue() <= 0.8 &&
                        Color.valueOf(testUtils.getPixelColor(x, centerY)).green() <= 0.8) {
                    if (Color.valueOf(testUtils.getPixelColor(x, centerY)).red() >= 0.8 &&
                            Color.valueOf(testUtils.getPixelColor(x, centerY)).blue() <= 0.3 &&
                            Color.valueOf(testUtils.getPixelColor(x, centerY)).green() <= 0.3) {
                        isRed = true;
                        break;
                    }
                }
            }
            if (!isRed) {
                fail("Text color in the field TO is not red");
            }
            startingPointX = multiTextView.getVisibleBounds().right;
            endPointX = multiTextView.getVisibleBounds().left;
            isRed = false;
            for (int x = startingPointX; x > endPointX; x--) {
                if (Color.valueOf(testUtils.getPixelColor(x, centerY)).blue() <= 0.8 &&
                        Color.valueOf(testUtils.getPixelColor(x, centerY)).green() <= 0.8) {
                    if (Color.valueOf(testUtils.getPixelColor(x - 1, centerY)).red() >= 0.8 &&
                            Color.valueOf(testUtils.getPixelColor(x - 1, centerY)).blue() <= 0.3 &&
                            Color.valueOf(testUtils.getPixelColor(x - 1, centerY)).green() <= 0.3) {
                        isRed = true;
                        break;
                    }
                }
            }
            if (!isRed) {
                fail("Text color of the +X in field TO is not red");
            }
        }
    }

    @When("^I remove the (\\d+) address clicking X button")
    public void I_remove_address_clicking_X(int address) {
        testUtils.removeAddressClickingX(address);
    }

    @When("^I compare (\\S+) from json file with (\\S+)")
    public void I_compare_json_file_with_string(String name, String stringToCompare) {
        waitForIdle();
        TestUtils.getJSONObject(name);
        switch (name) {
            case "rating":
            case "rating_string":
                assertText(stringToCompare, TestUtils.rating);
                break;
            case "messageSubject":
                if (stringToCompare.contains("longSubject")) {
                    stringToCompare = testUtils.longText();
                }
            case "messageBody":
                if (stringToCompare.contains("longText")) {
                    while (!stringToCompare.equals(testUtils.longText())) {
                        stringToCompare = testUtils.longText();
                    }
                }
                if (json == null || json.equals("")) {
                    BySelector selector = By.clazz("android.widget.MessageWebView");
                    for (UiObject2 object : device.findObjects(selector)) {
                        if (!object.getText().contains(stringToCompare)) {
                            fail("Message is not containing: " + stringToCompare);
                        }
                    }
                } else {
                    assertTextInJSON(TestUtils.json, stringToCompare);
                }
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

    @When("^I check that the Calendar is correct and body text is (\\S+)")
    public void I_check_calendar(String bodyText) {
        timeRequiredForThisMethod(10);
        waitForIdle();
        onView(withId(R.id.showAllInvitees)).perform(click());
        waitForIdle();
        if (!getTextFromView(onView(withId(R.id.eventSummary))).equals("EVENT FINDE") ||
                !getTextFromView(onView(withId(R.id.eventLocation))).equals("KAME-HOUSE\n" +
                        "Southern Island, NBI 8250012 B, https://www.planck.security") ||
                !getTextFromView(onView(withId(R.id.eventTime))).contains("Sat Nov 13") ||
                !getTextFromView(onView(withId(R.id.shortInvitees))).equals("AttendeeName (attendee@mail.es)\n" +
                        "Master Roshi (turtle@mail.es)\n" +
                        "Organizer Name (organizer@mail.es) [Organizer]")) {
            fail("Wrong Calendar Text");
        }
        BySelector selector = By.clazz("android.webkit.WebView");
        for (UiObject2 webv : device.findObjects(selector)) {
            if (webv.getParent().getResourceName() != null &&
                    webv.getParent().getResourceName().equals(BuildConfig.APPLICATION_ID+":id/calendarInviteLayout") &&
                    !webv.getChildren().get(0).getChildren().get(0).getText().contains(bodyText)) {
                fail("Wrong message body");
            }
        }
        waitForIdle();
        ViewInteraction calendarButton = onView(withId(R.id.openCalendarImg));
        onView(withId(R.id.eventLocation)).perform(openLinkWithText("https://www.planck.security"));
        waitForIdle();
        for (int i = 0; i < 1500; i++) {
            waitForIdle();
        }
        if (testUtils.textExistsOnScreen("EVENT FINDE")) {
            fail("URLs has not been clicked");
        }
        while (!testUtils.textExistsOnScreen("EVENT FINDE")) {
            device.pressBack();
            waitForIdle();
        }
        testUtils.longClick("openCalendarImg");
        waitForIdle();
        if (viewIsDisplayed(calendarButton)) {
            fail("Calendar Button is not openning the calendar");
        }
        device.pressBack();
        waitForIdle();
        if (!viewIsDisplayed(calendarButton)) {
            fail("Calendar Button???");
        }
    }


    @When("^I wait for the message and click it$")
    public void I_wait_for_the_message_and_click_it() {
        timeRequiredForThisMethod(45);
        testUtils.waitForMessageAndClickIt();
    }

    @When("^I set (\\S+) setting to (\\S+)")
    public void I_set_string_setting(String setting, String value) {
        waitForIdle();
        if (value.equals("true") || value.equals("false")) {
            boolean valueB = value.equals("true");
            //RestrictionsManager.setBooleanRestrictions(setting, valueB);
            if (setting.equals("pep_enable_privacy_protection")) {
                pep_enable_privacy_protection = valueB;
            }
        } else {
            //RestrictionsManager.setStringRestrictions(setting, value);
        }
    }

    @When("^I set incoming settings to server (\\S+), securityType (\\S+), port (\\d+) and userName (\\S+)")
    public void I_set_incoming_settings(String server, String securityType,int port, String userName) {
        //RestrictionsManager.setIncomingBundleSettings(server, securityType, port, userName);
    }

    @When("^I set outgoing settings to server (\\S+), securityType (\\S+), port (\\d+) and userName (\\S+)")
    public void I_set_outgoing_settings(String server, String securityType,int port, String userName) {
        //RestrictionsManager.setOutgoingBundleSettings(server, securityType, port, userName);
    }

    @When("^I compare incoming settings with server (\\S+), securityType (\\S+), port (\\d+) and userName (\\S+)$")
    public void I_compare_incoming_settings(String server, String securityType,int port, String userName) {
        /*MailSettings settings = RestrictionsManager.getRestrictions();
        assert settings != null;
        if (!RestrictionsManager.compareSetting(server, securityType, port, userName, settings.getIncoming())) {
            fail("Incoming settings are not the same: " + server + " // " + settings.getIncoming().getServer() + " ; "  + securityType + " // " + settings.getIncoming().getSecurityType() + " ; "  + port + " // " + settings.getIncoming().getPort() + " ; "  + userName + " // " + settings.getIncoming().getUserName());
        }*/
    }

    @When("^I compare outgoing settings with server (\\S+), securityType (\\S+), port (\\d+) and userName (\\S+)$")
    public void I_compare_outgoing_settings(String server, String securityType,int port, String userName) {
        /*MailSettings settings = RestrictionsManager.getRestrictions();
        assert settings != null;
        if (!RestrictionsManager.compareSetting(server, securityType, port, userName, settings.getOutgoing())) {
            fail("Outgoing settings are not the same: " + server + " // " + settings.getOutgoing().getServer() + " ; "  + securityType + " // " + settings.getOutgoing().getSecurityType() + " ; "  + port + " // " + settings.getOutgoing().getPort() + " ; "  + userName + " // " + settings.getOutgoing().getUserName());
        }*/
    }

    @When("^I compare (\\S+) setting with (\\S+)$")
    public void I_compare_setting(String setting, String value) {
        /*String settingValue = RestrictionsManager.getSetting(setting);
        if (!settingValue.equals(value)) {
            fail("Setting " + setting + " has value " + settingValue + " and not " + value);
        }*/
    }

    @When("^I click the last message received$")
    public void I_click_the_last_message_received() {
        timeRequiredForThisMethod(10);
        testUtils.clickLastMessage();
    }

    @When("^I long click message at position (\\d+)$")
    public void I_long_click_the_last_message_received(int position) {
        timeRequiredForThisMethod(10);
        testUtils.longClickMessageAtPosition(position);
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
        testUtils.waitForView(R.id.negativeActionButton);
        onView(withId(R.id.negativeActionButton)).perform(click());
        waitForIdle();
        onView(withId(R.id.afirmativeActionButton)).perform(click());
        waitForIdle();
        onView(withId(R.id.afirmativeActionButton)).perform(click());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        waitForIdle();
    }

    @When("^I reset partner key$")
    public void I_reset_partner_key() {
        waitForIdle();
        testUtils.selectFromMenu(testUtils.stringToID("reset_partner_key_action"));
        waitForIdle();
        onView(withId(R.id.acceptButton)).perform(click());
        waitForIdle();
        onView(withId(R.id.cancelButton)).perform(click());
        waitForIdle();
    }

    @Then("^I check there is an extra key$")
    public void I_check_there_is_an_extra_key() {
        timeRequiredForThisMethod(80);
        TestUtils.getJSONObject("keys");
        waitForIdle();
        if (!TestUtils.jsonArray.toString().contains("47220F5487391A9ADA8199FD8F8EB7716FA59050")) {
            fail("Wrong extra key");
        }
    }

    @Then("^I check there is an extra key on Key Management$")
    public void I_check_there_is_an_extra_key_management() {
        timeRequiredForThisMethod(80);
        waitForIdle();
        testUtils.selectFromScreen(testUtils.stringToID("privacy_preferences"));
        testUtils.selectFromScreen(testUtils.stringToID("account_settings_push_advanced_title"));
        testUtils.scrollToViewAndClickIt(testUtils.stringToID("master_key_management"));
        waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        waitForIdle();
        onView(withId(R.id.extra_keys_view)).perform(swipeUp());
        waitForIdle();
        testUtils.assertsTextExistsOnScreen("4722 0F54 8739 1A9A DA81\n99FD 8F8E B771 6FA5 9050");
        testUtils.pressBack();
        testUtils.pressBack();
    }

    private void confirmAllTrustWords(JSONArray array) {
        checkTrustWords(array, "short");
        waitForIdle();
        onView(withId(R.id.show_long_trustwords)).perform(click());
        checkTrustWords(array, "long");
        waitForIdle();
    }

    private void checkTrustWords(JSONArray array, String words) {
        BySelector selector = By.clazz("android.widget.ListView");
        int size = 1;
        for (int positionToClick = 0; positionToClick < size; positionToClick++) {
            waitForIdle();
            testUtils.openOptionsMenu();
            if (size == 1) {
                size = calculateNewSize(size, selector);
            }
            waitForIdle();
            selectLanguage(positionToClick, size, selector);
            waitForIdle();
            if (words.equals("short")) {
                getTrustWords();
            } else {
                getTrustWords();
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
        fail("Wrong Trust Words");
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
        fail("Text is not in JSON");
    }

    private void assertTextInJSON(JSONObject json, String textToCompare) {
        waitForIdle();
        if (json.toString().contains(textToCompare)) {
            return;
        }
        fail("json file doesn't contain the text: " + json + " ***TEXT*** : " + textToCompare);
    }

    private void assertText(String expectedText, String textToCompare) {
        if (!expectedText.contains(textToCompare)) {
            fail("Texts are different. Expected <" + expectedText + "> but got <" + textToCompare + ">");
        }
    }

    private void confirmAllTrustWords(String webViewText) {
        BySelector selector = By.clazz("android.widget.CheckedTextView");
        int size = 1;
        for (int positionToClick = 0; positionToClick < size; positionToClick++) {
            waitForIdle();
            testUtils.selectFromMenu(R.string.settings_language_label);
            size = calculateNewSize(size, selector);
            waitForIdle();
            selectLanguage(positionToClick, size, selector);
            //getTrustWords();
            String[] trustWordsSplited = trustWords.split("\\s+");
            checkWordIsInText(trustWordsSplited, webViewText);
        }
    }

    private int calculateNewSize(int size, BySelector selector) {
        while (size <= 1) {
            waitForIdle();
            size = device.findObjects(selector).get(0).getChildren().size();
        }
        return size;
    }

    private void selectLanguage(int positionToClick, int size, BySelector selector) {
        waitForIdle();
        for (int position = 0; position < size; position++) {
            if (position == positionToClick) {
                while (device.findObjects(selector).get(0).getChildren().size() <= 1) {
                    waitForIdle();
                }
                try {
                    waitForIdle();
                    device.findObjects(selector).get(0).getChildren().get(position).longClick();
                    waitForIdle();
                } catch (Exception ex) {
                    Timber.i("Cannot click language selected");
                }
                try {
                    waitForIdle();
                    onView(withId(android.R.id.button1)).perform(click());
                    waitForIdle();
                } catch (Exception ex) {
                    Timber.i("Cannot find button1");
                }
            }
        }
    }

    private void getTrustWords() {
        waitForIdle();
        BySelector acceptButton = By.clazz("android.widget.TextView");
        while (true) {
            for (UiObject2 object : device.findObjects(acceptButton)) {
                if (object.getResourceName() != null && object.getResourceName().equals("security.planck.test.enterprise.debug:id/trustwords")) {
                    trustWords = object.getText();
                    return;
                }
            }
        }
    }

    private void checkWordIsInText(String[] arrayToCompare, String webViewText) {
        for (String textToCompare : arrayToCompare) {
            if (!webViewText.contains(textToCompare)) {
                fail("Text not found in Trustwords");
            }
        }
    }

    private String getWebviewText() {
        String webViewText = "empty";
        UiObject2 wb;
        boolean webViewLoaded = false;
        while (!webViewLoaded) {
            try {
                waitForIdle();
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
                        waitForIdle();
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
            waitForIdle();
        }
        return webViewText;
    }

    @When("^I switch (\\S+) Wi-Fi")
    public void I_switch_wifi(String state) throws IOException {
        switch (state){
            case "on":
                testUtils.setWifi(true);
                break;
            case "off":
                testUtils.setWifi(false);
                break;
            default:
                fail("Option of Wi-Fi: " + state + "; doesn't exist");
                break;
        }
        waitForIdle();
    }

    @When("^I click stop trusting words$")
    public void I_click_stop_trusting_words() {
        timeRequiredForThisMethod(10);
        testUtils.goToHandshakeDialog();
        waitForIdle();
        testUtils.clickNegativeButton();
        testUtils.clickAfirmativeButton();
        testUtils.clickAfirmativeButton();
        for (int i = 0; i < 500; i++) {
            waitForIdle();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @When("^I click confirm trust words$")
    public void I_click_confirm_trust_words() {
        timeRequiredForThisMethod(10);
        testUtils.goToHandshakeDialog();
        waitForIdle();
        //onView(withId(R.id.affirmativeActionButton)).check(matches(isCompletelyDisplayed()));

        testUtils.clickAfirmativeButton();
        testUtils.clickAfirmativeButton();
        testUtils.clickAfirmativeButton();
        for (int i = 0; i < 500; i++) {
            waitForIdle();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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
        email = email.replaceAll("\\s+", "");
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

    @When("^Normal use of 2 users between A and B for (\\d+) days$")
    public void _use_sync_devices(int totalDays) {
        String emailAccount = "";
        int messagesInADay = 2000;
        getBotsList();
        testUtils.readConfigFile();
        switch (testUtils.test_number()) {
            case "1":
                testUtils.getMessageListSize();
                I_wait_for_the_message_and_click_it();
                emailAccount = testUtils.getMessageBody();
                testUtils.pressBack();
                I_send_message_to_address(1, "myself", "A account", testUtils.getAccountAddress(0));
                I_go_back_to_accounts_list();
                break;
            case "2":
                I_send_message_to_address(1, "myself", "B account", testUtils.getAccountAddress(0));
                I_wait_for_the_message_and_click_it();
                emailAccount = testUtils.getMessageBody();
                testUtils.pressBack();
                I_disable_sync("B");
                break;
        }
        I_go_back_to_accounts_list();
        I_remove_account();
        testUtils.setTestNumber(0);
        testUtils.createNAccounts(1, false, false);
        testUtils.readConfigFile();
        String pepColor = "planck_yellow";
        for (int currentDay = 1; currentDay <= totalDays; currentDay++) {
            for (int currentMessage = 0; currentMessage < messagesInADay; currentMessage++) {
                testUtils.getMessageListSize();
                switch (testUtils.test_number()) {
                    case "1":
                        I_wait_for_the_message_and_click_it();
                        //I_check_toolBar_color_is(pepColor);
                        testUtils.pressBack();
                        testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), accountSelected);
                        I_remove_all_messages();
                        I_select_account("0");
                        testUtils.getMessageListSize();
                        I_send_message_to_address(1, emailAccount, "Message from A to B", "Day " + currentDay + ", message " + currentMessage);
                        break;
                    case "2":
                        I_send_message_to_address(1, emailAccount, "Message from B to A", "Day " + currentDay + ", message " + currentMessage);
                        I_wait_for_the_message_and_click_it();
                        //I_check_toolBar_color_is(pepColor);
                        testUtils.pressBack();
                        testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), accountSelected);
                        I_remove_all_messages();
                        I_select_account("0");
                        testUtils.getMessageListSize();
                        break;
                }
            }
        }
    }

    @When("^Normal use of sync for devices (\\S+) and (\\S+) for (\\d+) days$")
    public void Normal_use_sync_devices(String device1, String device2, int totalDays) {
        int minutesInADay = 1440;
        int delayTimeMinutes = 1/6;
        getBotsList();
        testUtils.readConfigFile();
        int message = 1;
        for (int currentDay = 1; currentDay <= totalDays; currentDay++) {
            for (int currentMinutes = 0; currentMinutes < minutesInADay; currentMinutes += 30) {
                I_check_1_and_2_sync(device1, device2);
                testUtils.getMessageListSize();
                switch (testUtils.test_number()) {
                    case "1":
                        I_wait_for_the_message_and_click_it();
                        //I_check_toolBar_color_is("planck_yellow");
                        testUtils.pressBack();
                        I_send_message_to_address(1, "bot" + currentDay, "DeviceA_2ndMessage", "message " + message + "from device 1 to 2, day " + currentDay);
                        while (testUtils.getListSize() > 1) {
                            testUtils.getMessageListSize();
                            waitForIdle();
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        waitForIdle();
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        testUtils.getMessageListSize();
                        break;
                    case "2":
                        I_send_message_to_address(1, "bot" + currentDay, "DeviceB_1stMessage", "message " + message + "from device 2 to 1, day " + currentDay);
                        I_wait_for_the_message_and_click_it();
                        //I_check_toolBar_color_is("planck_yellow");
                        testUtils.pressBack();
                        testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), accountSelected);
                        I_remove_all_messages();
                        I_select_account("0");
                        testUtils.getMessageListSize();
                        break;
                }
                I_go_back_to_accounts_list();
                testUtils.checkValueIsInDB("identity","flags", "256");
                testUtils.pressBack();
                testUtils.pressBack();
                I_select_account("0");
                testUtils.getMessageListSize();
                try {
                    Thread.sleep(1000 * 60 * delayTimeMinutes);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                message ++;
            }
            message = 1;
            switch (testUtils.test_number()) {
                case "1":
                    I_wait_for_the_message_and_click_it();
                    //I_check_toolBar_color_is("planck_yellow");
                    testUtils.pressBack();
                    I_wait_for_the_message_and_click_it();
                    //I_check_toolBar_color_is("planck_yellow");
                    testUtils.pressBack();
                    while (testUtils.getListSize() > 1) {
                        testUtils.getMessageListSize();
                        waitForIdle();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    testUtils.getMessageListSize();
                    I_wait_for_the_new_message();
                    break;
                case "2":
                    I_send_message_to_address(1, "bot" + currentDay, "Handshake", "Doing Handshake with bot" + currentDay);
                    I_click_the_last_message_received();
                    I_click_confirm_trust_words();
                    //I_check_toolBar_color_is("planck_green");
                    I_click_reply_message();
                    I_reset_partner_key();
                    //I_check_toolBar_color_is("planck_no_color");
                    I_discard_the_message();
                    testUtils.pressBack();
                    I_send_message_to_address(1, "bot" + currentDay, "Handshake-2nd", "Sending message after reset with bot" + currentDay);
                    I_click_the_last_message_received();
                    //I_check_toolBar_color_is("planck_yellow");
                    testUtils.pressBack();
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), accountSelected);
                    I_remove_all_messages();
                    I_select_account("0");
                    testUtils.getMessageListSize();
                    I_send_message_to_address(1, "bot" + currentDay, "Bucle"+currentDay+"Done", "Finishing first bucle");
                    break;
                default:
                    fail("Unknown Sync Device: " + testUtils.test_number());
                    break;
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @When("^I reset own key$")
    public void I_reset_own_key() {
        testUtils.selectFromMenu(R.string.action_settings);
        testUtils.selectFromScreen(testUtils.stringToID("privacy_preferences"));
        testUtils.selectFromScreen(testUtils.stringToID("reset"));
        testUtils.pressOKButtonInDialog();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        testUtils.pressBack();
        waitForIdle();
        testUtils.pressBack();
        waitForIdle();

    }
    @When("^I reset my own key$")
    public void I_reset_my_own_key() {
        switch (testUtils.test_number()) {
            case "0":
            case "1":
                I_go_back_to_accounts_list();
                testUtils.exportDB();
                String mainKeyID = testUtils.getOwnKeyFromDB("management.db","identity","user_id");
                testUtils.pressBack();
                testUtils.pressBack();
                I_select_account("0");
                testUtils.getMessageListSize();
                I_send_message_to_address(1, "bot1", "ResetKeyTest_1", "Sending message to Bot before Reset");
                I_click_the_last_message_received();
                testUtils.checkOwnKey(mainKeyID, true);
                testUtils.pressBack();
                I_go_back_to_accounts_list();
                testUtils.resetMyOwnKey();
                testUtils.pressBack();
                testUtils.exportDB();
                testUtils.pressBack();
                testUtils.pressBack();
                I_select_account("0");
                I_send_message_to_address(1, "myself", "ResetKeyDone", "Old: " + mainKeyID + " // New: " +
                        testUtils.getOwnKeyFromDB("management.db","identity","user_id"));
                I_wait_for_the_message_and_click_it();
                testUtils.checkOwnKey(mainKeyID, false);
                testUtils.pressBack();
                I_send_message_to_address(1, "bot1", "ResetKeyTest_3", "Sending message to Bot after Reset");
                if (testUtils.clickLastMessage()) {
                    fail("Cannot read New Bot's message after Reset");
                }
                testUtils.checkOwnKey(mainKeyID, false);
                break;
            case "2":
                testUtils.getMessageListSize();
                I_go_back_to_accounts_list();
                testUtils.exportDB();
                String mainKeyID2 = testUtils.getOwnKeyFromDB("management.db","identity","user_id");
                testUtils.pressBack();
                testUtils.pressBack();
                I_select_account("0");
                I_wait_for_the_message_and_click_it();
                testUtils.checkOwnKey(mainKeyID2, true);
                testUtils.pressBack();
                I_wait_for_the_new_message();
                testUtils.pressBack();
                I_go_back_to_accounts_list();
                testUtils.exportDB();
                testUtils.getOwnKeyFromDB("management.db","identity","user_id");
                testUtils.pressBack();
                testUtils.pressBack();
                I_select_account("0");
                I_send_message_to_address(1, "bot6", "KeyIsReset", "Sending message to Bot after Reset");
                I_click_the_last_message_received();
                testUtils.checkOwnKey(mainKeyID2, false);
                testUtils.pressBack();
                I_wait_for_the_new_message();
                if (testUtils.clickLastMessage()) {
                    fail("Cannot read New Bot's message after Reset");
                }
                testUtils.checkOwnKey(mainKeyID2, false);
                break;
            default:
                fail("Unknown Device for this test: " + testUtils.test_number());
        }

    }


    @When("^I sync devices (\\S+) and (\\S+)$")
    public void I_sync_devices(String device1, String device2) {
        int inboxMessages = testUtils.getListSize();
        int totalMessages = getTotalMessagesSize();
        boolean ignoreThisTest = true;
        switch (testUtils.test_number()) {
            case "1":
                if (device1.equals("A") || device2.equals("A")) {
                    ignoreThisTest = false;
                    testUtils.syncDevices();
                }
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    ignoreThisTest = false;
                    testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), 0);
                }
                break;
            case "2":
                if (device1.equals("B") || device2.equals("B")) {
                    ignoreThisTest = false;
                    testUtils.syncDevices();
                }
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    ignoreThisTest = false;
                    testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), 0);
                }
                break;
            case "3":
                if (device1.equals("C") || device2.equals("C")) {
                    ignoreThisTest = false;
                    testUtils.syncDevices();
                }
                if (exists(onView(withId(R.id.available_accounts_title)))) {
                    ignoreThisTest = false;
                    testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), 1);
                }
                break;
            default:
                Timber.i("Cannot sync this device");
                break;
        }
        testUtils.getMessageListSize();
        if (!ignoreThisTest && totalMessages >= getTotalMessagesSize()) {
            fail("There are more sync messages before sync than after sync");
        }
        if (inboxMessages != 0 && inboxMessages != testUtils.getListSize()) {
            fail("Sync messages went to wrong folder");
        }
        testUtils.getMessageListSize();
    }

    @When("^I create an account for sync on device C$")
    public void I_create_an_account_for_C() {
        switch (testUtils.test_number()) {
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
        switch (testUtils.test_number()) {
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
                fail("Unknown Sync Device to check devices are sync");
                break;
        }
    }

    @When("^I check account devices (\\S+) and (\\S+) are not protected$")
    public void I_check_1_and_2_not_protected(String firstDevice, String secondDevice) {
        testUtils.selectAccount(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), 0);
        switch (testUtils.test_number()) {
            case "1":
                testUtils.checkAccountIsNotProtected("A", firstDevice, secondDevice, syncThirdDevice);
                break;
            case "2":
                testUtils.checkAccountIsNotProtected("B", firstDevice, secondDevice, syncThirdDevice);
                break;
            case "3":
                testUtils.checkAccountIsNotProtected("C", firstDevice, secondDevice, syncThirdDevice);
                break;
            default:
                fail("Unknown Account to assert is not protected");
                break;
        }
    }

    @When("^I check devices (\\S+) and (\\S+) are not sync$")
    public void I_check_A_B_are_not_sync(String firstDevice, String secondDevice) {
        switch (testUtils.test_number()) {
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
                fail("Unknown Sync Device to check devices are not sync");
                break;
        }
    }

    @When("^I disable sync on device (\\S+)$")
    public void I_disable_sync(String device) {
        if (device.equals("C")) {
            testUtils.setTrustWords("Disabled sync on device C");
        }
        switch (testUtils.test_number()) {
            case "1":
                if (device.equals("A")) {
                    testUtils.disableKeySync();
                } else {
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
                } else {
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
                } else {
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
        switch (testUtils.test_number()) {
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
        waitForIdle();
        if (!exists(onView(withId(R.id.message_list)))) {
            testUtils.pressBack();
        }
    }

    @When("^I setup second account for devices A and B$")
    public void I_setup_second_account_for_devices_A_B() {

    }

    @When("^I reject trust words$")
    public void I_reject_trust_words() {
        timeRequiredForThisMethod(10);
        testUtils.clickStatus();
        while (!exists(onView(withId(R.id.rejectHandshake)))) {
            waitForIdle();
            waitUntilIdle();
        }
        onView(withId(R.id.rejectHandshake)).check(matches(isCompletelyDisplayed()));
        Espresso.onIdle();
        TestUtils.swipeUpScreen();
        onView(withId(R.id.rejectHandshake)).perform(click());
        waitForIdle();
        testUtils.pressBack();
        waitForIdle();
    }

    @When("^I check the privacy status is (\\S+)$")
    public void I_check_pEp_status(String status) {
        waitForIdle();
        checkPrivacyStatus(status);
        waitForIdle();
    }

    private void checkPrivacyStatus(String status) {
        waitForIdle();
        if (!status.equals("Undefined")) {
            while (!viewIsDisplayed(R.id.securityStatusIcon)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                waitForIdle();
            }
        }
        switch (status) {
            case "NotEncrypted":
                testUtils.assertStatus(Rating.pEpRatingUnencrypted);
                return;
            case "Encrypted":
                testUtils.assertStatus(Rating.pEpRatingReliable);
                return;
            case "MediaKey":
                testUtils.assertStatus(Rating.pEpRatingMediaKeyProtected);
                return;
            case "Trusted":
                testUtils.assertStatus(Rating.pEpRatingTrusted);
                return;
            case "WeaklyEncrypted":
                testUtils.assertStatus(Rating.pEpRatingUnreliable);
                return;
            case "Dangerous":
                testUtils.assertStatus(Rating.pEpRatingMistrust);
                return;
            case "CannotDecrypt":
                testUtils.assertStatus(Rating.pEpRatingCannotDecrypt);
                return;
            case "UnderAttack":
                testUtils.assertStatus(Rating.pEpRatingUnderAttack);
                return;
            case "Broken":
                testUtils.assertStatus(Rating.pEpRatingB0rken);
                return;
            case "Undefined":
                if (getTextFromView(onView(withId(R.id.to))).equals("") && !viewIsDisplayed(R.id.securityStatusIcon)) {
                    return;
                }
                fail("Rating is not Undefined");
        }
    }

    private void checkPrivacyStatus_old(String status) {
        waitForIdle();
        if (getTextFromView(onView(withId(R.id.to))).equals("") && !viewIsDisplayed(R.id.securityStatusIcon)) {
            return;
        }
        if (BuildConfig.IS_ENTERPRISE) {
            switch (status) {
                case "pEpRatingUnencrypted":
                    if (!viewIsDisplayed(onView(withId(R.id.securityStatusText)))) {
                        fail("Showing a rating that is not " + status);
                    }
                    if (!getTextFromView(onView(withId(R.id.securityStatusText))).equals(resources.getString(testUtils.stringToID("pep_rating_not_encrypted")))) {
                        fail("Showing a text that is not " + resources.getString(testUtils.stringToID("pep_rating_not_encrypted")));
                    }
                    //I_check_toolBar_color_is("planck_yellow");
                    return;
                case "pEpRatingUndefined":
                    if (getTextFromView(onView(withId(R.id.to))).equals("") && viewIsDisplayed(onView(withId(R.id.securityStatusText)))) {
                        fail("Showing a rating when there is no recipient");
                    }
                    return;
                case "pEpRatingUnsecure":
                    if (!viewIsDisplayed(onView(withId(R.id.securityStatusText)))) {
                        fail("Not showing Unsecure status");
                    }
                    if (pep_enable_privacy_protection) {
                        //I_check_toolBar_color_is("planck_red");
                    } else {
                        //I_check_toolBar_color_is("pep_gray");
                    }
                    return;
            }
        }
        Rating[] statusRating = new Rating[1];
        BySelector selector = By.clazz("android.widget.ScrollView");
        while (!viewIsDisplayed(R.id.toolbar)) {
            waitForIdle();
        }
        waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        for (int i = 0; i < 100; i++) {
            waitUntilIdle();
        }
        try {
            testUtils.typeTextToForceRatingCalculation(R.id.subject);
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
                testUtils.typeTextToForceRatingCalculation(R.id.subject);
            } catch (Exception e) {
                Timber.i("Cannot find subject");
            }
        }
        waitForIdle();
        status = testUtils.getStatusRating(statusRating, status);
        if (statusRating[0] != null) {
            testUtils.assertMessageStatus(statusRating[0], status);
        } else {

            //testUtils.checkPrivacyTextColor(testUtils.colorToID(status));
        }
    }

    @And("^I test widgets$")
    public void I_test_widget() {
        String brand = "planck";
        device.pressHome();
        BySelector selector = By.clazz("android.widget.TextView");
        BySelector horizontalScroll = By.clazz("android.widget.LinearLayout");
        int horizontalWidgetScroll = 0;
        int scroll = 0;
        int visibleCenterX = 0;
        boolean verticalLeftScroll = true;
        for (int widgetToDrag = 1; widgetToDrag < 4; widgetToDrag++) {
            waitForIdle();
            device.pressBack();
            String text;
            switch (widgetToDrag) {
                case 1:
                    text = brand + " Unread";
                    break;
                case 2:
                    text = brand + " Message List";
                    break;
                case 3:
                    text = brand + " Accounts";
                    break;
                default:
                    text = brand;
                    break;
            }
            while (!testUtils.textExistsOnScreen("Widgets")) {
                waitForIdle();
                device.drag(device.getDisplayWidth() / 2, device.getDisplayHeight() * 15 / 20,
                        device.getDisplayWidth() / 2, device.getDisplayHeight() * 15 / 20, 450);
                waitForIdle();
            }
            testUtils.selectFromScreen("Widgets");
            waitForIdle();
            if (horizontalWidgetScroll == 0) { //Horizontal scroll
                horizontalWidgetScroll = -1;
                for (UiObject2 linearLayout : device.findObjects(horizontalScroll)) {
                    if (linearLayout.getResourceName() != null) {
                        if (linearLayout.getResourceName().equals("com.sec.android.app.launcher:id/widget_page_indicator")) {
                            horizontalWidgetScroll = linearLayout.getChildCount();
                        }
                    }
                }
            }
            if (horizontalWidgetScroll == -1) { //Vertical scroll
                //device.click(5, device.getDisplayHeight() - 5);
                boolean openWidgetMenu = true;
                for (scroll = 1; scroll < 30; scroll++) {
                    for (UiObject2 textView : device.findObjects(selector)) {
                        if (openWidgetMenu && textView.getText().equals(brand)) {
                            textView.click();
                            openWidgetMenu = false;
                            break;
                        } else if (!openWidgetMenu && textView.getText().equals(text)) {
                            testUtils.dragWidget(widgetToDrag, textView.getParent().getChildren().get(0).getVisibleCenter().x, textView.getParent().getChildren().get(0).getVisibleCenter().y);
                            scroll = 30;
                            break;
                        } else if (textView.getResourceName() != null && textView.getResourceName().equals("com.android.launcher3:id/section") &&
                            textView.getVisibleBounds().left == 0){
                            verticalLeftScroll = false;
                        }
                    }

                    testUtils.verticalScreenScroll(verticalLeftScroll, device.getDisplayHeight() * scroll / 30, device.getDisplayHeight() * scroll / 10 + 3);
                }
            } else {  //Horizontal scroll
                if (scroll == 0) {
                    for (; scroll < horizontalWidgetScroll - 1; scroll++) {
                        waitForIdle();
                        device.drag(10, device.getDisplayHeight() / 2,
                                device.getDisplayWidth() - 10, device.getDisplayHeight() / 2, 10);
                        waitForIdle();
                    }
                }
                for (scroll = 0; scroll < horizontalWidgetScroll; scroll++) {
                    waitForIdle();
                    int elements = 0;
                    for (UiObject2 textView : device.findObjects(selector)) {
                        if (textView.getText() != null && textView.getText().equals(brand)) {
                            textView.click();
                            waitForIdle();
                            int widgetPreview = 0;
                            while (widgetPreview == 0) {
                                try {
                                    Rect visibleBounds;
                                    for (UiObject2 subTextView : device.findObjects(horizontalScroll)) {
                                        if (subTextView.getResourceName() != null && subTextView.getResourceName().equals("com.sec.android.app.launcher:id/add_widget_preview_background")) {
                                            widgetPreview++;
                                            visibleBounds = subTextView.getVisibleBounds();
                                            while (widgetPreview < widgetToDrag) {
                                                waitForIdle();
                                                device.swipe(visibleBounds.right + 5, visibleBounds.bottom + 5,
                                                        visibleBounds.left - 5, visibleBounds.bottom + 5, 30);
                                                waitForIdle();
                                                widgetPreview++;
                                            }
                                            if (widgetPreview == widgetToDrag) {
                                                scroll = horizontalWidgetScroll - 1;
                                                if (visibleCenterX == 0) {
                                                    visibleCenterX = subTextView.getVisibleCenter().x;
                                                }
                                                testUtils.dragWidget(widgetToDrag, visibleCenterX, visibleBounds.centerY());
                                                scroll = horizontalWidgetScroll;
                                                break;
                                            }
                                        }
                                        //if (scroll == horizontalWidgetScroll - 1) {
                                        //    break;
                                        //}
                                    }
                                } catch (Exception noWidget) {
                                    Timber.i("Cannot find Widget");
                                }
                            }
                        } else {
                            elements++;
                        }
                        if (scroll == horizontalWidgetScroll) {
                            break;
                        }
                    }
                    if (scroll == horizontalWidgetScroll - 1) {
                        scroll = horizontalWidgetScroll - 2;
                        break;
                    }
                    waitForIdle();
                    if (elements > 4) {
                        device.drag(device.getDisplayWidth() - 10, device.getDisplayHeight() / 2,
                                10, device.getDisplayHeight() / 2, 15);
                        waitForIdle();
                    } else {
                        scroll--;
                    }
                }
            }
        }
        device.pressBack();
        int widgets = 0;
        UiObject2 messagesListWidget = null;
        for (UiObject2 view : device.findObjects(selector)) {
            if (view.getText() != null) {
                if (view.getText().contains(resources.getString(testUtils.stringToID("integrated_inbox_title")))) {
                    messagesListWidget = view;
                    widgets++;
                }
            }
        }
        if (widgets != 3) {
            fail("Missing a Widget");
        }
        if (!testUtils.textExistsOnScreen("WidTest")) {
            Timber.e("Cannot find the message on the screen");
            testUtils.clearAllRecentApps();
        }
        waitForIdle();
        messagesListWidget.click();
        waitForIdle();
    }

    @And("^I select from message menu (\\S+)$")
    public void I_select_from_message_menu(String textToSelect) {
        timeRequiredForThisMethod(15);
        testUtils.selectFromMenu(testUtils.stringToID(textToSelect));
        waitForIdle();
    }

    @And("^I (\\S+) the message to the folder (\\S+)")
    public void I_refile_a_message(String action, String folder) {
        switch (action){
            case "move":
                action = "move_action";
                break;
            case "copy":
                action = "copy_action";
                break;
            default:
                fail("Cannot do the action: " + action);
        }
        switch (folder){
            case "spam":
                folder = "special_mailbox_name_spam";
                break;
            case "archive":
                folder = "special_mailbox_name_archive";
                break;
            case "trash":
                folder = "special_mailbox_name_trash";
                break;
            default:
                fail("Cannot find the folder: " + folder);
        }
        waitForIdle();
        testUtils.selectFromMenu(testUtils.stringToID(action));
        while (!testUtils.textExistsOnScreen(resources.getString(testUtils.stringToID(folder)))) {
            waitForIdle();
            swipeDownList();
            waitForIdle();
        }
        waitForIdle();
        testUtils.selectFromScreen(testUtils.stringToID(folder));
        waitForIdle();
    }

    @And("^I disable protection from privacy status menu$")
    public void I_disable_protection_from_privacy_status_menu() {
        timeRequiredForThisMethod(15);
        testUtils.selectFromMenu(testUtils.stringToID("pep_title_activity_privacy_status"));
        testUtils.selectFromMenu(testUtils.stringToID("pep_force_unprotected"));
        testUtils.pressBack();
        waitForIdle();
    }

    @Then("^I open menu$")
    public void I_select_from_menu() {
        timeRequiredForThisMethod(10);
        waitForIdle();
        testUtils.openOptionsMenu();
    }

    @Then("^I check toolBar is visible$")
    public void I_check_toolbar_is_visible() {
        timeRequiredForThisMethod(10);
        waitForIdle();
        if (viewIsDisplayed(onView(withId(R.id.delete)))) {
            testUtils.pressBack();
            waitForIdle();
            fail("Toolbar is showing Message Options when there are no messages selected");
        }
    }

    @Then("^I select from screen (\\S+)$")
    public void I_select_from_screen(String textToSelect) {
        timeRequiredForThisMethod(15);
        waitForIdle();
        testUtils.selectFromScreen(testUtils.stringToID(textToSelect));
        waitForIdle();
    }

    @Then("^I export settings$")
    public void I_export_settings() {
        waitForIdle();
        testUtils.openOptionsMenu();
        testUtils.selectFromScreen(testUtils.stringToID("import_export_action"));
        testUtils.selectFromScreen(testUtils.stringToID("settings_export_all"));
        BySelector selector;
        selector = By.clazz("android.widget.EditText");
        boolean endOfLoop = false;
        UiObject2 fileNameInTextBox = null;
        waitForIdle();
        while (!endOfLoop) {
            for (UiObject2 editText : device.findObjects(selector)) {
                if (editText.getResourceName().equals("android:id/title")) {
                    while (!editText.getText().equals("testingsettings.k9s")) {
                        editText.setText("testingsettings.k9s");
                        fileNameInTextBox = editText;
                    }
                    endOfLoop = true;
                    break;
                }
            }
        }
        waitForIdle();
        endOfLoop = false;
        UiObject2 container = fileNameInTextBox.getParent();
        while (!endOfLoop) {
            try {
                while (fileNameInTextBox.getText().equals("testingsettings.k9s")) {
                    waitForIdle();
                    container.getChildren().get(2).getChildren().get(0).click(3000);
                    waitForIdle();
                    endOfLoop = true;
                }
            } catch (Exception settingsSaved) {
                Timber.i("Saving settings");
            }
            break;
        }
        try {
            while (fileNameInTextBox.getText().equals("testingsettings.k9s")) {
                waitForIdle();
            }
        } catch (Exception ex) {
            Timber.i("Settings are exported");
        }
        waitForIdle();
        selector = By.clazz("android.widget.TextView");
        endOfLoop = false;
        while (!endOfLoop) {
            for (UiObject2 textView : device.findObjects(selector)) {
                if (textView.getResourceName().equals("android:id/alertTitle") && textView.getText().equals(resources.getString(testUtils.stringToID("settings_export_success_header")))) {
                    endOfLoop = true;
                    break;
                }
                waitForIdle();
            }
        }

        waitForIdle();
        selector = By.clazz("android.widget.Button");
        endOfLoop = false;
        while (!endOfLoop) {
            for (UiObject2 button : device.findObjects(selector)) {
                if (button.getResourceName().equals("android:id/button1")) {
                    button.click();
                    endOfLoop = true;
                    break;
                }
            }
        }
        waitForIdle();
    }

    @Then("^I import settings$")
    public void I_import_settings() {
        waitForIdle();
        testUtils.openOptionsMenu();
        testUtils.selectFromScreen(testUtils.stringToID("settings_import"));
        BySelector selector = By.clazz("android.widget.TextView");
        boolean endOfLoop = false;
        waitForIdle();
        while (!endOfLoop) {
            for (UiObject2 textView : device.findObjects(selector)) {
                try {
                    if (textView != null && textView.getText().contains("testingsettings")) {
                        textView.click();
                        waitForIdle();
                        endOfLoop = true;
                        break;
                    }
                } catch (Exception nullView) {
                    Timber.i("TextView is null");
                }
            }
        }
        testUtils.waitForUiObject2(resources.getString(testUtils.stringToID("settings_import_selection")), "android:id/alertTitle", selector);
        testUtils.pressOKButtonInDialog();
        testUtils.waitForUiObject2(resources.getString(testUtils.stringToID("settings_import_success_header")), "android:id/alertTitle", By.clazz("android.widget.TextView"));
        testUtils.pressOKButtonInDialog();
        testUtils.waitForUiObject2(resources.getString(testUtils.stringToID("settings_import_activate_account_header")), "android:id/alertTitle", By.clazz("android.widget.TextView"));
        selector = By.clazz("android.widget.EditText");
        endOfLoop = false;
        while (!endOfLoop) {
            for (UiObject2 editText : device.findObjects(selector)) {
                try {
                    if (editText.getResourceName().equals(BuildConfig.APPLICATION_ID+":id/incoming_server_password")) {
                        editText.setText(testUtils.getAccountPassword());
                        endOfLoop = true;
                        break;
                    }
                } catch (Exception noPassword) {
                    Timber.i("No account password");
                }
            }
        }
        waitForIdle();
        testUtils.pressOKButtonInDialog();
        waitForIdle();
    }

    @Then("^I check Global settings$")
    public void I_check_global_settings() {
        testUtils.assertGloblaSettings();
    }

    @Then("^I change Global settings$")
    public void I_change_global_settings() {
        testUtils.changeGlobalSettings();
    }

    @Then("^I check Account settings$")
    public void I_check_account_settings() {
        testUtils.assertAccountSettings();
    }

    @Then("^I change Account settings$")
    public void I_change_account_settings() {
        testUtils.changeAccountSettings();
    }

    @Then("^I remove account$")
    public void I_remove_account() {
        timeRequiredForThisMethod(25);
        testUtils.goBackAndRemoveAccount();
    }

    @Then("^I remove unsecure email addresses$")
    public void I_remove_unsecure_email_addresses() {
        waitForIdle();
        onView(withId(R.id.user_action_banner)).perform(click());
        waitForIdle();
    }

    @Then("^I remove email address$")
    public void I_remove_email_address() {
        timeRequiredForThisMethod(20);
        waitForIdle();
        device.pressKeyCode(KeyEvent.KEYCODE_DEL);
        waitForIdle();
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
        waitForIdle();
        Set_external_mock(file);
        testUtils.attachFile(fileName);
        waitForIdle();
        testUtils.testReset = true;
    }

    @Given("^Set external mock (\\S+)$")
    public void Set_external_mock(String mock) {
        timeRequiredForThisMethod(10);
        int raw = 0;
        switch (mock) {
            case "settings":
                raw = R.raw.settingsthemedark;
                waitForIdle();
                fileName = "settings.k9s";
                break;
            case "settingsthemedark":
                raw = R.raw.settingsthemedark;
                waitForIdle();
                fileName = "settingsthemedark.k9s";
                break;
            case "MSoffice":
                raw = R.raw.testmsoffice;
                waitForIdle();
                fileName = "testmsoffice.docx";
                break;
            case "PDF":
                raw = R.raw.testpdf;
                waitForIdle();
                fileName = "testpdf.pdf";
                break;
            case "masterKey":
                raw = R.raw.masterkeypro;
                waitForIdle();
                fileName = "masterkey.asc";
                break;
            case "picture":
                raw = R.raw.testpicture;
                waitForIdle();
                fileName = "testpicture.png";
                break;
            case "specialCharacters":
                raw = R.raw.testmsoffice;
                waitForIdle();
                fileName = "aµØßàåæçñяΣオ可.pdf";
                break;
            case "calendarEvent":
                raw = R.raw.calendar_invite;
                waitForIdle();
                fileName = "calendar.ics";
                break;
            case "passphrase":
                switch (testUtils.test_number()) {
                    case "4":
                        raw = R.raw.passphrase1;
                        break;
                    case "5":
                        raw = R.raw.passphrase2;
                        break;
                    case "6":
                        raw = R.raw.passphrase3;
                        break;
                }
                waitForIdle();
                fileName = "passphrase.asc";
                break;
            case "largeFile":
                raw = R.raw.testlargefile;
                waitForIdle();
                fileName = "largeFile.mp4";
                break;
        }
        while (true) {
            try {
                waitForIdle();
                TestUtils.createFile(fileName, raw);
                waitForIdle();
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
        waitForIdle();
    }

    @Then("^I click the send message button$")
    public void I_click_the_send_message_button() {
        timeRequiredForThisMethod(5);
        while (exists(onView(withId(R.id.send)))) {
            testUtils.clickView(R.id.send);
        }
    }

    @When("^I click compose message")
    public void I_click_message_compose_button() {
        //timeRequiredForThisMethod(5);
        testUtils.composeMessageButton();
    }

    @When("^I run the tests")
    public void I_run_the_tests() {
        startTest(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), 0);
    }

    @When("^I check there are (\\d+) global settings and (\\d+) account settings")
    public void I_check_there_are_less_settings(int totalGlobalSettings, int totalAccountSettings) {
        int size;
        testUtils.selectFromMenu(R.string.action_settings);
        size = testUtils.getListSize(R.id.recycler_view);
        if (size != totalGlobalSettings) {
            fail("There are " + size + " elements in global settings and should be " + totalGlobalSettings);
        }
        testUtils.selectAccountSettingsFromList(0);
        size = testUtils.getListSize(R.id.recycler_view);
        testUtils.pressBack();
        if (size != totalAccountSettings) {
            fail("There are " + size + " elements in account settings and should be " + totalAccountSettings);
        }
    }

    @When("^I test Unified Inbox (\\d+) times")
    public void I_test_unified_inbox(int times) {
        I_send_message_to_address(4, "bot1", "Message for Testing Unified Inbox", "Body of the message");
        for (int i = 0; i < times; i++) {
            testUtils.openHamburgerMenu();
            testUtils.selectFromScreen(R.string.integrated_inbox_title);
            testUtils.clickMessageAtPosition(1);
            waitForIdle();
            testUtils.goBackToMessageList();
            testUtils.openHamburgerMenu();
            testUtils.selectFromScreen(R.string.special_mailbox_name_inbox);
            testUtils.composeMessageButton();
            testUtils.goBackAndSaveAsDraft();
            testUtils.openHamburgerMenu();
            testUtils.selectFromScreen(R.string.integrated_inbox_title);
            testUtils.composeMessageButton();
            testUtils.goBackAndSaveAsDraft();
            testUtils.clickMessageAtPosition(2);
            waitForIdle();
            testUtils.goBackToMessageList();
            testUtils.openHamburgerMenu();
            testUtils.selectFromScreen(R.string.special_mailbox_name_outbox);
            testUtils.openHamburgerMenu();
            testUtils.selectFromScreen(R.string.integrated_inbox_title);
            testUtils.clickMessageAtPosition(3);
            waitForIdle();
            testUtils.goBackToMessageList();
            testUtils.openHamburgerMenu();
            testUtils.selectFromScreen(R.string.special_mailbox_name_inbox);
            testUtils.clickMessageAtPosition(1);
            waitForIdle();
            testUtils.goBackToMessageList();
            testUtils.composeMessageButton();
            testUtils.pressBack();
        }

    }


    @When("^I go to (\\S+) folder from navigation menu")
    public void I_go_to_folder_from_navigation_menu(String folder) {
        while (!getTextFromView(onView(withId(R.id.actionbar_title_first))).toLowerCase().contains(folder.toLowerCase())) {
            int folderID = 0;
            switch (folder) {
                case "inbox":
                case "Inbox":
                    folderID = R.string.special_mailbox_name_inbox;
                    break;
                case "drafts":
                case "Drafts":
                    folderID = R.string.special_mailbox_name_drafts;
                    break;
                case "sent":
                case "Sent":
                    folderID = R.string.special_mailbox_name_sent;
                    break;
                case "outbox":
                case "Outbox":
                    folderID = R.string.special_mailbox_name_outbox;
                    break;
                case "spam":
                case "Spam":
                    folderID = R.string.special_mailbox_name_spam;
                    break;
                case "trash":
                case "Trash":
                    folderID = R.string.special_mailbox_name_trash;
                    break;
                case "archive":
                case "Archive":
                    folderID = R.string.special_mailbox_name_archive;
                    break;

            }
            testUtils.openHamburgerMenu();
            if (folder.equals("Suspicious") || folder.equals("suspicious")) {
                testUtils.scrollUpNavigation();
                testUtils.selectFromScreen("Suspicious");
            } else {
                testUtils.selectFromScreen(folderID);
            }
        }
    }


    @When("^I select account (\\S+)$")
    public void I_select_account(String account) {
         testUtils.rotateDevice();
        accountSelected = Integer.parseInt(account);
        while (testUtils.getTotalAccounts() == -1) {
            testUtils.readConfigFile();
        }
        if (!(accountSelected < testUtils.getTotalAccounts())) {
            skipTest("No more accounts");
        }
        startTest(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")), accountSelected);
        testUtils.rotateDevice();
    }


    @When("^I select (\\S+) folder from hamburger menu$")
    public void I_select_folder_from_hamburger_menu(String folder) {
        int folderID = 0;
        testUtils.openHamburgerMenu();
        switch (folder){
            case "Inbox":
                folderID = R.string.special_mailbox_name_inbox;
                break;
            case "Outbox":
                folderID = R.string.special_mailbox_name_outbox;
                break;
            case "Drafts":
                folderID = R.string.special_mailbox_name_drafts;
                break;
            case "Trash":
                folderID = R.string.special_mailbox_name_trash;
                break;
            case "Sent":
                folderID = R.string.special_mailbox_name_sent;
                break;
            case "Archive":
                folderID = R.string.special_mailbox_name_archive;
                break;
            case "Spam":
                folderID = R.string.special_mailbox_name_spam;
                break;
            default:
                fail("Folder " + folder + " doesn't exist");
        }
        testUtils.selectFromScreen(folderID);
    }
        @When("^I select (\\S+) folder of account (\\S+)$")
        public void I_select_folder_account(String folder, String account) {
        accountSelected = Integer.parseInt(account);
        while (testUtils.getTotalAccounts() == -1) {
            testUtils.readConfigFile();
        }
        if (!(accountSelected < testUtils.getTotalAccounts())) {
            skipTest("No more accounts");
        }
        startTest(folder, accountSelected);
    }

    @When("^I (\\S+) protection on device (\\S+)$")
    public void I_disable_protection(String protection, String device) {
        switch (testUtils.test_number()) {
            case "1":
                if (device.equals("A")) {
                    testUtils.modifyProtection(0);
                }
                break;
            case "2":
                if (device.equals("B")) {
                    testUtils.modifyProtection(0);
                }
                break;
            case "3":
                if (device.equals("C")) {
                    testUtils.modifyProtection(0);
                }
                break;
            default:
                Timber.i("Cannot " + protection + " protection on: " + device);
                break;
        }
    }

    @When("^I import key with passphrase for account (\\d+)$")
    public void I_import_passphrase(int account) {
        testUtils.readConfigFile();
        if (!exists(onView(withId(R.id.available_accounts_title)))) {
            testUtils.selectFromMenu(R.string.action_settings);
        }
        testUtils.selectAccountSettingsFromList(account);
        testUtils.selectFromScreen(testUtils.stringToID("privacy_preferences"));
        timeRequiredForThisMethod(15);
        waitForIdle();
        Set_external_mock("passphrase");
        waitForIdle();
        testUtils.testReset = true;
        testUtils.selectFromScreen(testUtils.stringToID("pgp_key_import_title"));
        fingerprint = testUtils.getFingerprint();
        testUtils.selectButtonFromScreen(testUtils.stringToID("pgp_key_import_confirmation_confirm"));
        while (!exists(onView(withId(R.id.passphrase))) && !exists(onView(withId(android.R.id.button1)))) {
            waitForIdle();
        }
        waitForIdle();
        while (!getTextFromView(onView(withId(R.id.passphrase))).contains(testUtils.getPassphrasePassword())) {
            waitForIdle();
            onView(withId(R.id.passphrase)).perform(typeText(testUtils.getPassphrasePassword()));
        }
        waitForIdle();
        testUtils.clickView(R.id.afirmativeActionButton);
        testUtils.waitForKeyImport();
        testUtils.clickView(android.R.id.button1);
        testUtils.pressBack();
        testUtils.pressBack();
    }

    @When("^I compare fingerprint$")
    public void I_compare_fingerprint() {
        testUtils.openOptionsMenu();
        testUtils.selectFromMenu(R.string.show_headers_action);
        testUtils.scrollUpToView(R.id.from);
        testUtils.assertsTextExistsOnScreen(fingerprint);
        testUtils.goBackToMessageList();
        testUtils.disableKeySync();
    }

    @When("^I remove account (\\S+)$")
    public void I_remove_account(String account) {
        int accountToRemove = Integer.parseInt(account);
        if (!exists(onView(withId(R.id.accounts_list)))) {
            while (true) {
                try {
                    waitForIdle();
                    openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
                    waitForIdle();
                } catch (Exception ex) {
                    Timber.i("Cannot open menu");
                    break;
                }
            }
            testUtils.selectFromMenu(R.string.action_settings);
        }
        waitForIdle();
        while (true) {
            try {
                waitForIdle();
                if (exists(onView(withId(R.id.accounts_list)))) {
                    while (!viewIsDisplayed(R.id.accounts_list)) {
                        waitForIdle();
                    }
                    onView(withId(R.id.accounts_list)).check(matches(isCompletelyDisplayed()));
                    while (exists(onView(withId(R.id.accounts_list)))) {
                        waitForIdle();
                        onData(anything()).inAdapterView(withId(R.id.accounts_list)).atPosition(accountToRemove).perform(longClick());
                        waitForIdle();
                        BySelector selector = By.clazz("android.widget.TextView");
                        for (UiObject2 object : device.findObjects(selector)) {
                            if (object.getText().equals(resources.getString(R.string.remove_account_action))) {
                                object.click();
                                waitForIdle();
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

    private void skipTest(String text) {
        throw new cucumber.api.PendingException(text);
    }

    public void startTest(String folder, int accountToStart) {
        getBotsList();
        if (!BuildConfig.IS_ENTERPRISE) {
            testUtils.selectAccount(folder, accountToStart);
        }
    }

    private void getBotsList(){
        try {
            if (bot[0] != null) {
                return;
            }
        } catch (Exception e) {
            Timber.i("bot list doesn't exist");
        }
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
    }

    @And("^I click to load more messages$")
    public void I_click_load_messages() {
        timeRequiredForThisMethod(10);
        waitForIdle();
        while (testUtils.getListSize() < 101) {
            testUtils.getMessageListSize();
            waitForIdle();
        }
        testUtils.scrollDownToView(R.id.main_text);
        testUtils.clickView(R.id.main_text);
        waitForIdle();
    }

    @And("^I assert there are more messages$")
    public void I_assert_more_messages() {
        waitForIdle();
        waitForIdle();
        testUtils.getMessageListSize();
        if (testUtils.getListSize() <= 101) {
            fail("Is not loading more messages");
        }
        waitForIdle();
    }

    @And("^I click view (\\S+)$")
    public void I_click_view(String viewClicked) {
        timeRequiredForThisMethod(10);
        waitForIdle();
        testUtils.clickView(testUtils.intToID(viewClicked));
        waitForIdle();
    }

    @And("^I test the format and it is showing the pictures$")
    public void I_test_format_and_pictures() {
        timeRequiredForThisMethod(10);
        switch (testUtils.test_number()) {
            case "7":
                testUtils.clickLastMessage();
                break;
            case "8":
                testUtils.waitForNMessageInTheLIst(3);
                testUtils.clickMessageAtPosition(3);
                break;
            case "9":
                testUtils.waitForNMessageInTheLIst(5);
                testUtils.clickMessageAtPosition(5);
                break;
            default:
                break;
        }
        //testUtils.selectFromMenu(R.string.single_message_options_action);
        testUtils.clickTextOnScreen(R.string.compose_title_forward);
        I_enter_text_in_field(testUtils.getFormatAccount(), "messageTo");
        //I_fill_subject_field("New");
        I_click_the_send_message_button();
        testUtils.goBackToMessageList();
        //I_wait_for_the_new_message();
        testUtils.clickLastMessage();
        //I_click_reply_message();
        waitForIdle();
        TestUtils.swipeUpScreen();
        onView(withId(R.id.message_content)).perform(click());
        int[] firstLetterCentralThickness = new int[2];
        int[] firstLetterTopThickness = new int[2];
        int textBoxHeight = 0;
        int firstLetterTopYPixel;
        BySelector selectorA = By.clazz("android.view.View");
        for (UiObject2 viewView : device.findObjects(selectorA)) {
            if (viewView.getText() != null && viewView.getText().contains("Testing Blue")) {
                firstLetterCentralThickness[0] = testUtils.getNextHorizontalColoredXPixelToTheRight(viewView.getVisibleBounds().left, viewView.getVisibleBounds().centerY());
                firstLetterCentralThickness[1] = testUtils.getNextHorizontalWhiteXPixelToTheRight(firstLetterCentralThickness[0], viewView.getVisibleBounds().centerY());
                firstLetterTopYPixel = testUtils.getNextVerticalWhiteYPixelToTheTop(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().centerY()) + 1;
                firstLetterTopThickness[0] = testUtils.getNextHorizontalWhiteXPixelToTheLeft(firstLetterCentralThickness[0] + 1, firstLetterTopYPixel);
                firstLetterTopThickness[1] = testUtils.getNextHorizontalWhiteXPixelToTheRight(firstLetterCentralThickness[0] + 1, firstLetterTopYPixel);
                textBoxHeight = viewView.getVisibleBounds().bottom - viewView.getVisibleBounds().top;
                break;
            }
        }
        for (UiObject2 viewView : device.findObjects(selectorA)) {
            if (viewView.getText() != null && viewView.getText().contains("Testing")) {
                switch (viewView.getText()) {
                    case "Testing Header":
                        if (viewView.getVisibleBounds().bottom - viewView.getVisibleBounds().top <= textBoxHeight + 2) {
                            fail(viewView.getText() + " is not Header");
                        }
                        break;
                    case "Testing Blue":
                        if (Color.valueOf(testUtils.getPixelColor(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().centerY())).blue() != 1.0
                                || Color.valueOf(testUtils.getPixelColor(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().centerY())).red() != 0.0
                                || Color.valueOf(testUtils.getPixelColor(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().centerY())).green() != 0.0) {
                            fail(viewView.getText() + " is not BLUE");
                        }
                        break;
                    case "Testing Red":
                        if (Color.valueOf(testUtils.getPixelColor(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().centerY())).red() != 1.0
                                || Color.valueOf(testUtils.getPixelColor(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().centerY())).blue() != 0.0
                                || Color.valueOf(testUtils.getPixelColor(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().centerY())).green() != 0.0) {
                            fail(viewView.getText() + " is not RED");
                        }
                        break;
                    case "Testing Italic\n":
                        int italicCentralXStart = testUtils.getNextHorizontalColoredXPixelToTheRight(viewView.getVisibleBounds().left, viewView.getVisibleBounds().centerY());
                        int italicCentralXEnd = testUtils.getNextHorizontalWhiteXPixelToTheRight(italicCentralXStart, viewView.getVisibleBounds().centerY());
                        if ((firstLetterCentralThickness[1] - firstLetterCentralThickness[0] + 1 < italicCentralXEnd - italicCentralXStart)
                                || (firstLetterCentralThickness[1] - firstLetterCentralThickness[0] - 1 > italicCentralXEnd - italicCentralXStart)) {
                            fail(viewView.getText() + " is not ITALIC");
                        }
                        firstLetterTopYPixel = testUtils.getNextVerticalColoredYPixelToTheBottom(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().top + 1);
                        int italicTopStart = testUtils.getNextHorizontalWhiteXPixelToTheLeft(firstLetterCentralThickness[0], firstLetterTopYPixel + 1);
                        int italicTopEnd = testUtils.getNextHorizontalWhiteXPixelToTheRight(firstLetterCentralThickness[0], firstLetterTopYPixel + 1);
                        if (firstLetterTopThickness[0] == italicTopStart || firstLetterTopThickness[1] == italicTopEnd) {
                            fail(viewView.getText() + " is not ITALIC");
                        }
                        if ((firstLetterTopThickness[1] - firstLetterTopThickness[0] - 2 > italicTopEnd - italicTopStart) || (firstLetterTopThickness[1] - firstLetterTopThickness[0] + 2 < italicTopEnd - italicTopStart)) {
                            fail(viewView.getText() + " is not the same size");
                        }
                        break;
                    case "Testing Bold\n":
                        int boldCentralXStart = testUtils.getNextHorizontalColoredXPixelToTheRight(viewView.getVisibleBounds().left, viewView.getVisibleBounds().centerY());
                        int boldCentralXEnd = testUtils.getNextHorizontalWhiteXPixelToTheRight(boldCentralXStart, viewView.getVisibleBounds().centerY());
                        if (firstLetterCentralThickness[1] - firstLetterCentralThickness[0] >= boldCentralXEnd - boldCentralXStart) {
                            fail(viewView.getText() + " is not BOLD");
                        }
                        break;
                    case "Testing Underline":
                        int underlineFirstLetterXStart = testUtils.getNextHorizontalColoredXPixelToTheRight(viewView.getVisibleBounds().left, viewView.getVisibleBounds().centerY());
                        int underlineFirstLetterXEnd = testUtils.getNextHorizontalWhiteXPixelToTheRight(underlineFirstLetterXStart, viewView.getVisibleBounds().centerY());
                        if (firstLetterCentralThickness[1] != underlineFirstLetterXEnd || firstLetterCentralThickness[0] != underlineFirstLetterXStart) {
                            fail(viewView.getText() + " is not the same size");
                        }
                        int bottomUnderline = testUtils.getNextVerticalColoredYPixelToTheTop(firstLetterCentralThickness[0] + 1, viewView.getVisibleBounds().bottom) - 1;
                        int underlineYStart = testUtils.getNextHorizontalWhiteXPixelToTheLeft(firstLetterCentralThickness[0] + 1, bottomUnderline);
                        int underlineYEnd = testUtils.getNextHorizontalWhiteXPixelToTheRight(firstLetterCentralThickness[0] + 1, bottomUnderline);
                        if (underlineYEnd - underlineYStart < 30) {
                            fail(viewView.getText() + " is not Underlined");
                        }
                        break;
                    case "Testing No Format":
                        if (viewView.getVisibleBounds().bottom - viewView.getVisibleBounds().top >= textBoxHeight - 2) {
                            fail(viewView.getText() + " has format");
                        }
                        break;
                }
            }
        }
        List<UiObject2> images = device.findObjects(By.clazz("android.widget.Image"));
        int pic0 = testUtils.getPixelColor(images.get(0).getVisibleBounds().centerX(),
                images.get(0).getVisibleBounds().centerY());
        int pic1 = testUtils.getPixelColor(images.get(1).getVisibleBounds().centerX(),
                images.get(1).getVisibleBounds().centerY());
        int pic2 = testUtils.getPixelColor(images.get(2).getVisibleBounds().centerX(),
                images.get(2).getVisibleBounds().centerY());
        testUtils.pressShowPicturesButton();
        waitForIdle();
        testUtils.goBackToMessageList();
        testUtils.clickLastMessage();
        waitForIdle();
        TestUtils.swipeUpScreen();
        waitForIdle();
        images = device.findObjects(By.clazz("android.widget.Image"));
        int newPic0 = testUtils.getPixelColor(images.get(0).getVisibleBounds().centerX(),
                images.get(0).getVisibleBounds().centerY());
        int newPic1 = testUtils.getPixelColor(images.get(1).getVisibleBounds().centerX(),
                images.get(1).getVisibleBounds().centerY());
        int newPic2 = testUtils.getPixelColor(images.get(2).getVisibleBounds().centerX(),
                images.get(2).getVisibleBounds().centerY());
        if (pic0 == newPic0 || pic1 == newPic1 || pic2 == newPic2) {
            fail("Cannot show images or were shown before");
        }
        testUtils.pressBack();
        switch (testUtils.test_number()) {
            case "7":
                testUtils.pressBack();
                testUtils.clickFolder(resources.getString(R.string.special_mailbox_name_inbox));
                I_send_message_to_address(1, "bot1", "Format_Test", "First test finished");
                break;
            case "8":
                testUtils.pressBack();
                testUtils.clickFolder(resources.getString(R.string.special_mailbox_name_inbox));
                I_send_message_to_address(1, "bot1", "Format_Test", "Second test finished");
                break;
            case "9":
                waitForIdle();
                testUtils.pressBack();
                waitForIdle();
                testUtils.clickFolder(resources.getString(R.string.special_mailbox_name_inbox));
                waitForIdle();
                testUtils.clickMessageAtPosition(2);
                waitForIdle();
                for (int messageToRemove = 0; messageToRemove < 4; messageToRemove++) {
                    waitForIdle();
                    testUtils.clickView(R.id.delete);
                    waitForIdle();
                }
                testUtils.clickLastMessage();
                waitForIdle();
                testUtils.clickView(R.id.delete);
                break;
            default:
                break;
        }
        testUtils.goBackToMessageList();
        waitForIdle();
    }

    @And("^I search for (\\d+) (?:message|messages) with text (\\S+)$")
    public void I_click_search_and_search_for_text(int messages, String text) {
        timeRequiredForThisMethod(25);
        testUtils.goBackToMessageList();
        int[] messageListSize = new int[1];
        waitForIdle();
        while (!exists(onView(withId(R.id.search)))) {
            waitForIdle();
            testUtils.pressBack();
        }
        testUtils.clickSearch();
        if (viewIsDisplayed(R.id.fab_button_compose_message)) {
            fail("Compose message button is shown");
        }
        if (exists(onView(withId(R.id.search_clear)))) {
            try {
                onView(withId(R.id.search_clear)).perform(click());
                waitForIdle();
                onView(withId(R.id.search)).perform(click());
                waitForIdle();
            } catch (Exception e) {
                Timber.i("Cannot clear text in search box");
            }
        }
        waitForIdle();
        onView(withId(R.id.search_input)).perform(typeText(text));
        waitForIdle();
        onView(withId(R.id.search_input)).perform(pressImeActionButton(), closeSoftKeyboard());
        waitForIdle();
        try {
            onView(withId(R.id.message_list)).perform(saveSizeInInt(messageListSize, 0));
        } catch (Exception list) {
            Timber.i("Message list is empty");
            if (messageListSize[0] == 0) {
                messageListSize[0] = 1;
            }
        }
        if (messageListSize[0] - 1 != messages) {
            fail("There are not " + messages + " messages in the list. There are: " + (messageListSize[0] - 1));
        }
        while (getTextFromView(onView(withId(R.id.actionbar_title_first))).equals(resources.getString(R.string.search_results))) {
            testUtils.pressBack();
            waitForIdle();
        }
    }

    @And("^I click search button$")
    public void I_click_search_button() {
        testUtils.clickSearch();
    }

    @And("^I click reply message$")
    public void I_click_reply_message() {
        timeRequiredForThisMethod(10);
        waitForIdle();
        while (!viewIsDisplayed(R.id.openCloseButton)) {
            waitForIdle();
        }
        onView(withId(R.id.openCloseButton)).check(matches(isDisplayed()));
        testUtils.clickView(testUtils.intToID("openCloseButton"));
        waitForIdle();
        waitForIdle();
        while (!viewIsDisplayed(R.id.message_content)) {
            waitForIdle();
        }
        testUtils.typeTextToForceRatingCalculation(R.id.message_content);
    }

    @Then("^I send (\\d+) (?:message|messages) to (\\S+) with subject (\\S+) and body (\\S+)$")
    public void I_send_message_to_address(int totalMessages, String botName, String subject, String body) {
        String messageTo = "nothing";
        getBotsList();
        switch (botName) {
            case "myself":
                switch (testUtils.test_number()) {
                    case "0":
                        messageTo = testUtils.getAccountAddress(accountSelected);
                        break;
                    case "1":
                    case "2":
                    case "3":
                        messageTo = testUtils.getKeySyncAccount(0);
                        break;
                    case "4":
                    case "5":
                    case "6":
                        messageTo = testUtils.getPassphraseAccount();
                        break;
                    default:
                        fail("Unknown Device for this test: " + testUtils.test_number());
                }
                break;
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
            waitForIdle();
            if (exists(onView(withId(R.id.fab_button_compose_message)))) {
                testUtils.composeMessageButton();
            }
            if (exists(onView(withId(R.id.message_list)))) {
                testUtils.getListSize();
            }
            waitForIdle();
            testUtils.fillMessage(new TestUtils.BasicMessage("", subject, body, messageTo), false);
            waitForIdle();
            testUtils.sendMessage();
            waitForIdle();
            while (!viewIsDisplayed(R.id.message_list)) {
                testUtils.pressBack();
                waitForIdle();
            }
            testUtils.waitForNewMessage();
        }
        waitForIdle();
    }

    private void fillMessage(String to) {
        testUtils.fillMessage(new TestUtils.BasicMessage("", " ", " ", to), false);

    }

    @Then("^I send and remove (\\d+) messages to (\\S+) with subject (\\S+) and body (\\S+)$")
    public void I_send_and_remove_N_messages(int totalMessages, String botName, String subject, String body) {
        for (int i = 0; i < totalMessages; i++) {
            testUtils.getMessageListSize();
            I_send_message_to_address(1, botName, subject, body + ". Message to remove " + (i + 1) + " of " + totalMessages);
            testUtils.clickLastMessage();
            testUtils.clickView(R.id.delete);
            testUtils.goBackToMessageList();
        }
    }

    @Then("^I remove all messages$")
    public void I_remove_all_messages() {
        testUtils.getMessageListSize();
        if (testUtils.getListSize() > 1) {
            testUtils.clickLastMessage();
        } else {
            return;
        }
        while (!viewIsDisplayed(R.id.fab_button_compose_message)) {
            try {
                testUtils.clickView(R.id.delete);
            } catch (Exception ex) {
                Timber.i("There are no more messages to remove");
            }
            waitForIdle();
        }
        if (!BuildConfig.IS_ENTERPRISE) {
            while (!exists(onView(withId(R.id.available_accounts_title)))) {
                testUtils.pressBack();
                waitForIdle();
            }
        }
        testUtils.getMessageListSize();
    }

    @Then("^I wait for the new message$")
    public void I_wait_for_the_new_message() {
        timeRequiredForThisMethod(40);
        testUtils.waitForNewMessage();
    }

    @Then("^I test Stability for account (\\S+)$")
    public void I_test_Stability(String account) {
        timeRequiredForThisMethod(40);
        I_send_message_to_address(4, "bot1", "Message for Testing Unified Inbox", "Body of the message");
        for (int i = 0; i < 500; i++) {
            I_select_account(account);
            I_wait_seconds(5);
            I_send_and_remove_N_messages(3, "bot1", "stability", "TestingStability of message " + i);
            I_go_back_to_the_Inbox();
            testUtils.getMessageListSize();
            I_wait_seconds(5);
            I_test_unified_inbox(1);
            I_wait_seconds(5);
            I_go_back_to_accounts_list();
            //I_walk_through_app();
            I_wait_seconds(5);
            testUtils.pressBack();
        }

    }

    @Then("^I check the badge color of the first message is (\\S+)$")
    public void I_check_badge_color(String status) {
        timeRequiredForThisMethod(40);
        testUtils.checkBadgeStatus(status, 1);
    }

    @Then("^I check the badge color of the message (\\d+) is (\\S+)$")
    public void I_check_badge_color_of_message_x(int message, String status) {
        timeRequiredForThisMethod(40);
        testUtils.checkBadgeStatus(status, message);
    }

    @And("^I go to the sent folder$")
    public void I_go_to_the_sent_folder() {
        timeRequiredForThisMethod(25);
        waitForIdle();
        testUtils.goBackToMessageList();
        testUtils.goToSentFolder();
    }

    @And("^I select the inbox from the menu$")
    public void I_select_the_inbox_from_the_menu() {
        timeRequiredForThisMethod(25);
        waitForIdle();
        testUtils.openHamburgerMenu();
        waitForIdle();
        testUtils.clickFolder(resources.getString(testUtils.stringToID("special_mailbox_name_inbox")));
    }


    @And("^I select the inbox$")
    public void I_select_the_inbox() {
        timeRequiredForThisMethod(25);
        waitForIdle();
        testUtils.goToInboxFolder();
    }

    @And("^I enable passive mode$")
    public void I_enable_passive_mode() {
        timeRequiredForThisMethod(25);
        waitForIdle();
        testUtils.selectFromScreen(testUtils.stringToID("privacy_preferences"));
        waitForIdle();
        testUtils.checkBoxOnScreenChecked(testUtils.stringToID("pep_passive_mode"), true);
        waitForIdle();
        testUtils.pressBack();
        waitForIdle();
    }

    @And("^I disable passive mode$")
    public void I_disable_passive_mode() {
        timeRequiredForThisMethod(25);
        waitForIdle();
        testUtils.selectFromScreen(testUtils.stringToID("privacy_preferences"));
        waitForIdle();
        testUtils.checkBoxOnScreenChecked(testUtils.stringToID("pep_passive_mode"), false);
        waitForIdle();
        testUtils.pressBack();
        waitForIdle();
    }

    @And("^I go back to accounts list$")
    public void I_go_back_to_accounts_list() {
        timeRequiredForThisMethod(25);
        waitForIdle();
        if (!exists(onView(withId(R.id.available_accounts_title)))) {
            testUtils.selectFromMenu(R.string.action_settings);
        }
        waitForIdle();
    }

    @And("^I go to the drafts folder$")
    public void I_go_to_the_drafts_folder() {
        timeRequiredForThisMethod(25);
        waitForIdle();
        testUtils.goBackToMessageList();
        testUtils.goToDraftsFolder();
    }

    @And("^I click the first message$")
    public void I_click_the_first_message() {
        timeRequiredForThisMethod(12);
        testUtils.clickFirstMessage();
    }

    @Given("^I create and remove (\\d+) accounts$")
    public void I_create_and_remove_accounts(int total) {
        for (int account = 0; account < total; account++) {
            timeRequiredForThisMethod(100);
            testUtils.createAccount();
            testUtils.goBackAndRemoveAccount();
            waitForIdle();
        }
    }

    @Given("^I test (\\d+) threads with address (\\S+)$")
    public void I_summon_threads(int total, String address) {
        try {
            for (int i = 0; i < total; i++) {
                I_enter_text_in_field(address, "messageTo");
                testUtils.summonThreads();
                I_enter_text_in_field("empty", "messageTo");
                testUtils.summonThreads();
            }
        } catch (Exception ex) {
            Timber.e("Error summoning");
        }
    }

    @Then("^I discard the message$")
    public void I_discard_the_message() {
        timeRequiredForThisMethod(10);
        waitForIdle();
        testUtils.pressBack();
        testUtils.doWaitForObject("android.widget.Button");
        waitForIdle();
        while (!testUtils.textExistsOnScreen(resources.getString(testUtils.stringToID("save_or_discard_draft_message_instructions_fmt")))) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            waitForIdle();
        }
        onView(withText(R.string.discard_action)).perform(click());
    }

    @Given("^I press back$")
    public void I_press_back() {
        timeRequiredForThisMethod(2);
        waitForIdle();
        testUtils.pressBack();
        waitForIdle();
    }

    @And("^I go back to app$")
    public void I_go_back_to_app() {
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
    public void I_check_status_text_is(String status) {
        timeRequiredForThisMethod(10);
        try {
            TestUtils.swipeUpScreen();
            TestUtils.swipeDownScreen();
            TestUtils.swipeDownScreen();
            testUtils.typeTextToForceRatingCalculation(R.id.subject);
        } catch (Exception ex) {
            Timber.i("Cannot find subject field");
        }
        while (!exists(onView(withId(R.id.toolbar_container)))) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 0; i < 500; i++) {
            waitForIdle();
        }
        onView(withId(R.id.toolbar_container)).check(matches(isCompletelyDisplayed()));
        waitForIdle();
        checkPrivacyStatus(status);
        waitForIdle();
    }

    @And("^I go back to the Inbox$")
    public void I_go_back_to_the_Inbox() {
        timeRequiredForThisMethod(15);
        testUtils.goBackToMessageList();
    }

    @And("^I check color is (\\S+) at position (\\d+)$")
    public void I_check_color_is___at_position(String color, int position) {
        timeRequiredForThisMethod(5);
        waitForIdle();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(position)).check(matches(withBackgroundColor(testUtils.colorToID(color))));
        waitForIdle();
    }

    @And("^I open (\\d+) attached files$")
    public void I_open_attached_files(int attachments) {
        testUtils.emptyFolder("Download");
        openAttached(attachments);
        waitForIdle();
        File directory = new File(Environment.getExternalStorageDirectory().toString() + "/Download/");
        File[] files = directory.listFiles();
        byte[] buffer = new byte[8192];
        int count;
        for (File fileOpen : files) {
            timeRequiredForThisMethod(5);
            File file = new File(Environment.getExternalStorageDirectory().toString() + "/Download/" + fileOpen.getName());
            waitForIdle();
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
                    fail("couldn't find shaCode in json file");
                }
            } catch (Exception ex) {
                Timber.i("Couldn't get SHA256 from file: " + file.getName());
            }
        }
        testUtils.emptyFolder("Download");
    }

    @And("^I check (\\S+) is attached in draft$")
    public void I_check_attached_in_draft(String attachment) {
        waitForIdle();
        String attachmentText = "wrong attachment";
        switch (attachment) {
            case "settings":
                attachmentText = "settings.k9s";
                break;
            case "settingsthemedark":
                attachmentText = "settingsthemedark.k9s";
                break;
            case "MSoffice":
                attachmentText = "testmsoffice.docx";
                break;
            case "PDF":
                attachmentText = "testpdf.pdf";
                break;
            case "masterKey":
                attachmentText = "masterkey.asc";
                break;
            case "picture":
                attachmentText = "testpicture.png";
                break;
            default:
                fileName = "passphrase.asc";
                break;
        }
        testUtils.assertsTextExistsOnScreen(attachmentText);
    }

    @And("^I open attached Master Key$")
    public void I_open_attached_Master_Key() {
        testUtils.emptyFolder("Download");
        String masterKeyText = null;
        String masterKeyText2 = null;
        while (masterKeyText == null) {
            openAttachedMasterKey();
            waitForIdle();
            try {
                masterKeyText = testUtils.readFile(Environment.getExternalStorageDirectory().toString() + "/Download/", "masterkey.asc");
            } catch (Exception e) {
                Timber.i("Trying to read masterkey.asc file: " + e.getMessage());
            }
        }
        TestUtils.createFile("masterkeyfile.asc", R.raw.masterkeypro);
        masterKeyText2 = testUtils.readFile(Environment.getExternalStorageDirectory().toString(), "masterkeyfile.asc");
        if (!masterKeyText.equals(masterKeyText2)) {
            fail("Wrong Master key file");
        }
        testUtils.emptyFolder("Download");
        testUtils.emptyFolder("");
    }

    private void openAttached(int numberOfAttachments) {
        int attachments = -1;
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
                if (attachments == 0) {
                    fail("There are no attachments");
                }
                attachments = 0;
                for (UiObject2 object : device.findObjects(layout)) {
                    if (object.getResourceName() != null && object.getResourceName().equals(BuildConfig.APPLICATION_ID+":id/attachments")) {
                        int size = object.getChildren().size();
                        for (int attachment = 0; attachment < size; attachment++) {
                            if (!object.getChildren().get(attachment).getChildren().get(2).getText().contains("results.json") && !object.getChildren().get(attachment).getChildren().get(2).getText().contains("original.eml")) {
                                attachments ++;
                                object.getChildren().get(attachment).getChildren().get(0).click();
                            }
                        }
                        if (attachments != numberOfAttachments) {
                            fail("There are " + attachments + " attachments and should be " + numberOfAttachments);
                        }
                        return;
                    }
                }
            } catch (Exception ex) {
                Timber.i("Message Error: " + ex.getMessage());
                if (attachments == 0) {
                    fail("There are no attachments");
                }
                if (attachments != numberOfAttachments) {
                    fail("There are " + attachments + " attachments and should be " + numberOfAttachments);
                }
            }
        }
    }

    private void openAttachedMasterKey() {
        while (true) {
            try {
                TestUtils.swipeUpScreen();
                onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                TestUtils.swipeUpScreen();
                BySelector layout = By.clazz("android.widget.LinearLayout");
                for (UiObject2 object : device.findObjects(layout)) {
                    if (object.getResourceName() != null && object.getResourceName().equals(BuildConfig.APPLICATION_ID+":id/attachments") && object.getChildren().get(0).getChildren().get(2).getText().contains("masterkey")) {
                        object.getChildren().get(0).getChildren().get(0).click();
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
    public void I_set_checkbox_to(String resource, boolean checked) {
        timeRequiredForThisMethod(5);
        testUtils.checkBoxOnScreenChecked(testUtils.stringToID(resource), checked);
    }

    @Then("^I go back and save as draft$")
    public void I_go_back_and_save_as_draft() {
        timeRequiredForThisMethod(10);
        testUtils.goBackAndSaveAsDraft();
    }

    @Then("^I save as draft$")
    public void I_save_as_draft() {
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
        waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(position)).check(matches(withBackgroundColor(testUtils.colorToID(color))));
    }

    @Then("^I click acceptButton$")
    public void iClickAcceptButton() {
        timeRequiredForThisMethod(5);
        waitForIdle();
        testUtils.clickAcceptButton();
        waitForIdle();
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
    public void I_save_trustwords() {
        timeRequiredForThisMethod(10);
        waitForIdle();
        onView(withId(R.id.securityStatusText)).perform(click());
        waitForIdle();
        trustWords = getTextFromView(onView(withId(R.id.trustwords)));
        testUtils.pressBack();
        waitForIdle();
    }

    @Then("^I save test report$")
    public void I_save_report2() {
        Log.e("TEST","Estoy en save report");
        try  {


            HttpURLConnection connection = (HttpURLConnection) new URL("https://www.google.com").openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                // Not OK.
            }
            Log.e("TEST","Estoy en hecho en "+responseCode);
            return;
        } catch (Exception exception) {
            Log.e("TEST","Estoy en NO hecho: " + exception);
            return;
        }
    }

    public String accountAddress(String cucumberMessageTo) {
        String textCase = "";
        if (cucumberMessageTo.contains("-")) {
            String[] parts = cucumberMessageTo.split("-");
            cucumberMessageTo = parts[0];
            textCase = parts[1];
        }
        switch (cucumberMessageTo) {
            case "empty":
                cucumberMessageTo = "";
                break;
            case "myself":
                cucumberMessageTo = testUtils.getAccountAddress(accountSelected);
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
        switch (textCase) {
            case "UpperCase":
                cucumberMessageTo = cucumberMessageTo.toUpperCase();
                break;
            case "LowerCase":
                cucumberMessageTo = cucumberMessageTo.toLowerCase();
                break;
            case "MixCase":
                String newText = "";
                Random random = new Random();
                for (int i = 0; i < cucumberMessageTo.length(); i++) {
                    if (random.nextBoolean()) {
                        newText = newText + Character.toUpperCase(cucumberMessageTo.charAt(i));
                    } else {
                        newText = newText + Character.toLowerCase(cucumberMessageTo.charAt(i));
                    }
                }
                cucumberMessageTo = newText;
                break;
        }
        return cucumberMessageTo;
    }

    private void SetDirectory(File file) {
        CopyAssets(file); // Then run the method to copy the file.
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {


        } else if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Timber.e("Nope");
        }
    }

    private void CopyAssets(File file) {
            try {
                String extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/test/";
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
                    if (scenario == null) {
                        time[0] = 0;
                        fail("Timeout. Couldn't finish the test");
                    } else if (time[0] > finalTime) {
                        try {
                            time[0] = 0;
                            fail("Timeout. Couldn't finish the test");
                        } catch (Exception ex) {
                            Timber.e("Couldn't close the test");
                        }
                    }
                }
            }, 0, 1000);
        }
    }
}