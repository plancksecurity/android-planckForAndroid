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
import org.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class StoreDraftEncryptedOnTrustedServersWhenNeverUnprotected {
    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "username@email.com";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @Test
    public void StoreDraftEncryptedOnTrustedServersWhenNeverUnprotected() {
        testUtils.createAccount(false);
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        device.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        testUtils.openOptionsMenu();
        testUtils.selectFromMenu(R.id.is_always_secure);
        device.waitForIdle();
        device.pressBack();
    }
}
