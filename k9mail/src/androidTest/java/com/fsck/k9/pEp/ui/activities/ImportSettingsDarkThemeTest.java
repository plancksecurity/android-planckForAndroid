package com.fsck.k9.pEp.ui.activities;

import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.intent.Checks;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiCollection;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiSelector;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import com.fsck.k9.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;


public class ImportSettingsDarkThemeTest {

    private TestUtils testUtils;
    private UiDevice device;


    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        testUtils.externalAppRespondWithFile(R.raw.settingsthemelight);
        testUtils.startActivity();
    }

    @Test
    public void importSettingDarkTheme(){
        testUtils.pressBack();
        testUtils.openOptionsMenu();
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.import_export_action);
        device.waitForIdle();
        testUtils.selectFromMenu(R.string.settings_import);
        turnOnCheckBoxAndOffTheOther(R.string.settings_import_global_settings);
        testUtils.selectAcceptButton();
        device.waitForIdle();
        testUtils.selectAcceptButton();
        device.waitForIdle();
        onView(withId(R.id.accounts_list)).perform(ViewActions.click());
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UiObject2 uo = selectLayout();
        uo.getText();
        //onView(withId(R.id.message_swipe)).check(matches(withBackgroundColor(R.color.white)));
        //onView(selectLayout()).check(matches(withBackgroundColor(R.color.white)));

      /*  UiCollection list = new UiCollection( new UiSelector().className("android.widget.LinearLayout"));
        list.getChild(new UiSelector().index(5)).*/

    }
    private UiObject2 selectLayout(){
        BySelector selector = By.clazz("android.widget.LinearLayout");
        if (device.findObjects(selector).get(5) != null){
            return device.findObjects(selector).get(5);
        }
        return device.findObjects(selector).get(5);
    }

    private void turnOnCheckBoxAndOffTheOther(int resourceOn){
        BySelector selector = By.clazz("android.widget.CheckedTextView");
        device.waitForIdle();
        for (UiObject2 checkBox : device.findObjects(selector))
            {device.waitForIdle();
            if (checkBox.getText().equals(InstrumentationRegistry.getTargetContext().getResources().getString(resourceOn))){
                if (!checkBox.isChecked()){
                    checkBox.click();
                }
            }else{
                if (checkBox.isChecked()){
                    checkBox.longClick();
                }
            }
        }
        device.waitForIdle();
    }

    public static Matcher<View> withBackgroundColor(final int color) {
        Checks.checkNotNull(color);
        int color1 = ContextCompat.getColor(getTargetContext(),color);
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public boolean matchesSafely(View view) {
                int color2 = ((ColorDrawable) view.getBackground()).getColor();
                return color1 == (color2);
            }
            @Override
            public void describeTo(Description description) {

            }
        };
    }
}
