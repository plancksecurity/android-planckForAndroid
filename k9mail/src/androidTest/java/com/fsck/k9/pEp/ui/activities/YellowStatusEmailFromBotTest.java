package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
public class YellowStatusEmailFromBotTest {

    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String emailTo = "ponaquiloquequieras@test.pep-security.net";
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
        greyStatusEmailTest();
        waitForNewEmail();
        //yellowStatusEmailTest();
    }

    private void waitForNewEmail() {
        BySelector selector = By.clazz("android.widget.TextView");
        while ((lastEmailRecivedSubject == uiDevice.findObjects(selector).get(lastEmailRecivedPosition).getText())
                // &&(lastEmailRecivedDate == uiDevice.findObjects(selector).get(lastEmailRecivedPosition+1).getText())
                // &&(lastEmailRecivedFor == uiDevice.findObjects(selector).get(lastEmailRecivedPosition+2).getText())
                ){
            testUtils.doWait();
        }
    }

    private void greyStatusEmailTest() {
        emailFrom = testUtils.getTextFromTextviewThatContainsText("@");
        lastEmailRecivedPosition = getLastEmailRecivedPosition();
        BySelector selector = By.clazz("android.widget.TextView");
        onView(withId(R.id.message_list))
                .perform(swipeDown());
        lastEmailRecivedSubject = uiDevice.findObjects(selector).get(lastEmailRecivedPosition).getText();
        //lastEmailRecivedDate = uiDevice.findObjects(selector).get(lastEmailRecivedPosition+1).getText();
        //lastEmailRecivedFor = uiDevice.findObjects(selector).get(lastEmailRecivedPosition+2).getText();
        testUtils.composseMessageButton();
        testUtils.testStatusEmpty();
        testUtils.doWait();
        testUtils.testStatusMail(emailTo, "Subject", "Message", Rating.pEpRatingUnencrypted.value);
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
}
