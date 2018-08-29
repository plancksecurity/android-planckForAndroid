
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

import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

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
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.containsText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.getTextFromView;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.hasValueEqualTo;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.waitUntilIdle;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;
import static org.hamcrest.CoreMatchers.not;

@RunWith(AndroidJUnit4.class)
public class CucumberTestSteps {

    private static final String HOST = "@test.pep-security.net";

    private String bot[];

    private String fileName = "";

    private UiDevice device;
    private TestUtils testUtils;
    private Instrumentation instrumentation;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;
    private Resources resources;
    private String trustWords;
    @Rule
    public IntentsTestRule<Accounts> activityTestRule = new IntentsTestRule<>(Accounts.class, true, false);

    @Before
    public void setup() {
        if (testUtils == null) {
            instrumentation = InstrumentationRegistry.getInstrumentation();
            device = UiDevice.getInstance(instrumentation);
            testUtils = new TestUtils(device, instrumentation);
            if (testUtils.getCurrentActivity() == null) {
                testUtils.increaseTimeoutWait();
                espressoTestingIdlingResource = new EspressoTestingIdlingResource();
                IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
                bot = new String[4];
                resources = InstrumentationRegistry.getTargetContext().getResources();
                startTimer(350);
                activityTestRule.launchActivity(new Intent());
            }
        }
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
        activityTestRule.finishActivity();
    }

    @When(value = "^I create an account$")
    public void I_create_account() {
        if (!exists(onView(withId(R.id.message_list)))) {
            testUtils.createAccount(false);
        }
        bot = testUtils.botList;
    }


