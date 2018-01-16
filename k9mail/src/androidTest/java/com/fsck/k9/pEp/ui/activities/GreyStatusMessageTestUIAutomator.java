package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class GreyStatusMessageTestUIAutomator {

    private static final String EMAIL = "newemail@mail.es";

    private TestUtils testUtils;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.startActivity();
    }

    @Test
    public void greyStatusMessage(){
        greyStatusMessageTest(false);
    }

    public void greyStatusMessageTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(isGmail);
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                 new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "", "", ""),
                 new TestUtils.BasicIdentity(Rating.pEpRatingUndefined, ""));
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));
        testUtils.pressBack();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.save_or_discard_draft_message_dlg_title);
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        testUtils.pressBack();
        testUtils.removeLastAccount();
    }
}
