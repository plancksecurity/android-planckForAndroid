package com.fsck.k9.pEp.ui.activities;

import android.content.Context;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class WrongColorContactInSentItemsWhenDisableProtectionTest {
    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private BySelector textViewSelector;
    private Resources resources;
    private Context context;
    private int lastMessageReceivedPosition;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        textViewSelector = By.clazz("android.widget.ImageButton");
        context = InstrumentationRegistry.getTargetContext();
        resources = context.getResources();
        testUtils.startActivity();
    }

    @Test
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        //testUtils.createAccount(false);
        lastMessageReceivedPosition = testUtils.getLastMessageReceivedPosition();
        testUtils.composeMessageButton();
        device.waitForIdle();
        messageTo = "unkown@user.is";
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_BODY, MESSAGE_SUBJECT, messageTo), false);
        device.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        testUtils.openOptionsMenu();
        testUtils.selectFromMenu(R.string.pep_force_unprotected);
        device.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.sendMessage();

    }

}

