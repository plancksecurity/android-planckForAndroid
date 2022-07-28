package com.fsck.k9.pEp.infrastructure.modules;


import android.content.Context;
import android.content.RestrictionsManager;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.pEp.DefaultDispatcherProvider;
import com.fsck.k9.pEp.DispatcherProvider;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.threading.JobExecutor;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.infrastructure.threading.UIThread;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsCheck;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsChecker;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import security.pEp.mdm.FakeRestrictionsManager;
import security.pEp.mdm.PEpRestrictionsManager;
import security.pEp.mdm.RestrictionsManagerContract;
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

    @Provides @Singleton
    public PEpSettingsChecker providepEpSettingsCheck(ThreadExecutor jobExecutor, UIThread uiThread) {
        return new PEpSettingsCheck(application, jobExecutor, uiThread);
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
    public RestrictionsManagerContract provideRestrictionsManager() {
        return K9.test
                ? new FakeRestrictionsManager()
                : new PEpRestrictionsManager(
                (RestrictionsManager) application.getSystemService(Context.RESTRICTIONS_SERVICE),
                application.getPackageName()
        );
    }

    @Provides
    @Named("MainUI")
    public PEpProvider providepEpProvider() {
        return application.getpEpProvider();
    }

    @Provides
    @Singleton
    public DispatcherProvider provideDispatcherProvider() { return new DefaultDispatcherProvider(); }

    @Provides
    @Singleton
    public K9 provideK9() { return (K9) application; }

}
