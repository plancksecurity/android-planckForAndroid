package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fsck.k9.pEp.PEpPermissionChecker;
import com.fsck.k9.pEp.ui.activities.PermissionsActivity;
import com.fsck.k9.pEp.ui.fragments.intro.IntroFirstFragment;
import com.fsck.k9.pEp.ui.fragments.intro.IntroSecondFragment;
import com.fsck.k9.pEp.ui.fragments.intro.IntroThirdFragment;
import com.github.paolorotolo.appintro.AppIntro;

import security.pEp.sync.permissions.PermissionChecker;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class WelcomeMessage extends AppIntro {

    private PermissionChecker pEpPermissionChecker;

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
        pEpPermissionChecker = new PEpPermissionChecker(getApplicationContext());
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
        if (pEpPermissionChecker.hasBasicPermission()) {
            AccountSetupBasics.actionNewAccount(this);
        } else {
            PermissionsActivity.actionAskPermissions(this);
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
