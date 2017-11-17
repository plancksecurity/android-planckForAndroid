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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AttachFilesToEmailTest {

    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final String EMAIL = "juan@miau.xyz";

    private UiDevice device;
    private TestUtils testUtils;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.startActivity();
    }

    @Test 
    public void attachFilesToEmail() {
        testUtils.increaseTimeoutWait();
        accountConfiguration();
        testUtils.accountDescription(DESCRIPTION, USER_NAME);
        testUtils.accountListSelect(DESCRIPTION);
        testUtils.composseMessageButton();
        testUtils.fillEmail(EMAIL, "Subject", "Message", true);
        testUtils.sendEmail();
        testUtils.removeAccount("accounts_list");
    }

    private void accountConfiguration(){
        onView(withId(R.id.skip)).perform(click());
        testUtils.newEmailAccount();
        //testUtils.gmailAccount();
    }

}
