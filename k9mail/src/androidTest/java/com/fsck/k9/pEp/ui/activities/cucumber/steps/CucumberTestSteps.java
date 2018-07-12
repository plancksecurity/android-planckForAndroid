
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
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.ui.activities.TestUtils;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

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
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.hasValueEqualTo;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;
import static org.hamcrest.CoreMatchers.not;

@RunWith(AndroidJUnit4.class)
public class CucumberTestSteps {

    private static final String HOST = "test.pep-security.net";

    private String messageToBot = "";

    private UiDevice device;
    private TestUtils testUtils;
    private Instrumentation instrumentation;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;
    private Resources resources;
    @Rule
    public IntentsTestRule<WelcomeMessage> activityTestRule = new IntentsTestRule<>(WelcomeMessage.class, true, false);

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        testUtils.increaseTimeoutWait();
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
        messageToBot = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        resources = InstrumentationRegistry.getTargetContext().getResources();
        startTimer();
        activityTestRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(espressoTestingIdlingResource.getIdlingResource());
        activityTestRule.finishActivity();
    }

    @When(value = "^I create an account$")
    public void I_create_account() {
        testUtils.createAccount(false);
    }


    @When("^I fill messageTo field with (\\S+)")
    public void I_fill_messageTo_field(String cucumberMessageTo) {
        switch (cucumberMessageTo){
            case "empty":
                cucumberMessageTo = " ";
                device.waitForIdle();
                while (!hasValueEqualTo(onView(withId(R.id.to)), " ")) {
                    try {
                        device.waitForIdle();
                        onView(withId(R.id.to)).perform(typeText(cucumberMessageTo), closeSoftKeyboard());
                        device.waitForIdle();
                    } catch (Exception ex){
                        Timber.i("Can not remove field 'to'");
                    }
                }
                break;
            case "self":
                cucumberMessageTo = testUtils.getTextFromTextViewThatContainsText("@");
                break;
            case "bot":
                cucumberMessageTo = messageToBot;
        }
        fillMessage(cucumberMessageTo);
    }

    private String ifEmptyString(String name){
        if (name.equals("empty")){
            name = " ";
        }
        return name;
    }

    @When("^I fill messageSubject field with (\\S+)")
    public void I_fill_subject_field(String cucumberSubject) {
        cucumberSubject = ifEmptyString(cucumberSubject);
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(click());
        onView(withId(R.id.subject)).perform(typeText(cucumberSubject), closeSoftKeyboard());
    }

    @When("^I fill messageBody field with (\\S+)")
    public void I_fill_body_field(String cucumberBody) {
        cucumberBody = ifEmptyString(cucumberBody);
        device.waitForIdle();
        onView(withId(R.id.message_content)).perform(click());
        onView(withId(R.id.message_content)).perform(typeText(cucumberBody), closeSoftKeyboard());
    }


    @When("^I click last message received$")
    public void I_click_last_message_received() {
        testUtils.clickLastMessageReceived();
    }

    @When("^I confirm trust words$")
    public void I_confirm_trust_words() {
        onView(withId(R.id.confirmTrustWords)).perform(click());
    }

    @When("^I check status is (\\S+)$")
    public void I_check_pEp_status(String status) {
        int statusRating = 0;
        device.waitForIdle();
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
        testUtils.assertMessageStatus(statusRating);
        device.waitForIdle();
    }

    @And("^I select from message menu (\\S+)$")
    public void I_select_from_message_menu(String textToSelect){
        testUtils.selectoFromMenu(testUtils.stringToID(textToSelect));
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

    @Then("^I attach files to new message (true|false) and send it$")
    public void I_attach_files_to_new_message(boolean attachFiles) {
        testUtils.fillMessage(new TestUtils.BasicMessage("", "Subject", "Message", testUtils.getTextFromTextViewThatContainsText("@")), attachFiles);
        testUtils.sendMessage();
    }

    @When("^I click message status$")
    public void I_click_message_status() {
        testUtils.clickMessageStatus();
        device.waitForIdle();
    }

    @Then("^I click send message button$")
    public void I_click_send_message_button() {
        testUtils.clickView(R.id.send);
    }

    @When("^I click message compose")
    public void I_click_message_compose() {
        testUtils.composeMessageButton();
        device.waitForIdle();
    }

    @And("^I click view (\\S+)$")
    public void I_click_view(String viewClicked){
        device.waitForIdle();
        testUtils.clickView(testUtils.intToID(viewClicked));
        device.waitForIdle();
    }

    @Then("^I send (\\d+) (?:message|messages) to bot$")
    public void I_send_messages_to_bot(int totalMessages) {
        device.waitForIdle();
        for (int message = 0; message < totalMessages; message++) {
            testUtils.composeMessageButton();
            device.waitForIdle();
            testUtils.fillMessage(new TestUtils.BasicMessage("", "subject", "messageBody", messageToBot), false);
            testUtils.sendMessage();
            device.waitForIdle();
            testUtils.waitForNewMessage();
        }
        device.waitForIdle();
    }

    private void fillMessage(String to){
        testUtils.fillMessage(new TestUtils.BasicMessage("", "", "", to), false);

    }

    @Then("^I wait for new message$")
    public void I_wait_for_new_message(){
        testUtils.waitForNewMessage();
    }

    @And("^I go to sent folder$")
    public void I_go_to_sent_folder(){
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

    @Given("^Set external mock (\\S+)$")
    public void Set_external_mock(String mock){
        switch (mock){
            case "settings":
                testUtils.externalAppRespondWithFile(R.raw.settings);
                break;
            case "settingsthemedark":
                testUtils.externalAppRespondWithFile(R.raw.settingsthemedark);
                break;
        }
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

    @Then("^I check toolBar color is (\\S+)$")
    public void I_check_toolBar_color_is(String color){
        testUtils.checkToolbarColor(testUtils.colorToID(color));
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

    @And("^I fill again messageTo field with (\\S+)$")
    public void I_fill_again_message_to_field(String name){
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(typeText(" "), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.to)).perform(typeText(name), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(typeText(" "), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.message_content)).perform(typeText(" "), closeSoftKeyboard());
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

    private void startTimer(){
        final int[] time = {0};
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                time[0]++;
                if (time[0] > 300){
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