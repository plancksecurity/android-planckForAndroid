package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import timber.log.Timber;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.not;

@RunWith(AndroidJUnit4.class)
public class AssertColorContactInSentItemsWhenDisableProtectionTest {
    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private BySelector textViewSelector;
    private Resources resources;
    private Context context;
    private Instrumentation instrumentation;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        testUtils.increaseTimeoutWait();
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
        textViewSelector = By.clazz("android.widget.TextView");
        context = InstrumentationRegistry.getTargetContext();
        resources = context.getResources();
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(espressoTestingIdlingResource.getIdlingResource());
    }

    @Test (timeout = TIMEOUT_TEST)
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        testUtils.createAccount(false);
        composeMessage();
        checkPEpStatus(Rating.pEpRatingTrusted);
        selectFromMenu(R.string.pep_force_unprotected);
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.subject);
        onView(withId(R.id.message_content)).perform(typeText(" "));
        checkPEpStatus(Rating.pEpRatingUnencrypted);
        device.waitForIdle();
        testUtils.sendMessage();
        testUtils.waitForNewMessage();
        goToSentFolder();
        selectFirstMessage();
        testUtils.clickView(R.id.tvPep);
        testUtils.assertMessageStatus(Rating.pEpRatingTrusted.value);
        testUtils.goBackAndRemoveAccount();
    }

    private void selectFirstMessage() {
        boolean firstMessageClicked = false;
        Activity currentActivity = testUtils.getCurrentActivity();
        while (currentActivity == testUtils.getCurrentActivity()) {
            while (!firstMessageClicked) {
                try {
                    device.waitForIdle();
                    testUtils.swipeDownMessageList();
                    device.waitForIdle();
                    onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
                    firstMessageClicked = true;
                    device.waitForIdle();
                } catch (Exception ex) {
                    Timber.i("Message list view not found");
                }
            }
        }
    }

    private void selectFromMenu(int textToSelect) {
        testUtils.selectoFromMenu(textToSelect);
    }

    private void checkPEpStatus(Rating rating) {
        testUtils.doWaitForResource(R.id.pEp_indicator);
        testUtils.checkStatus(rating);
        device.waitForIdle();
        testUtils.pressBack();
    }

    private void composeMessage() {
        testUtils.composeMessageButton();
        device.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
    }

    private void goToSentFolder() {
        device.waitForIdle();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.account_settings_folders);
        device.waitForIdle();
        String folder = resources.getString(R.string.special_mailbox_name_sent);
        boolean folderClicked = false;
        while (!folderClicked) {
            for (UiObject2 textView : device.findObjects(textViewSelector)) {
                try {
                    if (textView.findObject(textViewSelector).getText() != null && textView.findObject(textViewSelector).getText().contains(folder)) {
                        textView.findObject(textViewSelector).click();
                        folderClicked = true;
                        return;
                    }
                    device.waitForIdle();
                } catch (Exception e) {
                    Timber.i("View is not sent folder");
                }
            }
        }
        device.waitForIdle();
        waitForTextOnScreen(resources.getString(R.string.special_mailbox_name_sent));
    }

    private void waitForTextOnScreen(String text) {
        boolean textIsOk = false;
        do {
            device.waitForIdle();
            try {
                textIsOk = testUtils.getTextFromTextViewThatContainsText(text).contains(resources.getString(R.string.special_mailbox_name_sent));
            } catch (Exception e) {

            }
        } while (!textIsOk);
    }
}

