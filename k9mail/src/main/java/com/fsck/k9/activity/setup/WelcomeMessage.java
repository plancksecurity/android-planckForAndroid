package com.fsck.k9.activity.setup;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.fsck.k9.helper.K9AlarmManager;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.fragments.intro.IntroFirstFragment;
import com.fsck.k9.pEp.ui.fragments.intro.IntroSecondFragment;
import com.fsck.k9.pEp.ui.fragments.intro.IntroThirdFragment;
import com.github.paolorotolo.appintro.AppIntro;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class WelcomeMessage extends AppIntro {

    public static void showWelcomeMessage(Context context) {
        Intent intent = new Intent(context, WelcomeMessage.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(new IntroFirstFragment());
        addSlide(new IntroSecondFragment());
        addSlide(new IntroThirdFragment());

        showSkipButton(true);
        setProgressButtonEnabled(true);

        PEpUtils.askForBatteryOptimizationWhiteListing(this);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        AccountSetupBasics.actionNewAccount(this);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        AccountSetupBasics.actionNewAccount(this);
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));

    }
}
