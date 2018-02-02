package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
public class ConfigureSeveralAccountsRemoveThemAndSetUpNewOne {

    private UiDevice device;
    private TestUtils testUtils;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @Test
    public void ConfigureAccountRemoveThemSetUpNewAccount(){
        testUtils.createAccount(false);
        int total = 4;
        for (int account = 0; account < total; account++){
            device.waitForIdle();
            testUtils.pressBack();
            device.waitForIdle();
            testUtils.selectFromMenu(R.string.add_account_action);
            device.waitForIdle();
            testUtils.newEmailAccount("email" + account, "password");
            testUtils.accountDescription("desciption" + account, "username" + account);
        }
        device.pressBack();
        for (int account = 0; account < total; account++){
            testUtils.removeLastAccount();
        }
        createNewEmailAccount();
    }
    void createNewEmailAccount() {
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.add_account_action);
        device.waitForIdle();
        onView(withId(R.id.account_email)).perform(typeText("email"));
        onView(withId(R.id.account_password)).perform(typeText("password"), closeSoftKeyboard());
        onView(withId(R.id.manual_setup)).perform(click());
        testUtils.fillImapData();
        onView(withId(R.id.next)).perform(click());
        device.waitForIdle();
        device.pressBack();
        device.waitForIdle();
        device.pressBack();
        device.waitForIdle();
        onView(withId(R.id.next)).perform(click());
    }
}
