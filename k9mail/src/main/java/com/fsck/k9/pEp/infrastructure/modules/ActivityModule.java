package com.fsck.k9.pEp.infrastructure.modules;

import android.app.Activity;
import android.content.Context;

import com.fsck.k9.pEp.infrastructure.PerActivity;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import security.pEp.permissions.PermissionRequester;
import security.pEp.ui.permissions.PepPermissionRequester;
import security.pEp.ui.toolbar.PEpToolbarCustomizer;
import security.pEp.ui.toolbar.ToolBarCustomizer;

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

    @Provides
    @PerActivity
    @Named("ActivityContext")
    Context provideActivityContext() {
        return this.activity;
    }

    @Provides
    PermissionRequester providepEpPermissionRequestProvider() {
        return new PepPermissionRequester(activity);
    }

    @Provides
    ToolBarCustomizer providepEpToolbarCustomizer() {
        return new PEpToolbarCustomizer(activity);
    }
}
