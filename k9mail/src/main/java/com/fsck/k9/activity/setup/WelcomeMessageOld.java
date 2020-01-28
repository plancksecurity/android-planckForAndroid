package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fsck.k9.K9;
import com.fsck.k9.pEp.ui.fragments.intro.IntroFirstFragment;
import com.fsck.k9.pEp.ui.fragments.intro.IntroSecondFragment;
import com.fsck.k9.pEp.ui.fragments.intro.IntroThirdFragment;
import com.github.paolorotolo.appintro.AppIntro;

import security.pEp.ui.permissions.PermissionsActivity;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class WelcomeMessageOld extends AppIntro {

    public static void showWelcomeMessage(Context context) {
        Intent intent = new Intent(context, WelcomeMessageOld.class);
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
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        nextAction();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        nextAction();
    }

    private void nextAction() {
        if (K9.isShallRequestPermissions()) {
            PermissionsActivity.actionAskPermissions(this);
        } else {
            AccountSetupBasics.actionNewAccount(this);
        }
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
