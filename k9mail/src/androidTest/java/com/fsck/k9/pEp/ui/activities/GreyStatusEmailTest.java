package com.fsck.k9.pEp.ui.activities;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.KeyEvent;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GreyStatusEmailTest {

    public static final int TIME = 2000;
    //Cosntants are capital and static final, for example private static final String DESCRIPTION_NAME=
    private static final String DESCRIPTION = "jj";
    private static final String USER_NAME = "U.U";
    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void greyStatusEmailTest() {
        accountConfiguration();
        accountDescription(DESCRIPTION, USER_NAME);
        accountListSelect(DESCRIPTION);
        composseMessageButton();
        testStatusEmpty();
        //Status:   0 Unkonwn / 3 No secure / 7 Secure & Confident5
        testStatusMail("newemail@mail.es", "Subject", "Message", Rating.pEpRatingUnencrypted.value);
        testStatusMail("", "", "", Rating.pEpRatingUndefined.value);
        testStatusMail("newemail@mail.es", "", "", Rating.pEpRatingUnencrypted.value);
        sendEmail();
    }

    private void testStatusEmpty(){
        checkStatus(Rating.pEpRatingUndefined.value);
    }

    private void testStatusMail(String to, String subject, String message, int status){
        fillEmail(to, subject, message);
        doWait(TIME);
        doWait(TIME);
        checkStatus(status);
    }

    private void accountConfiguration(){
        onView(withId(R.id.skip)).perform(click());
        newEmailAccount();
        //gmailAccount();
    }

    private void newEmailAccount(){
        onView(withId(R.id.account_email)).perform(typeText("juan@miau.xyz"));
        onView(withId(R.id.account_password)).perform(scrollTo(), typeText("9IJNHY6TFC"), closeSoftKeyboard());
        onView(withId(R.id.manual_setup)).perform(click());

        fillImapData();
        onView(withId(R.id.next)).perform(click());
        doWait(TIME);
        fillSmptData();
        doWait(TIME);
        onView(withId(R.id.next)).perform(click());
        doWait(TIME);
        onView(withId(R.id.next)).perform(click());
    }

    private void fillSmptData() {
        fillServerData();
    }

    private void fillImapData() {
        fillServerData();
    }

    private void fillServerData() {
        onView(withId(R.id.account_server)).perform(replaceText("mx1.hostinger.es"));
        onView(withId(R.id.account_username)).perform(replaceText("juan@miau.xyz"));
    }

    private void gmailAccount(){
        onView(withId(R.id.account_oauth2)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.next)).perform(click());
        doWait(TIME);
        onView(withId(R.id.next)).perform(click());
        doWait(TIME);
        onView(withId(R.id.next)).perform(click());
    }

    private void accountDescription(String description, String userName) {
        onView(withId(R.id.account_description)).perform(typeText(description));
        onView(withId(R.id.account_name)).perform(typeText(userName));
        doWait(TIME);
        onView(withId(R.id.done)).perform(click());
    }

    private void accountListSelect(String description){
        doWait(TIME);
        onData(hasToString(startsWith(description))).inAdapterView(withId(R.id.accounts_list)).perform(click());
    }

    private void composseMessageButton(){
        onView(withId(R.id.fab_button_compose_message)).perform(click());
    }

    private void checkStatus(int status){
        onView(withId(R.id.pEp_indicator)).perform(click());
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, status))));
        Espresso.pressBack();
    }

    private void fillEmail(String to, String subject, String message){
        onView(withId(R.id.to)).perform(click());
        onView(withId(R.id.to)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DEL));
        doWait(TIME);
        onView(withId(R.id.to)).perform(typeText(to));
        onView(withId(R.id.subject)).perform(typeText(subject));
        onView(withId(R.id.message_content)).perform(typeText(message));
    }

    private void sendEmail(){
        onView(withId(R.id.send)).perform(click());
    }

    private void doWait(int timeMillis){
        try {
        Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getResourceString(int id, int n) {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        return targetContext.getResources().getStringArray(id)[n];
    }

    @NonNull
    private String getEmail() {
        return BuildConfig.PEP_TEST_EMAIL_ADDRESS;
    }

    @NonNull String getPassword(){ return  BuildConfig.PEP_TEST_EMAIL_PASSWORD;}
}
