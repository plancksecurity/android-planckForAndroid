package com.fsck.k9.pEp.infrastructure.modules;


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
import com.fsck.k9.pEp.ui.fragments.PEpSettingsCheck;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsChecker;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import security.pEp.permissions.PermissionChecker;
import security.pEp.ui.permissions.PEpPermissionChecker;

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
        return new PEpPermissionChecker(context);
    }

    @Provides
    public Preferences providePreferences(K9 application) {
        return Preferences.getPreferences(application);
    }

    @Provides
    @Named("MainUI")
    public PEpProvider providepEpProvider(K9 application) {
        return application.getpEpProvider();
    }

    @Provides
    @Singleton
    public DispatcherProvider provideDispatcherProvider() { return new DefaultDispatcherProvider(); }

}
