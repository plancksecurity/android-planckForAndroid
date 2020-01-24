package com.fsck.k9.pEp.ui.activities;

import android.content.res.Resources;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;
import timber.log.Timber;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

@RunWith(AndroidJUnit4.class)
public class AssertColorContactInSentItemsWhenDisableProtectionTest {
    private UiDevice device;
    private TestUtils testUtils;
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private BySelector textViewSelector;
    private Resources resources;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        testUtils.increaseTimeoutWait();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        textViewSelector = By.clazz("android.widget.TextView");
        resources = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources();
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        testUtils.createAccount();
        composeMessage();
        checkPEpStatus(Rating.pEpRatingTrusted);
        selectFromMenu(R.string.pep_force_unprotected);
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(typeText(" "));
        checkPEpStatus(Rating.pEpRatingUnencrypted);
        device.waitForIdle();
        testUtils.sendMessage();
        testUtils.waitForNewMessage();
        goToSentFolder();
        testUtils.clickFirstMessage();
        testUtils.clickView(R.id.tvPep);
        testUtils.assertMessageStatus(Rating.pEpRatingTrusted.value);
        testUtils.goBackAndRemoveAccount();
    }

    private void selectFromMenu(int textToSelect) {
        //testUtils.selectoFromMenu(textToSelect);
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
        String messageTo = testUtils.getTextFromTextViewThatContainsText("@");
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

