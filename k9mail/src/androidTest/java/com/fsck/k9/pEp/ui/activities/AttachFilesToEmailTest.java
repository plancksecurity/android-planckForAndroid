package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AttachFilesToEmailTest {

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
        attachFilesToAccount(false);
    }

    private void attachFilesToAccount(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(isGmail);
        testUtils.composeMessageButton();
        testUtils.fillMessage(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL), true);
        testUtils.sendMessage();
        testUtils.pressBack();
        testUtils.removeLastAccount();
    }

}