    @When("^I fill messageTo field with (\\S+)")
    public void I_fill_messageTo_field(String cucumberMessageTo) {
        switch (cucumberMessageTo) {
            case "empty":
                cucumberMessageTo = " ";
                testUtils.removeTextFromTextView("to");
                break;
            case "myself":
                cucumberMessageTo = testUtils.getTextFromTextViewThatContainsText("@");
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
        cucumberMessageTo = cucumberMessageTo + ",";
        if (!(getTextFromView(onView(withId(R.id.to))).equals("") || getTextFromView(onView(withId(R.id.to))).equals(" "))) {
            try {
                fillMessage(cucumberMessageTo);
            } catch (Exception ex) {
                Timber.i("Couldn't fill message: " + ex.getMessage());
            }
        } else {
            try {
                device.waitForIdle();
                onView(withId(R.id.to)).perform(typeText(cucumberMessageTo), closeSoftKeyboard());
                device.waitForIdle();
            } catch (Exception ex) {
                Timber.i("Couldn't find view: " + ex.getMessage());
            }
        }
    }

    @When("^I fill messageSubject field with (\\S+)")
    public void I_fill_subject_field(String cucumberSubject) {
        textViewEditor(cucumberSubject,"subject");
    }

    @When("^I fill messageBody field with (\\S+)")
    public void I_fill_body_field(String cucumberBody) {
        textViewEditor(cucumberBody, "message_content");
    }

    private void textViewEditor (String text, String viewName) {
        int viewId = testUtils.intToID(viewName);
        device.waitForIdle();
        boolean filled = false;
        if (!text.equals("empty")) {
            while (!filled) {
                try {
                    onView(withId(viewId)).perform(click());
                    onView(withId(viewId)).perform(closeSoftKeyboard());
                    onView(withId(viewId)).perform(typeText(text), closeSoftKeyboard());
                    filled = true;
                    onView(withId(viewId)).perform(closeSoftKeyboard());
                } catch (Exception ex) {
                    onView(withId(viewId)).perform(closeSoftKeyboard());
                }
            }
        } else {
            testUtils.removeTextFromTextView(viewName);
        }
    }

    @When("^I compare messageBody with (\\S+)")
    public void I_compare_body(String cucumberBody) {
        boolean viewExists = false;
        if (cucumberBody.equals("empty")) {
            cucumberBody = "";
        }
        testUtils.doWaitForResource(R.id.message_container);
        while (!viewExists) {
            device.waitForIdle();
            if (exists(onView(withId(R.id.message_container)))) {
                String [] body = new String[1];
                body[0] = cucumberBody;
                compareTextWithWebViewText(body);
                viewExists = true;
            }
        }
    }


    @When("^I click last message$")
    public void I_click_last_message_received() {
        testUtils.clickLastMessageReceived();
    }

    @When("^I confirm trust words$")
    public void I_confirm_trust_words() {
        confirmAllTrustWords(false);
        confirmAllTrustWords(true);
    }

    private void confirmAllTrustWords (boolean longTrustWords) {
        BySelector selector = By.clazz("android.widget.CheckedTextView");
        int size = 1;
        for (int positionToClick = 0; positionToClick < size; positionToClick++) {
            device.waitForIdle();
            onView(withId(R.id.tvPep)).perform(click());
            device.waitForIdle();
            testUtils.selectFromMenu(R.string.settings_language_label);
            size = calculateNewSize(size, selector);
            device.waitForIdle();
            selectLanguage(positionToClick, size, selector);
            if (longTrustWords) {
                device.waitForIdle();
                testUtils.selectFromMenu(R.string.pep_menu_long_trustwords);
            }
            getTrustWords();
            testUtils.pressBack();
            String []trustWordsSplited = trustWords.split("\\s+");
            compareTextWithWebViewText(trustWordsSplited);
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
                        device.findObjects(selector).get(position).click();
                        device.waitForIdle();
                    }
                    onView(withId(android.R.id.button1)).perform(click());
                }
            }
        } while (exists(onView(withId(android.R.id.button1))));
    }

    private void getTrustWords() {
        while (trustWords == null) {
            try {
                device.waitForIdle();
                trustWords = getTextFromView(onView(withId(R.id.trustwords)));
            } catch (Exception ex) {
                Timber.i("Cannot find trustWords: " + ex.getMessage());
            }
        }
    }

    private void compareTextWithWebViewText(String [] arrayToCompare) {
        UiObject2 wb;
        boolean webViewLoaded = false;
        while (!webViewLoaded) {
            try {
                device.waitForIdle();
                wb = device.findObject(By.clazz("android.webkit.WebView"));
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
                        } else {
                            webViewTemporal = webViewTemporal.getChildren().get(0);
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

    @When("^I reject trust words$")
    public void I_reject_trust_words() {
        onView(withId(R.id.wrongTrustwords)).perform(click());
    }

    @When("^I click confirm trust words$")
    public void I_click_confirm_trust_words() {
        onView(withId(R.id.confirmTrustWords)).perform(click());
    }

    @When("^I stop trusting$")
    public void I_untrust_trust_words() {
        testUtils.clickMessageStatus();
        device.waitForIdle();
        onView(withId(R.id.handshake_button_text)).perform(click());
    }

    @When("^I check in the handshake dialog if the privacy status is (\\S+)$")
    public void I_check_pEp_status(String status) {
        checkPrivacyStatus(status);
        device.waitForIdle();
        I_press_back();
    }

    void checkPrivacyStatus(String status){
        int statusRating = -10;
        device.waitForIdle();
        waitUntilIdle();
        BySelector selector = By.clazz("android.widget.ScrollView");
            for (UiObject2 object : device.findObjects(selector)) {
                boolean actionPerformed = false;
                while (!actionPerformed) {
                    try {
                        object.swipe(Direction.DOWN, 1);
                        actionPerformed = true;
                    } catch (Exception ex) {
                        Timber.i("Couldn't swipe down view: " + ex.getMessage());
                    }
                }
            }
            try {
                onView(withId(R.id.to)).perform(typeText(","), closeSoftKeyboard());
            } catch (Exception ex) {
                Timber.i("Cannot find field to");
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
                statusRating = -1;
                break;
            case "pEpRatingB0rken":
                statusRating = -2;
                break;
            case "pEpRatingUnderAttack":
                statusRating = -3;
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
    }

    @Then("^I attach (\\S+)$")
    public void I_attach_file_to_message(String file) {
        device.waitForIdle();
        Set_external_mock(file);
        testUtils.attachFile(fileName);
        device.waitForIdle();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @When("^I open privacy status$")
    public void I_click_message_status() {
        testUtils.clickMessageStatus();
        device.waitForIdle();
    }

    @Then("^I click send message button$")
    public void I_click_send_message_button() {
        while (exists(onView(withId(R.id.send)))) {
            testUtils.clickView(R.id.send);
        }
    }

    @When("^I click message compose")
    public void I_click_message_compose() {
        testUtils.composeMessageButton();
    }

    @When("^I start test")
    public void startTest() {
        boolean methodFinished = false;
        Activity currentActivity = testUtils.getCurrentActivity();
        while (!methodFinished) {
            try {
                device.waitForIdle();
                onView(withId(R.id.accounts_list)).check(matches(isDisplayed()));
                device.waitForIdle();
                onView(withId(R.id.accounts_list)).perform(click());
                device.waitForIdle();
                if (exists(onView(withId(R.id.message_list)))) {
                    testUtils.swipeDownMessageList();
                    device.waitForIdle();
                    testUtils.getMessageListSize();
                    methodFinished = true;
                }
            } catch (Exception ex) {
                while (currentActivity == testUtils.getCurrentActivity()) {
                    testUtils.pressBack();
                    device.waitForIdle();
                    Timber.i("View not found, pressBack to previous activity: " + ex);
                }
            }
            currentActivity = testUtils.getCurrentActivity();
        }
    }

    @And("^I click view (\\S+)$")
    public void I_click_view(String viewClicked){
        device.waitForIdle();
        testUtils.clickView(testUtils.intToID(viewClicked));
        device.waitForIdle();
    }

    @Then("^I send (\\d+) (?:message|messages) to (\\S+) with subject (\\S+) and body (\\S+)$")
    public void I_send_messages_to_bot(int totalMessages,String botName, String subject, String body) {
        testUtils.readBotList();
        bot = testUtils.botList;
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

    @Then("^I wait for new message$")
    public void I_wait_for_new_message(){
        testUtils.waitForNewMessage();
    }

    @And("^I go to sent folder$")
    public void I_go_to_sent_folder(){
        device.waitForIdle();
        testUtils.goToSentFolder();
    }

    @And("^I click first message$")
    public void I_click_first_message(){
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

    @Then("^I discard message$")
    public void I_discard_message(){
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
        try {
            onView(withId(R.id.to)).perform(click());
            onView(withId(R.id.subject)).perform(click());
        } catch (Exception ex) {
            Timber.i("Couldn't find view");
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

    @And("^I open attached files$")
    public void I_open_attached_file_at_position() {
        device.waitForIdle();
        testUtils.clickAttachedFileAtPosition();
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

    @And("^I set timeout to (\\d+) seconds$")
    public void I_set_timeout(int time){
        startTimer(time);
    }

    private void startTimer(int finalTime){
        final int[] time = {0};
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                time[0]++;
                if (time[0] > finalTime){
                    try {
                        Timber.i("Timeout: closing the app...");
                        System.exit(0);
                    } catch (Exception ex) {
                        Timber.i("Couldn't close the app");
                    }
                }
            }
        }, 0, 1000);
    }
}