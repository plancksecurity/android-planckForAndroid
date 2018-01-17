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
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class YellowStatusEmailFromBotTest {

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "random@test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private BySelector selector;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        selector = By.clazz("android.widget.TextView");
        testUtils.startActivity();
    }

    @Test
    public void sendMessageAndAssertYellowStatusMessage() {
        testUtils.createAccount(false);
        testUtils.getLastMessageReceived();
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForMessageWithText("p≡p", "p≡pbot (" + messageTo + ")");
        testUtils.clickLastMessageReceived();
        clickReplayMessage();
        clickMailStatus();
        checkBotMessageColor();
        goBackToMessageList();
        testUtils.composeMessageButton();
        yellowStatusMessageTest();
    }

    @Test
    public void twoStatusMessageYellowAndGray() {
        testUtils.composeMessageButton();
        fillComposeFields();
        clickMailStatus();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(0)).check(matches(withBackgroundColor(R.color.pep_yellow)));
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(1)).check(matches(withBackgroundColor(R.color.pep_no_color)));
    }

    private void fillComposeFields() {
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.to)).perform(typeText("randomtest@Message.is"), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(longClick(), closeSoftKeyboard());
        device.waitForIdle();
    }

    public static RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {

        return new RecyclerViewMatcher(recyclerViewId);
    }

    private void goBackToMessageList() {
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
    }

    private void checkBotMessageColor() {
        testUtils.doWaitForResource(R.id.toolbar_container);
        device.waitForIdle();
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(R.color.pep_yellow)));
    }

    private void clickMailStatus() {
        device.waitForIdle();
        onView(withId(R.id.pEp_indicator)).perform(click());
        device.waitForIdle();
    }

    private void clickReplayMessage() {
        device.waitForIdle();
        onView(withId(R.id.reply_message)).perform(click());
    }

    public int getLastMessageReceivedPosition() {
        int size = device.findObjects(selector).size();
        for (int position = 0; position < size; position++) {
            String textAtPosition = device.findObjects(selector).get(position).getText();
            if (textAtPosition != null && textAtPosition.contains("@")) {
                position++;
                while (device.findObjects(selector).get(position).getText() == null) {
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
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.pEp_indicator)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.my_recycler_view)).check(doesNotExist());
        assertCurrentActivityIsInstanceOf(PEpTrustwords.class);

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
