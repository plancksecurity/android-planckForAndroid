package com.fsck.k9.pEp.ui.activities;


import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables;
import android.support.test.espresso.intent.Checks;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.privacy.status.PEpTrustwords;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.pEp.jniadapter.Rating;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class BackButtonDeviceAfterHandshakeButtonPressedTest {

    private static final String HOST = "test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String messageTo;
    private String lastMessageReceivedDate;
    private int lastMessageReceivedPosition;
    private BySelector textViewSelector;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice);
        testUtils.increaseTimeoutWait();
        textViewSelector = By.clazz("android.widget.TextView");
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        //messageTo = "test35" + "@" + HOST;
        testUtils.startActivity();
    }

    @Test
    public void backButtonDeviceAfterHandshakeButtonPressed(){
        //testUtils.createAccount(false);
        sendMessages();
        uiDevice.waitForIdle();
        clickLastMessageReceived();
        assertMessageStatus(Rating.pEpRatingReliable.value);
        uiDevice.waitForIdle();
        onView(withId(R.id.handshake_button_text)).perform(click());
        uiDevice.waitForIdle();
        onView(withId(R.id.confirmTrustWords)).perform(click());
        testUtils.pressBack();
        testUtils.pressBack();
    }

    private void clickLastMessageReceived() {
        uiDevice.waitForIdle();
        uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition).click();
    }

    public void sendMessages() {
        uiDevice.waitForIdle();
        for (int messages = 0; messages < 3; messages ++) {
            getLastMessageReceived();
            testUtils.composeMessageButton();
            uiDevice.waitForIdle();
            testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
            testUtils.sendMessage();
            uiDevice.waitForIdle();
            waitForMessageWithText("bot", "bot (" + messageTo + ")");
        }
    }

    private void assertMessageStatus(int status) {
        uiDevice.waitForIdle();
        clickMessageStatus();
        uiDevice.waitForIdle();
        onView(withId(R.id.pEpTitle)).check(matches(withText(testUtils.getResourceString(R.array.pep_title, status))));
    }

    private void clickMessageStatus() {
        uiDevice.waitForIdle();
        onView(withId(R.id.tvPep)).perform(click());
        uiDevice.waitForIdle();
    }

    private void waitForMessageWithText(String textInMessage, String preview) {
        boolean messageSubject = false;
        boolean messageDate = false;
        boolean messagePreview = false;
        boolean emptyMessageList;
        do {
            emptyMessageList = uiDevice.findObjects(textViewSelector).size() <= lastMessageReceivedPosition;
            if (!emptyMessageList) {
                messageSubject = testUtils.getTextFromTextViewThatContainsText(textInMessage)
                        .equals(uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition).getText());
                messageDate = (!(lastMessageReceivedDate
                        .equals(uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 1).getText())))
                        || ((uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 1).getText())
                        .equals(uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 4).getText()));
                messagePreview = testUtils.getTextFromTextViewThatContainsText(preview)
                        .equals(uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 2).getText());
            }
            uiDevice.waitForIdle();
        } while (!(!(emptyMessageList)
                && (messageSubject && messageDate && messagePreview)));
    }

    public static UtilsPackage.RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {

        return new UtilsPackage.RecyclerViewMatcher(recyclerViewId);
    }

    public int getLastMessageReceivedPosition() {
        int size = uiDevice.findObjects(textViewSelector).size();
        for (int position = 0; position < size; position++) {
            String textAtPosition = uiDevice.findObjects(textViewSelector).get(position).getText();
            if (textAtPosition != null && textAtPosition.contains("@")) {
                position++;
                while (uiDevice.findObjects(textViewSelector).get(position).getText() == null) {
                    position++;
                    if (position >= size) {
                        return -1;
                    }
                }
                return position;
            }
        }
        return size;
    }

    public static Matcher<View> withBackgroundColor(final int color) {
        Checks.checkNotNull(color);
        int color1 = ContextCompat.getColor(getTargetContext(), color);
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public boolean matchesSafely(View view) {
                int color2 = ((ColorDrawable) view.getBackground()).getColor();
                return color1 == (color2);
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    private void getLastMessageReceived() {
        uiDevice.waitForIdle();
        lastMessageReceivedPosition = getLastMessageReceivedPosition();
        onView(withId(R.id.message_list))
                .perform(swipeDown());
        if (lastMessageReceivedPosition != -1) {
            lastMessageReceivedDate = uiDevice.findObjects(textViewSelector).get(lastMessageReceivedPosition + 1).getText();
        } else {
            lastMessageReceivedDate = "";
            lastMessageReceivedPosition = uiDevice.findObjects(textViewSelector).size();
        }
    }

    public void assertCurrentActivityIsInstanceOf(Class<? extends Activity> activityClass) {
        Activity currentActivity = getCurrentActivity();
        checkNotNull(currentActivity);
        checkNotNull(activityClass);
        assertTrue(currentActivity.getClass().isAssignableFrom(activityClass));
    }

    private Activity getCurrentActivity() {
        final Activity[] activity = new Activity[1];
        try {
            splashActivityTestRule.runOnUiThread(() -> {
                java.util.Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                activity[0] = Iterables.getOnlyElement(activities);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return activity[0];
    }
}
