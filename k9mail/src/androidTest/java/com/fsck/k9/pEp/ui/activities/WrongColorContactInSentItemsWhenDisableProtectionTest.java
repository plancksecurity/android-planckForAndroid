package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.anything;

@RunWith(AndroidJUnit4.class)
public class WrongColorContactInSentItemsWhenDisableProtectionTest {
    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private BySelector textViewSelector;
    private Resources resources;
    private Context context;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        textViewSelector = By.clazz("android.widget.TextView");
        context = InstrumentationRegistry.getTargetContext();
        resources = context.getResources();
        testUtils.startActivity();
    }

    @Test
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        testUtils.createAccount(false);
        composeMessage();
        checkPEpStatus(Rating.pEpRatingTrusted);
        selectFromMenu(R.string.pep_force_unprotected);
        checkPEpStatus(Rating.pEpRatingUnencrypted);
        device.waitForIdle();
        testUtils.sendMessage();
        goToSentFolder();
        selectFirstMessage();
        assertStatus(Rating.pEpRatingTrusted.value);
        removeAccount();
    }

    private void removeAccount() {
        for (int pressBack = 0; pressBack < 5; pressBack++) {
            device.waitForIdle();
            testUtils.pressBack();
        }
        device.waitForIdle();
        testUtils.removeLastAccount();
    }

    private void assertStatus(int status) {
        testUtils.doWaitForResource(R.id.tvPep);
        device.waitForIdle();
        testUtils.assertMessageStatus(status);
    }

    private void selectFirstMessage() {
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.message_list);
        device.waitForIdle();
        onData(anything()).inAdapterView(withId(R.id.message_list)).atPosition(0).perform(click());
        device.waitForIdle();
    }

    private void selectFromMenu(int textToSelect) {
        testUtils.openOptionsMenu();
        testUtils.selectFromMenu(textToSelect);
    }

    private void checkPEpStatus(Rating rating) {
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.pEp_indicator);
        device.waitForIdle();
        testUtils.checkStatus(rating);
        testUtils.pressBack();
    }

    private void composeMessage() {
        testUtils.composeMessageButton();
        device.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_BODY, MESSAGE_SUBJECT, messageTo), false);
    }

    private void goToSentFolder() {
        device.waitForIdle();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.account_settings_folders);
        device.waitForIdle();
        String folder = resources.getString(R.string.special_mailbox_name_sent);
        for (UiObject2 textView : device.findObjects(textViewSelector)) {
            try {
                if (textView.findObject(textViewSelector).getText() != null && textView.findObject(textViewSelector).getText().contains(folder)) {
                    device.waitForIdle();
                    textView.findObject(textViewSelector).click();
                    device.waitForIdle();
                    return;
                }
                device.waitForIdle();
            } catch (Exception e) {
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

