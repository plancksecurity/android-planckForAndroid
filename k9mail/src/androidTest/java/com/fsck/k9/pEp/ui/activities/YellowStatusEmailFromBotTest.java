package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
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
import android.view.View;

import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.privacy.status.PEpTrustwords;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
public class YellowStatusEmailFromBotTest {

    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String emailTo = "test2@test.pep-security.net";
    private String emailFrom;
    private String lastEmailRecivedSubject;
    private String lastEmailRecivedFor;
    private String lastEmailRecivedDate;
    private int lastEmailRecivedPosition;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice);
        testUtils.startActivity();
    }

    @Test
    public void yellowStatusEmail() {
        testUtils.increaseTimeoutWait();
        getLastEmailRecived();
        testUtils.composseMessageButton();
        testUtils.doWait();
        testUtils.fillEmail(emailTo, "Subject", "Message", false);
        testUtils.sendEmail();
        testUtils.doWait();
        waitForBotEmail();
        clickBotEmail();
        clickReplayMessage();
        clickMailStatus();
        checkBotEmailColor();
        testUtils.doWait();
        testUtils.pressBack();
        testUtils.doWait();
        testUtils.pressBack();
        testUtils.doWait();
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        testUtils.doWait();
        testUtils.pressBack();
        testUtils.doWait();
        testUtils.composseMessageButton();
        yellowStatusEmailTest();
    }

    private void checkBotEmailColor() {
        testUtils.doWaitForResource(R.id.toolbar_container);
        testUtils.doWait();
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(R.color.pep_yellow)));
    }

    private void clickMailStatus() {
        testUtils.doWait();
        onView(withId(R.id.pEp_indicator)).perform(click());
    }

    private void clickReplayMessage() {
        testUtils.doWait();
        onView(withId(R.id.reply_message)).perform(click());
    }

    private void clickBotEmail() {
        testUtils.doWait();
        BySelector selector = By.clazz("android.widget.TextView");
        uiDevice.findObjects(selector).get(lastEmailRecivedPosition).click();
        //uiDevice.findObjects(selector).get(3).click();
    }

    private void waitForBotEmail() {
        BySelector selector = By.clazz("android.widget.TextView");
        while ((uiDevice.findObjects(selector).size() <= lastEmailRecivedPosition)
                ||(testUtils.getTextFromTextviewThatContainsText("bot").equals(uiDevice.findObjects(selector).get(lastEmailRecivedPosition).getText()))
                 &&(lastEmailRecivedDate.equals(uiDevice.findObjects(selector).get(lastEmailRecivedPosition+1).getText()))
                // &&(lastEmailRecivedFor == uiDevice.findObjects(selector).get(lastEmailRecivedPosition+2).getText())
                ){
            testUtils.doWait();
        }
    }

    private void getLastEmailRecived() {
        emailFrom = testUtils.getTextFromTextviewThatContainsText("@");
        lastEmailRecivedPosition = getLastEmailRecivedPosition();
        BySelector selector = By.clazz("android.widget.TextView");
        onView(withId(R.id.message_list))
                .perform(swipeDown());
        if (lastEmailRecivedPosition != -1) {
            lastEmailRecivedSubject = uiDevice.findObjects(selector).get(lastEmailRecivedPosition).getText();
            lastEmailRecivedDate = uiDevice.findObjects(selector).get(lastEmailRecivedPosition + 1).getText();
        }else{
            lastEmailRecivedSubject = "";
            lastEmailRecivedDate = "";
            lastEmailRecivedPosition = uiDevice.findObjects(selector).size();
        }
    }

    public int getLastEmailRecivedPosition(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = uiDevice.findObjects(selector).size();
        for (int position = 0; position < size; position++) {
             uiDevice.findObjects(selector).get(position);
            if (uiDevice.findObjects(selector).get(position).getText() != null && uiDevice.findObjects(selector).get(position).getText().contains("@")){
                position++;
                while (uiDevice.findObjects(selector).get(position).getText() == null){
                    position++;
                    if (position >= size){
                        position = -1;
                        return position;
                    }
                }
                return position;
            }
        }
        return size;
    }

    private void yellowStatusEmailTest() {
        testUtils.fillEmail(emailTo, "Subject", "Message", false);
        onView(withId(R.id.pEp_indicator)).perform(click());
        testUtils.doWait();
        onView(withId(R.id.my_recycler_view)).check(doesNotExist());
        assertCurrentActivityIsInstanceOf(PEpTrustwords.class);

    }

    public static Matcher<View> withBackgroundColor(final int color) {
        Checks.checkNotNull(color);
        int color1 = ContextCompat.getColor(getTargetContext(),color);
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public boolean matchesSafely(View view) {
                int color2 = ((ColorDrawable) view.getBackground()).getColor();
                if (color1 == (color2)){
                    return true;
                }else {
                    return false;
                }
            }
            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public void assertCurrentActivityIsInstanceOf(Class<? extends Activity> activityClass) {
        final Activity[] activity = new Activity[1];
        try {
            splashActivityTestRule.runOnUiThread(new Runnable() {
                                       public void run() {
                                           java.util.Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                                           activity[0] = Iterables.getOnlyElement(activities);
                                       }
                                   });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        Activity currentActivity = activity[0];
        checkNotNull(currentActivity);
        checkNotNull(activityClass);
        assertTrue(currentActivity.getClass().isAssignableFrom(activityClass));
    }
}
