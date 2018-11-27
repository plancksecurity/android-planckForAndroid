
package com.fsck.k9.pEp.ui.activities.cucumber.steps;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.view.KeyEvent;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.ui.activities.TestUtils;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.util.Timer;
import java.util.TimerTask;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.containsText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.containstText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.getTextFromView;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.viewIsDisplayed;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.waitUntilIdle;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;
import static org.hamcrest.CoreMatchers.not;
import org.json.JSONObject;

@RunWith(AndroidJUnit4.class)
public class CucumberTestSteps {

    private static final String HOST = "@test.pep-security.net";

    private String bot[];
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
    public IntentsTestRule<Accounts> activityTestRule = new IntentsTestRule<>(Accounts.class, true, false);

    @Before
    public void setup() {
        if (testUtils == null) {
            instrumentation = InstrumentationRegistry.getInstrumentation();
            device = UiDevice.getInstance(instrumentation);
            testUtils = new TestUtils(device, instrumentation);
            testUtils.increaseTimeoutWait();
            espressoTestingIdlingResource = new EspressoTestingIdlingResource();
            IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
            bot = new String[4];
            /*bot[0] = b;
            bot[1] = "bot001";
            bot[2] = "bot002";
            bot[3] = "bot003";*/
            resources = InstrumentationRegistry.getTargetContext().getResources();
            startTimer(50000);
            if (testUtils.getCurrentActivity() == null) {
                //startTimer(350);
                testUtils.testReset = true;
                try {
                    activityTestRule.launchActivity(new Intent());
                } catch (Exception ex) {
                    Timber.i("Cannot launch activity");
                }
            }
        }
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
        timer.cancel();
        if (testUtils.testReset) {
            while (testUtils.getCurrentActivity() != null) {
                device.waitForIdle();
                device.pressBack();
                try {
                    onView(withText(R.string.discard_action)).check(matches(isDisplayed()));
                    onView(withText(R.string.discard_action)).perform(click());
                } catch (Exception ex) {
                    Timber.i("There is no message to discard");
                }
            }
        }

        activityTestRule.finishActivity();
    }

    @When(value = "^I created an account$")
    public void I_create_account() {
        device.waitForIdle();
        if (!exists(onView(withId(R.id.message_list)))) {
            testUtils.createAccount(false);
        }
    }


