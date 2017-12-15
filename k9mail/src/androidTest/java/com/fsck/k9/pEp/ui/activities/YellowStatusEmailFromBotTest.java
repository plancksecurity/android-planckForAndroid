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
public class YellowStatusEmailFromBotTest {

    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String emailTo = "random@test.pep-security.net";
    private String lastMessageReceivedDate;
    private int lastMessageReceivedPosition;
    private BySelector selector;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice);
        testUtils.increaseTimeoutWait();
        selector = By.clazz("android.widget.TextView");
        testUtils.startActivity();
    }

    @Test
    public void sendMessageAndAssertYellowStatusMessage() {
        getLastMessageRecived();
        testUtils.composseMessageButton();
        uiDevice.waitForIdle();
        testUtils.fillEmail(emailTo, "Subject", "Message", false);
        testUtils.sendEmail();
        uiDevice.waitForIdle();
        waitForBotMessage();
        clickBotMessage();
        clickReplayMessage();
        clickMailStatus();
        checkBotMessageColor();
        goBackToMessageList();
        testUtils.composseMessageButton();
        yellowStatusMessageTest();
    }

    @Test
    public void twoStatusMessageYellowAndGray() {
        testUtils.composseMessageButton();
        fillComposeFields();
        clickMailStatus();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        uiDevice.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(0)).check(matches(withBackgroundColor(R.color.pep_no_color)));
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(1)).check(matches(withBackgroundColor(R.color.pep_yellow)));
    }

    private void fillComposeFields() {
        uiDevice.waitForIdle();
        testUtils.fillEmail(emailTo, "Subject", "Message", false);
        onView(withId(R.id.to)).perform(typeText("randomtest@Message.is"), closeSoftKeyboard());
        uiDevice.waitForIdle();
        onView(withId(R.id.subject)).perform(longClick(), closeSoftKeyboard());
        uiDevice.waitForIdle();
    }

    public static RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {

        return new RecyclerViewMatcher(recyclerViewId);
    }

    private void goBackToMessageList() {
        uiDevice.waitForIdle();
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        uiDevice.waitForIdle();
        testUtils.pressBack();
        uiDevice.waitForIdle();
    }

    private void checkBotMessageColor() {
        testUtils.doWaitForResource(R.id.toolbar_container);
        uiDevice.waitForIdle();
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(R.color.pep_yellow)));
    }

    private void clickMailStatus() {
        uiDevice.waitForIdle();
        onView(withId(R.id.pEp_indicator)).perform(click());
        uiDevice.waitForIdle();
    }

    private void clickReplayMessage() {
        uiDevice.waitForIdle();
        onView(withId(R.id.reply_message)).perform(click());
    }

    private void clickBotMessage() {
        uiDevice.waitForIdle();
        uiDevice.findObjects(selector).get(lastMessageReceivedPosition).click();
    }

    private void waitForBotMessage() {
        while ((uiDevice.findObjects(selector).size() <= lastMessageReceivedPosition)
                || (testUtils.getTextFromTextviewThatContainsText("bot").equals(uiDevice.findObjects(selector).get(lastMessageReceivedPosition).getText())
                && (lastMessageReceivedDate.equals(uiDevice.findObjects(selector).get(lastMessageReceivedPosition + 1).getText())))
                ) {
            uiDevice.waitForIdle();
        }
    }

    private void getLastMessageRecived() {
        uiDevice.waitForIdle();
        lastMessageReceivedPosition = getLastMessageReceivedPosition();
        onView(withId(R.id.message_list))
                .perform(swipeDown());
        if (lastMessageReceivedPosition != -1) {
            lastMessageReceivedDate = uiDevice.findObjects(selector).get(lastMessageReceivedPosition + 1).getText();
        } else {
            lastMessageReceivedDate = "";
            lastMessageReceivedPosition = uiDevice.findObjects(selector).size();
        }
    }

    public int getLastMessageReceivedPosition() {
        int size = uiDevice.findObjects(selector).size();
        for (int position = 0; position < size; position++) {
            String textAtPosition = uiDevice.findObjects(selector).get(position).getText();
            if (textAtPosition != null && textAtPosition.contains("@")) {
                position++;
                while (uiDevice.findObjects(selector).get(position).getText() == null) {
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

    private void yellowStatusMessageTest() {
        testUtils.fillEmail(emailTo, "Subject", "Message", false);
        onView(withId(R.id.pEp_indicator)).perform(click());
        uiDevice.waitForIdle();
        onView(withId(R.id.my_recycler_view)).check(doesNotExist());
        assertCurrentActivityIsInstanceOf(PEpTrustwords.class);

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

    public static class RecyclerViewMatcher {
        private final int recyclerViewId;

        RecyclerViewMatcher(int recyclerViewId) {
            this.recyclerViewId = recyclerViewId;
        }

        Matcher<View> atPosition(final int position) {
            return atPositionOnView(position, -1);
        }

        Matcher<View> atPositionOnView(final int position, final int targetViewId) {

            return new TypeSafeMatcher<View>() {
                Resources resources = null;
                View childView;

                public void describeTo(Description description) {
                    String idDescription = Integer.toString(recyclerViewId);
                    if (this.resources != null) {
                        try {
                            idDescription = this.resources.getResourceName(recyclerViewId);
                        } catch (Resources.NotFoundException var4) {
                            idDescription = String.format("%s (resource name not found)",
                                    recyclerViewId);
                        }
                    }

                    description.appendText("with id: " + idDescription);
                }

                public boolean matchesSafely(View view) {

                    this.resources = view.getResources();

                    if (childView == null) {
                        RecyclerView recyclerView =
                                view.getRootView().findViewById(recyclerViewId);
                        if (recyclerView != null && recyclerView.getId() == recyclerViewId) {
                            childView = recyclerView.findViewHolderForAdapterPosition(position).itemView;
                        } else {
                            return false;
                        }
                    }

                    if (targetViewId == -1) {
                        return view == childView;
                    } else {
                        View targetView = childView.findViewById(targetViewId);
                        return view == targetView;
                    }

                }
            };
        }
    }
}
