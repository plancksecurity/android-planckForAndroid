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
        textViewSelector = By.clazz("android.widget.TextView");
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
        testUtils.doWaitForResource(R.id.pEp_indicator);
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
        goToSentFolder();
        device.waitForIdle();
        waitForTextOnScreen(resources.getString(R.string.special_mailbox_name_sent));
        device.waitForIdle();
        device.findObjects(textViewSelector).get(lastMessageReceivedPosition).click();  //Se sale de rango (indice 3 y size 0)
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.tvPep);
        device.waitForIdle();
        boolean end = false;
        do {
            try {
                testUtils.assertMessageStatus(Rating.pEpRatingUnencrypted.value);  //No encuentra la vista
                end = true;
            }catch (Exception e){

            }
        }while (!end);
        device.waitForIdle();
        testUtils.pressBack();

    }

    private void goToSentFolder() {
        testUtils.openOptionsMenu();
        device.waitForIdle();
        boolean end = false;
        do {
            try {
                testUtils.selectFromMenu(R.string.account_settings_folders);    //Selecciona un objeto null
                end = true;
            }catch (Exception e){

            }
        }while (!end);
            device.waitForIdle();
            String folder = resources.getString(R.string.special_mailbox_name_sent);
        for (UiObject2 textView : device.findObjects(textViewSelector)) {
            try {
                if (textView.findObject(textViewSelector).getText() != null && textView.findObject(textViewSelector).getText().contains(folder)) {
                    device.waitForIdle();
                    textView.findObject(textViewSelector).click();      //Selecciona la carpeta Archivos
                    device.waitForIdle();
                    return;
                }
                device.waitForIdle();
            }catch (Exception e){
            }
        }
    }

    private void waitForTextOnScreen(String text){
        boolean textIsOk = false;
        do{
            device.waitForIdle();
            try {
                textIsOk = testUtils.getTextFromTextViewThatContainsText(text).contains(resources.getString(R.string.special_mailbox_name_sent));
            }catch (Exception e){

            }
        }while (!textIsOk);
    }
}