    @When("^I enter (\\S+) in the messageTo field")
    public void I_fill_messageTo_field(String cucumberMessageTo) {
        device.waitForIdle();
        while (!exists(onView(withId(R.id.to)))) {
            device.waitForIdle();
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
                cucumberMessageTo = bot[0] + HOST;
                break;
            case "bot2":
                Timber.i("Filling message to bot2");
                cucumberMessageTo = bot[1] + HOST;
                break;
            case "bot3":
                Timber.i("Filling message to bot3");
                cucumberMessageTo = bot[2] + HOST;
                break;
            case "bot4":
                Timber.i("Filling message to bot4");
                cucumberMessageTo = bot[3] + HOST;
                break;
        }
        if (!(getTextFromView(onView(withId(R.id.to))).equals("") || getTextFromView(onView(withId(R.id.to))).equals(" "))) {
            cucumberMessageTo = cucumberMessageTo + ",";
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
                    onView(withId(R.id.to)).perform(typeText(cucumberMessageTo), closeSoftKeyboard());
                    filled = true;
                } catch (Exception ex) {
                    Timber.i("Couldn't find view: " + ex.getMessage());
                }
            }
        }
        try {
            onView(withId(R.id.subject)).perform(click(), closeSoftKeyboard());
            onView(withId(R.id.message_content)).perform(click(), closeSoftKeyboard());
            onView(withId(R.id.to)).check(matches(isDisplayed()));
        } catch (Exception ex) {
            Timber.i("Couldn't find view: " + ex.getMessage());
        }
    }

    @When("^I enter (\\S+) in the messageSubject field")
    public void I_fill_subject_field(String cucumberSubject) {
        textViewEditor(cucumberSubject,"subject");
    }

    @When("^I enter (\\S+) in the messageBody field")
    public void I_fill_body_field(String cucumberBody) {
        textViewEditor(cucumberBody, "message_content");
    }

    private void textViewEditor (String text, String viewName) {
        int viewId = testUtils.intToID(viewName);
        device.waitForIdle();
        UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
        scroll.swipe(Direction.DOWN, 1.0f);
        switch (text) {
            case "empty":
                testUtils.removeTextFromTextView(viewName);
                break;
            case "longText":
                text = testUtils.longText();
            default:
                while (!(containstText(onView(withId(viewId)), text))) {
                    try {
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                        onView(withId(viewId)).perform(click());
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                        onView(withId(viewId)).perform(typeText(text), closeSoftKeyboard());
                        onView(withId(viewId)).perform(closeSoftKeyboard());
                    } catch (Exception ex) {
                        if (viewIsDisplayed((viewId))) {
                            onView(withId(viewId)).perform(closeSoftKeyboard());
                            device.pressBack();
                        }
                    }
                }
        }
    }

    @When("^I compare messageBody with (\\S+)")
    public void I_compare_body(String cucumberBody) {
        String [] body;
        switch (cucumberBody) {
            case "empty":
                cucumberBody = resources.getString(R.string.default_signature);
                break;
            case "longText":
                cucumberBody = testUtils.longText();
                break;
        }
        testUtils.doWaitForResource(R.id.message_container);
        while (true) {
            device.waitForIdle();
            if (exists(onView(withId(R.id.message_container)))) {
                if (cucumberBody.equals("Rating/DecodedRating")) {
                    body = new String[2];
                    body[0] = "Rating | 6";
                    body[1] = "Decoded Rating | PEP_rating_reliable";
                } else {
                    body = new String[1];
                    body[0] = cucumberBody;
                }
                compareTextWithWebViewText(body);
                return;
            }
        }
    }


    @When("^I click the last message$")
    public void I_click_the_last_message() {
        testUtils.clickLastMessageReceived();
    }

    @When("^I click the last message received$")
    public void I_click_the_last_message_received() {
        testUtils.clickLastMessage();
    }

    @When("^I click message$")
    public void I_click_message() {
        testUtils.clickLastMessage();
    }

    @When("^I confirm trust words match$")
    public void I_confirm_trust_words_match() {
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.toolbar);
        device.waitForIdle();
        if (exists(onView(withId(R.id.tvPep)))) {
            onView(withId(R.id.tvPep)).perform(click());
        } else {
            onView(withId(R.id.pEp_indicator)).perform(click());
        }
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.toolbar);
        confirmAllTrustWords(testUtils.array);
        /*webViewText = getWebviewText();
        UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
        scroll.swipe(Direction.DOWN, 1.0f);
        try {
            onView(withId(R.id.to)).perform(click(), closeSoftKeyboard());
            testUtils.doWaitForResource(R.id.to);
            device.waitForIdle();
        } catch (Exception ex) {
            Timber.i("Cannot click field: to");
        }
        try {
            onView(withId(R.id.tvPep)).perform(click());
        } catch (Exception ex) {
            onView(withId(R.id.pEp_indicator)).perform(click());
        }
        confirmAllTrustWords(webViewText);
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.pep_menu_long_trustwords);
        confirmAllTrustWords(webViewText);
        */
    }

    private void confirmAllTrustWords (JSONArray array) {
        checkTrustWords(array, "short");
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.pep_menu_long_trustwords);
        checkTrustWords(array, "long");
    }

    private void checkTrustWords(JSONArray array, String words) {
        BySelector selector = By.clazz("android.widget.CheckedTextView");
        int size = 1;
        for (int positionToClick = 0; positionToClick < size; positionToClick++) {
            device.waitForIdle();
            testUtils.selectFromMenu(R.string.settings_language_label);
            size = calculateNewSize(size, selector);
            device.waitForIdle();
            selectLanguage(positionToClick, size, selector);
            getTrustWords();
            assertTrustWords(array, words);
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
        Assert.fail();
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
            getTrustWords();
            String []trustWordsSplited = trustWords.split("\\s+");
            checkWordIsInText(trustWordsSplited, webViewText);
        }
    }

    private  int calculateNewSize(int size, BySelector selector) {
        while (size <= 1) {
            device.waitForIdle();
            size = device.findObjects(selector).size();
        }
        return size;
    }

    private void selectLanguage(int positionToClick, int size, BySelector selector) {
        do {
            device.waitForIdle();
            for (int position = 0; position < size; position++) {
                if (position == positionToClick) {
                    while (device.findObjects(selector).size() <= 1){
                        device.waitForIdle();
                    }
                    while (!device.findObjects(selector).get(position).isChecked()) {
                        try {
                            device.waitForIdle();
                            device.findObjects(selector).get(position).longClick();
                            device.waitForIdle();
                        } catch (Exception ex) {
                            Timber.i("Cannot click language selected");
                        }
                    }
                    try {
                        onView(withId(android.R.id.button1)).perform(click());
                    } catch (Exception ex) {
                        Timber.i("Cannot find button1");
                    }
                }
            }
        } while (exists(onView(withId(android.R.id.button1))));
    }

    private void getTrustWords() {
        do {
            try {
                device.waitForIdle();
                trustWords = getTextFromView(onView(withId(R.id.trustwords)));
            } catch (Exception ex) {
                Timber.i("Cannot find trustWords: " + ex.getMessage());
            }
        } while (trustWords == null);
    }

    private void compareTextWithWebViewText(String [] arrayToCompare) {
        UiObject2 wb;
        boolean webViewLoaded = false;
        while (!webViewLoaded) {
            try {
                device.waitForIdle();
                waitUntilIdle();
                wb = device.findObject(By.clazz("android.webkit.WebView"));
                wb.click();
                UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
                scroll.swipe(Direction.UP, 1.0f);
                String webViewText = "empty";
                device.waitForIdle();
                UiObject2 webViewTemporal;
                boolean childFound = false;
                webViewTemporal = wb.getChildren().get(0);
                for (String textToCompare : arrayToCompare) {
                    while (!childFound) {
                        if (webViewTemporal.getText().contains(textToCompare)) {
                            webViewText = webViewTemporal.getText();
                            webViewLoaded = true;
                            childFound = true;
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
                    onView(withId(R.id.message_container)).check(matches(containsText(webViewText, textToCompare)));
                }
            } catch (Exception ex) {
                Timber.i("Cannot find webView: " + ex.getMessage());
            }
        }
        device.waitForIdle();
    }

    private void checkWordIsInText(String [] arrayToCompare, String webViewText) {
        for (String textToCompare : arrayToCompare) {
            if (!webViewText.contains(textToCompare)) {
                Assert.fail("Text not found in Trustwords");
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
                UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
                scroll.swipe(Direction.UP, 1.0f);
                device.waitForIdle();
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

    @When("^I reject trust words$")
    public void I_reject_trust_words() {
        onView(withId(R.id.wrongTrustwords)).perform(click());
    }

    @When("^I click confirm trust words$")
    public void I_click_confirm_trust_words() {
        testUtils.doWaitForResource(R.id.toolbar);
        device.waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        if (!exists(onView(withId(R.id.reply_message)))) {
            device.waitForIdle();
            testUtils.doWaitForResource(R.id.pEp_indicator);
            waitUntilIdle();
            while (exists(onView(withId(R.id.pEp_indicator)))) {
                device.waitForIdle();
                testUtils.clickView(R.id.pEp_indicator);
                Timber.i("Hecho click en indicator");
            }
        } else {
            while (exists(onView(withId(R.id.tvPep)))) {
                device.waitForIdle();
                testUtils.clickView(R.id.tvPep);
                Timber.i("Hecho click en tvPep");
            }
        }
        while (!viewIsDisplayed(R.id.confirmTrustWords)) {
            device.waitForIdle();
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
            scroll.swipe(Direction.UP, 1.0f);
        }
        onView(withId(R.id.confirmTrustWords)).perform(click());
    }
    @When("^I click wrong trust words$")
    public void I_click_wrong_trust_words() {
        testUtils.doWaitForResource(R.id.toolbar);
        device.waitForIdle();
        waitUntilIdle();
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
        if (!exists(onView(withId(R.id.reply_message)))) {
            device.waitForIdle();
            testUtils.doWaitForResource(R.id.pEp_indicator);
            waitUntilIdle();
            while (exists(onView(withId(R.id.pEp_indicator)))) {
                device.waitForIdle();
                testUtils.clickView(R.id.pEp_indicator);
            }
        } else {
            while (exists(onView(withId(R.id.tvPep)))) {
                device.waitForIdle();
                testUtils.clickView(R.id.tvPep);
            }
        }
        while (!exists(onView(withId(R.id.confirmTrustWords)))) {
            device.waitForIdle();
        }
        onView(withId(R.id.wrongTrustwords)).perform(click());
    }

    @When("^I stop trusting$")
    public void I_untrust_trust_words() {
        testUtils.clickMessageStatus();
        device.waitForIdle();
        onView(withId(R.id.handshake_button_text)).perform(click());
        device.waitForIdle();
        device.pressBack();
        device.waitForIdle();
    }

    @When("^I check in the handshake dialog if the privacy status is (\\S+)$")
    public void I_check_pEp_status(String status) {
        checkPrivacyStatus(status);
        device.waitForIdle();
    }

    private void checkPrivacyStatus(String status){
        int statusRating = -10;
        device.waitForIdle();
        waitUntilIdle();
        BySelector selector = By.clazz("android.widget.ScrollView");
        onView(withId(R.id.toolbar_container)).check(matches(isDisplayed()));
            try {
                onView(withId(R.id.subject)).perform(typeText(" "), closeSoftKeyboard());
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
                    onView(withId(R.id.subject)).perform(typeText(" "), closeSoftKeyboard());
                } catch (Exception e) {
                    Timber.i("Cannot find subject");
                }
            }
        switch (status){
            case "pEpRatingUndefined":
                statusRating = 0;
                break;
            case "pEpRatingCannotDecrypt":
                statusRating = 1;
                break;
            case "pEpRatingHaveNoKey":
                statusRating = 2;
                break;
            case "pEpRatingUnencrypted":
                statusRating = 3;
                break;
            case "pEpRatingUnencryptedForSome":
                statusRating = 4;
                break;
            case "pEpRatingUnreliable":
                statusRating = 5;
                break;
            case "pEpRatingReliable":
                statusRating = 6;
                break;
            case "pEpRatingTrusted":
                statusRating = 7;
                break;
            case "pEpRatingTrustedAndAnonymized":
                statusRating = 8;
                break;
            case "pEpRatingFullyAnonymous":
                statusRating = 9;
                break;
            case "pEpRatingMistrust":
                statusRating = 10;
                break;
            case "pEpRatingB0rken":
                statusRating = 11;
                break;
            case "pEpRatingUnderAttack":
                statusRating = 12;
                break;
        }
        if (statusRating != -10) {
            testUtils.assertMessageStatus(statusRating);
        } else {
            testUtils.checkToolbarColor(testUtils.colorToID(status));
        }
    }

    @And("^I select from message menu (\\S+)$")
    public void I_select_from_message_menu(String textToSelect){
        testUtils.selectFromMenu(testUtils.stringToID(textToSelect));
        device.waitForIdle();
    }

    @Then("^I open menu$")
    public void I_select_from_menu(){
        device.waitForIdle();
        testUtils.openOptionsMenu();
    }

    @Then("^I select from screen (\\S+)$")
    public void I_select_from_screen(String textToSelect){
        device.waitForIdle();
        testUtils.selectFromScreen(testUtils.stringToID(textToSelect));
        device.waitForIdle();
    }

    @Then("^I remove account$")
    public void I_remove_account() {
        testUtils.goBackAndRemoveAccount();
    }

    @Then("^I remove email address$")
    public void I_remove_email_address(){
        device.waitForIdle();
        device.pressKeyCode(KeyEvent.KEYCODE_DEL);
        device.waitForIdle();
    }

    @Then("^I attach files to message$")
    public void I_attach_files_to_message() {
        testUtils.fillMessage(new TestUtils.BasicMessage("", "", "", ""), true);
        testUtils.sendMessage();
        testUtils.testReset = true;
    }

    @Then("^I attach (\\S+)$")
    public void I_attach_file_to_message(String file) {
        device.waitForIdle();
        Set_external_mock(file);
        testUtils.attachFile(fileName);
        device.waitForIdle();
        testUtils.testReset = true;
    }

    @Given("^Set external mock (\\S+)$")
    public void Set_external_mock(String mock){
        int raw = 0;
        switch (mock){
            case "settings":
                raw = R.raw.settings;
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
        testUtils.clickMessageStatus();
        device.waitForIdle();
    }

    @Then("^I click the send message button$")
    public void I_click_then_send_message_button() {
        while (exists(onView(withId(R.id.send)))) {
            testUtils.clickView(R.id.send);
        }
    }

    @When("^I click compose message")
    public void I_click_message_compose_button() {
        testUtils.composeMessageButton();
    }

    @When("^I run the tests")
    public void startTest() {
        testUtils.readBotList();
        bot = testUtils.botList;
        while (true) {
            try {
                device.waitForIdle();
                if (exists(onView(withId(R.id.accounts_list)))) {
                    while (exists(onView(withId(R.id.accounts_list)))) {
                        onView(withId(R.id.accounts_list)).perform(click());
                        device.waitForIdle();
                    }
                    if (!exists(onView(withId(R.id.accounts_list)))) {
                        testUtils.swipeDownMessageList();
                        device.waitForIdle();
                        testUtils.getMessageListSize();
                        time[0] = 0;
                        return;
                    }
                } else {
                    while (!exists(onView(withId(R.id.accounts_list)))) {
                        testUtils.pressBack();
                        try {
                            device.waitForIdle();
                            onView(withText(R.string.discard_action)).check(matches(isCompletelyDisplayed()));
                            onView(withText(R.string.discard_action)).perform(click());
                        } catch (Exception ex) {
                            Timber.i("There is no message to discard");
                        }
                        device.waitForIdle();
                    }
                }
            } catch (Exception ex) {
                while (!exists(onView(withId(R.id.accounts_list)))) {
                    testUtils.pressBack();
                    device.waitForIdle();
                }
                Timber.i("View not found. Start test: " + ex);
            }
        }
    }

    @And("^I click view (\\S+)$")
    public void I_click_view(String viewClicked){
        device.waitForIdle();
        testUtils.clickView(testUtils.intToID(viewClicked));
        device.waitForIdle();
    }

    @And("^I click reply message$")
    public void I_click_reply_message(){
        device.waitForIdle();
        if (!viewIsDisplayed(testUtils.intToID("reply_message"))) {
            UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
            scroll.swipe(Direction.DOWN, 1.0f);
        }
        onView(withId(R.id.reply_message)).check(matches(isDisplayed()));
        testUtils.clickView(testUtils.intToID("reply_message"));
        device.waitForIdle();
    }

    @Then("^I send (\\d+) (?:message|messages) to (\\S+) with subject (\\S+) and body (\\S+)$")
    public void I_send_messages_to_bot(int totalMessages,String botName, String subject, String body) {
        String messageTo = "nothing";
        switch (botName){
            case "bot1":
                messageTo = bot[0] + HOST;
                break;
            case "bot2":
                messageTo = bot[1] + HOST;
                break;
            case "bot3":
                messageTo = bot[2] + HOST;
                break;
            case "bot4":
                messageTo = bot[3] + HOST;
                break;
        }
        device.waitForIdle();
        for (int message = 0; message < totalMessages; message++) {
            testUtils.composeMessageButton();
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
        testUtils.waitForNewMessage();
    }

    @And("^I go to the sent folder$")
    public void I_go_to_the_sent_folder(){
        device.waitForIdle();
        testUtils.goToSentFolder();
    }

    @And("^I click the first message$")
    public void I_click_the_first_message(){
        testUtils.clickFirstMessage();
    }

    @Given("^I create and remove (\\d+) accounts$")
    public void I_create_and_remove_accounts(int total){
        for (int account = 0; account < total; account++) {
            testUtils.createAccount(false);
            testUtils.goBackAndRemoveAccount();
            device.waitForIdle();
        }
    }

    @Then("^I discard the message$")
    public void I_discard_the_message(){
        device.waitForIdle();
        testUtils.pressBack();
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
    }

    @Given("^I press back$")
    public void I_press_back(){
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
    }

    @And("^I go back to app$")
    public void I_go_back_to_app(){
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
    public void I_check_toolBar_color_is(String color){
        while(!viewIsDisplayed(R.id.toolbar)) {
            device.waitForIdle();
        }
        boolean wait = false;
        while (!wait) {
            try {
                device.waitForIdle();
                onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
                if (!viewIsDisplayed(R.id.to)) {
                    try {
                        UiObject2 scroll = device.findObject(By.clazz("android.widget.ScrollView"));
                        scroll.swipe(Direction.DOWN, 1.0f);
                    } catch (Exception ex) {
                        Timber.i("Cannot do scroll down");
                    }
                }
                device.waitForIdle();
                waitUntilIdle();
                wait = true;
            } catch (Exception ex) {
                Timber.i("Cannot find toolbar");
            }
        }
        checkPrivacyStatus(color);
    }

    @And("^I go back to message compose$")
    public void I_go_back_to_message_compose(){
        testUtils.goBackToMessageList();
    }

    @And("^I check color is (\\S+) at position (\\d+)$")
    public void I_check_color_is___at_position(String color, int position){
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(position)).check(matches(withBackgroundColor(testUtils.colorToID(color))));
        device.waitForIdle();
    }

    @And("^I open (\\d+) attached files$")
    public void I_open_attached_files(int total) {
        device.waitForIdle();
        testUtils.clickAttachedFiles(total);
    }

    @Then("^I set checkbox (\\S+) to (true|false)$")
    public void I_set_checkbox_to(String resource, boolean checked){
        testUtils.checkBoxOnScreenChecked(testUtils.stringToID(resource), checked);
    }

    @Then("^I go back and save as draft$")
    public void I_go_back_and_save_as_draft(){
        testUtils.goBackAndSaveAsDraft(activityTestRule);
    }

    @And("^I compare texts on screen: (\\S+) and (\\S+)$")
    public void I_compare_texts_on_screen(String text1, String text2) {
        testUtils.assertsTextsOnScreenAreEqual(testUtils.stringToID(text1), testUtils.stringToID(text2));
    }

    @And("^I check status color is (\\S+) at position (\\d+)$")
    public void I_check_color_at(String color, int position) {
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(position)).check(matches(withBackgroundColor(testUtils.colorToID(color))));
    }

    @Then("^I click acceptButton$")
    public void iClickAcceptButton() {
        device.waitForIdle();
        testUtils.clickAcceptButton();
        device.waitForIdle();
    }

    @And("^I do next thing$")
    public void iDoNextThing() {
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
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @And("^I save trustWords$")
    public void I_save_trustwords(){
        device.waitForIdle();
        onView(withId(R.id.tvPep)).perform(click());
        device.waitForIdle();
        trustWords = getTextFromView(onView(withId(R.id.trustwords)));
        device.pressBack();
        device.waitForIdle();
    }

    private void startTimer(int finalTime) {
        if (time[0] == 0) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    time[0]++;
                    Timber.i("Timeout: " + time[0] + "/" + finalTime);
                    if (activityTestRule == null) {
                        Assert.fail();
                    } else if (time[0] > finalTime) {
                        try {
                            Timber.e("Timeout: closing the app...");
                            Assert.fail();
                        } catch (Exception ex) {
                            Timber.e("Couldn't close the app");
                        }
                    }
                }
            }, 0, 1000);
        }
    }
}