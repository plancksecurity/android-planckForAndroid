package com.fsck.k9.planck.infrastructure.modules;


import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.planck.DefaultDispatcherProvider;
import com.fsck.k9.planck.DispatcherProvider;
import com.fsck.k9.planck.PEpProvider;
import com.fsck.k9.planck.infrastructure.threading.JobExecutor;
import com.fsck.k9.planck.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.planck.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.planck.infrastructure.threading.UIThread;
import com.fsck.k9.planck.ui.fragments.PEpSettingsCheck;
import com.fsck.k9.planck.ui.fragments.PEpSettingsChecker;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import security.planck.permissions.PermissionChecker;
import security.planck.ui.permissions.PlanckPermissionChecker;

@Module
public class ApplicationModule {

    @Provides
    @Singleton
    @Named("AppContext")
    Context provideContext(K9 application) {
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
    public PEpSettingsChecker providepEpSettingsCheck(
            K9 application,
            ThreadExecutor jobExecutor,
            UIThread uiThread
    ) {
        return new PEpSettingsCheck(application, jobExecutor, uiThread);
    }

    //FIXME Reorganize modules, to avoid duplicating dependencies! (this are here and on pEpModule
    @Provides
    public PermissionChecker providepEpPermissionChecker(@Named("AppContext") Context context) {
        return new PlanckPermissionChecker(context);
    }

    @Provides
    public Preferences providePreferences(K9 application) {
        return Preferences.getPreferences(application);
    }

    @Provides
    public PEpProvider providepEpProvider(K9 application) {
        return application.getpEpProvider();
    }

    @Provides
    @Singleton
    public DispatcherProvider provideDispatcherProvider() { return new DefaultDispatcherProvider(); }

}
