package com.fsck.k9.pEp.ui.activities;


import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MessageUnsecureWhenDisableProtectionTest {
    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String messageTo = "";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private BySelector textViewSelector;
    private String messageReceivedDate[];

    int lastMessageReceivedPosition;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice);
        testUtils.increaseTimeoutWait();
        textViewSelector = By.clazz("android.widget.TextView");
        messageReceivedDate = new String[5];
        testUtils.startActivity();
    }

    @Test
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        //testUtils.getLastMessageReceived();
        getLastMessageReceived();
        testUtils.composeMessageButton();
        uiDevice.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        uiDevice.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingTrusted);
        testUtils.pressBack();
        testUtils.openOptionsMenu();
        testUtils.selectFromMenu(R.string.pep_force_unprotected);
        uiDevice.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.sendMessage();
        uiDevice.waitForIdle();
        //testUtils.waitForMessageWithText(MESSAGE_BODY, MESSAGE_SUBJECT);
        waitForMessageWithText(MESSAGE_BODY, MESSAGE_SUBJECT);
        //testUtils.clickLastMessageReceived();
        clickLastMessageReceived();
        uiDevice.waitForIdle();
        //testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        onView(withId(R.id.tvPep)).perform(click());
        onView(withId(R.id.pEpTitle)).check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingUnencrypted.value))));
        uiDevice.waitForIdle();
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.pressBack();
        //removeMessagesFromList();
    }
    public void getLastMessageReceived() {
        uiDevice.waitForIdle();
        onView(withId(R.id.message_list))
                .perform(swipeDown());
        uiDevice.waitForIdle();
        lastMessageReceivedPosition = getLastMessageReceivedPosition();
        int message = 0;
        if (lastMessageReceivedPosition != -1){
            removeMessagesFromList();
            int size = uiDevice.findObjects(textViewSelector).size();
            for (; (message < 5) && (lastMessageReceivedPosition + 1 + message * 3 < size); message++) {
                messageReceivedDate[message] = uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 1 + message * 3).getText();
            }
        } else {
            for (; message < 5; message++) {
                messageReceivedDate[message] = "";
            }
            lastMessageReceivedPosition = uiDevice.findObjects(textViewSelector).size();
        }
    }

    public void removeMessagesFromList(){
        uiDevice.waitForIdle();
        if (uiDevice.findObjects(textViewSelector).size() > lastMessageReceivedPosition) {
            uiDevice.waitForIdle();
            uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition).click();
            boolean emptyList = false;
            do {
                try {
                    uiDevice.waitForIdle();
                    onView(withId(R.id.delete)).check(matches(isDisplayed()));
                    onView(withId(R.id.delete)).perform(click());
                } catch (NoMatchingViewException e) {
                    emptyList = true;
                }
            } while (!emptyList);
        }
    }

    public int getLastMessageReceivedPosition() {
        int size = uiDevice.findObjects(textViewSelector).size();
        for (int position = 0; position < size; position++) {
            String textAtPosition = uiDevice.findObjects(textViewSelector).get(position).getText();
            if (textAtPosition != null && textAtPosition.contains("@")) {
                position++;
                if (position < size) {
                    while (uiDevice.findObjects(textViewSelector).get(position).getText() == null) {
                        position++;
                        if (position >= size) {
                            return -1;
                        }
                    }
                }
                return position;
            }
        }
        return size;
    }

    public void waitForMessageWithText(String textInMessage, String preview) {
        boolean messageSubject = false;
        boolean messagePreview = false;
        boolean emptyMessageList;
        emptyMessageList = uiDevice.findObjects(textViewSelector).size() <= lastMessageReceivedPosition;
        if (!emptyMessageList) {
            do {
                boolean newMessage = false;
                do {
                    uiDevice.waitForIdle();
                    int size = uiDevice.findObjects(textViewSelector).size();
                    for (int message = 0; (message < 5) && (lastMessageReceivedPosition + 1 + message * 3 < size); message++) {
                        if (!(uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 1 + message * 3).getText())
                                .equals(messageReceivedDate[message])) {
                            newMessage = true;
                            break;
                        }
                    }
                } while (!newMessage);
                if (uiDevice.findObjects(textViewSelector).size() >= lastMessageReceivedPosition + 2) {
                    messageSubject = testUtils.getTextFromTextViewThatContainsText(textInMessage)
                            .equals(uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition).getText());
                    messagePreview = testUtils.getTextFromTextViewThatContainsText(preview)
                            .equals(uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 2).getText());
                }
            } while (!(messageSubject && messagePreview));
        } else {
            while (emptyMessageList) {
                emptyMessageList = uiDevice.findObjects(textViewSelector).size() <= lastMessageReceivedPosition;
            }
        }
    }

    public void clickLastMessageReceived() {
        uiDevice.waitForIdle();
        uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition).click();
    }
}
