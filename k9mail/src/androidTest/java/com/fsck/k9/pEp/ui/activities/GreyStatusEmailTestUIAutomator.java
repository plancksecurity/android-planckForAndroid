package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class GreyStatusEmailTestUIAutomator {

    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final String EMAIL = "newemail@mail.es";

    private TestUtils testUtils;

    @Before
    public void startMainActivity() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.startActivity();
    }

    @Test
    public void greyStatusEmail(){
        greyStatusEmailTest(false);
    }

  /*  @Test
    public void attachFilesToGmail() {
        attachFilesToAccount(true);
    }*/

    public void greyStatusEmailTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        onView(withId(R.id.skip)).perform(click());
        if (isGmail) {
            testUtils.gmailAccount();
        } else {
            testUtils.newEmailAccount();
        }
        testUtils.accountDescription(DESCRIPTION, USER_NAME);
        testUtils.accountListSelect(DESCRIPTION);
        testUtils.composseMessageButton();
        testUtils.testStatusEmpty();
        testUtils.testStatusMail(EMAIL, "Subject", "Message", Rating.pEpRatingUnencrypted.value);
        testUtils.testStatusMail("", "", "", Rating.pEpRatingUndefined.value);
        testUtils.testStatusMail(EMAIL, "Subject", "Message", Rating.pEpRatingUnencrypted.value);
        testUtils.sendEmail();
        testUtils.pressBack();
        testUtils.removeAccount("accounts_list");
    }
}
