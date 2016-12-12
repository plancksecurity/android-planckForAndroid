package com.fsck.k9.pEp.infrastructure.modules;

import android.app.Activity;

import com.fsck.k9.pEp.infrastructure.PerActivity;

import dagger.Module;
import dagger.Provides;

@Module
public class ActivityModule {
    private final Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Provides
    @PerActivity
    Activity provideActivity() {
        return this.activity;
    }
}
