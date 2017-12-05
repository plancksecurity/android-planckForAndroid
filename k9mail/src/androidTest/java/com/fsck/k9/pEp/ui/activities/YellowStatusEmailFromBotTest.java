package com.fsck.k9.pEp.ui.activities;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.Checks;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasTextColor;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;


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
        /*greyStatusEmailSend();
        waitForBotEmail();*/
        clickBotEmail();
        clickReplayMessage();
        clickMailStatus();
        checkBotEmailColor();
        //yellowStatusEmailTest();
    }

    private void checkBotEmailColor() {
        testUtils.doWaitForResource(R.id.toolbar_container);
        testUtils.doWait();
        onView(allOf(withId(R.id.toolbar_container))).check(matches(withBgColor(R.color.pep_yellow)));
        testUtils.doWait();
        //assertTrue(withId(R.id.toolbar_container).matches(withLayoutColor(R.color.pep_yellow)));
        //onView(allOf(withId(R.id.toolbar_container)))
        //        .check(matches(hasTextColor(R.color.white)));
        //onView(withId(R.id.recipientContainer)).check(matches(hasTextColor(R.color.pep_yellow)));
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
        //uiDevice.findObjects(selector).get(lastEmailRecivedPosition).click();
        uiDevice.findObjects(selector).get(3).click();
    }

    private void waitForBotEmail() {
        BySelector selector = By.clazz("android.widget.TextView");
        while ((testUtils.getTextFromTextviewThatContainsText("bot").equals(uiDevice.findObjects(selector).get(lastEmailRecivedPosition).getText()))
                 &&(lastEmailRecivedDate.equals(uiDevice.findObjects(selector).get(lastEmailRecivedPosition+1).getText()))
                // &&(lastEmailRecivedFor == uiDevice.findObjects(selector).get(lastEmailRecivedPosition+2).getText())
                ){
            testUtils.doWait();
        }
    }

    private void greyStatusEmailSend() {
        emailFrom = testUtils.getTextFromTextviewThatContainsText("@");
        lastEmailRecivedPosition = getLastEmailRecivedPosition();
        BySelector selector = By.clazz("android.widget.TextView");
        onView(withId(R.id.message_list))
                .perform(swipeDown());
        lastEmailRecivedSubject = uiDevice.findObjects(selector).get(lastEmailRecivedPosition).getText();
        lastEmailRecivedDate = uiDevice.findObjects(selector).get(lastEmailRecivedPosition+1).getText();
        //lastEmailRecivedFor = uiDevice.findObjects(selector).get(lastEmailRecivedPosition+2).getText();
        testUtils.composseMessageButton();
        testUtils.doWait();
        testUtils.fillEmail(emailTo, "Subject", "Message", false);
        testUtils.sendEmail();
        testUtils.doWait();

    }

    public int getLastEmailRecivedPosition(){
        BySelector selector = By.clazz("android.widget.TextView");
        int size = uiDevice.findObjects(selector).size();
        int i = 0;
        for (; i < size; i++) {
            uiDevice.findObjects(selector).get(i);
            if (uiDevice.findObjects(selector).get(i).getText() != null && uiDevice.findObjects(selector).get(i).getText().contains("@")){
                i++;
                while (uiDevice.findObjects(selector).get(i).getText() == null){
                    i++;
                }
                return i;
            }
        }
        return i;
    }

    private void yellowStatusEmailTest() {
    }

    public static Matcher<View> withLayoutColor(final int color) {
        Checks.checkNotNull(color);
        return new BoundedMatcher<View, LinearLayout>(LinearLayout.class) {
            @Override
            protected boolean matchesSafely(LinearLayout item) {
                return ContextCompat.getColor(getTargetContext(),color)==item.getSolidColor();
            }
            @Override
            public void describeTo(Description description) {
            }
        };
    }

    public static Matcher<View> withBgColor(final int color) {
        Checks.checkNotNull(color);
        return new BoundedMatcher<View, LinearLayout>(LinearLayout.class) {
            @Override
            public boolean matchesSafely(LinearLayout row) {
                return color == ((ColorDrawable) row.getBackground()).getColor();
            }
            @Override
            public void describeTo(Description description) {
                description.appendText("nothing");
            }
        };
    }
}
