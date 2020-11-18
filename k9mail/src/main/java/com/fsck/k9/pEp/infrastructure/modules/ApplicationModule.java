package com.fsck.k9.pEp.infrastructure.modules;


import android.app.Application;
import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.pEp.DefaultDispatcherProvider;
import com.fsck.k9.pEp.DispatcherProvider;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.threading.JobExecutor;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.infrastructure.threading.UIThread;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import security.pEp.permissions.PermissionChecker;
import security.pEp.ui.permissions.PEpPermissionChecker;

@Module
public class ApplicationModule {

    private final K9 application;

    public ApplicationModule(K9 application) {
        this.application = application;
    }

    @Provides
    @Singleton
    @Named("AppContext")
    Context provideContext() {
        return application.getApplicationContext();
    }

    @Provides
    @Singleton
    ThreadExecutor provideThreadExecutor(JobExecutor jobExecutor) {
        return jobExecutor;
    }

    @Provides
    @Singleton
    PostExecutionThread providePostExecutionThread(UIThread uiThread) {
        return uiThread;
    }

    //FIXME Reorganize modules, to avoid duplicating dependencies! (this are here and on pEpModule
    @Provides
    public PermissionChecker providepEpPermissionChecker() {
        return new PEpPermissionChecker(application.getApplicationContext());
    }

    @Provides
    public Preferences providePreferences() {
        return Preferences.getPreferences(application);
    }

    @Provides
    @Named("MainUI")
    public PEpProvider providepEpProvider() {
        return application.getpEpProvider();
    }

    @Provides
    @Singleton
    public DispatcherProvider provideDispatcherProvider() { return new DefaultDispatcherProvider(); }

}
