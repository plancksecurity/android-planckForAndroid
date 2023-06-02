package com.fsck.k9.planck.infrastructure.modules;


import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.planck.DefaultDispatcherProvider;
import com.fsck.k9.planck.DispatcherProvider;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.infrastructure.threading.JobExecutor;
import com.fsck.k9.planck.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.planck.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.planck.infrastructure.threading.UIThread;
import com.fsck.k9.planck.ui.fragments.PlanckSettingsCheck;
import com.fsck.k9.planck.ui.fragments.PlanckSettingsChecker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.android.qualifiers.ApplicationContext;
import security.planck.permissions.PermissionChecker;
import security.planck.ui.permissions.PlanckPermissionChecker;

@Module
public class ApplicationModule {

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
    public PlanckSettingsChecker providepEpSettingsCheck(
            K9 application,
            ThreadExecutor jobExecutor,
            UIThread uiThread
    ) {
        return new PlanckSettingsCheck(application, jobExecutor, uiThread);
    }

    //FIXME Reorganize modules, to avoid duplicating dependencies! (this are here and on pEpModule
    @Provides
    public PermissionChecker providepEpPermissionChecker(@ApplicationContext Context context) {
        return new PlanckPermissionChecker(context);
    }

    @Provides
    public Preferences providePreferences(@ApplicationContext Context application) {
        return Preferences.getPreferences(application);
    }

    @Provides
    public PlanckProvider providepEpProvider(K9 application) {
        return application.getPlanckProvider();
    }

    @Provides
    @Singleton
    public DispatcherProvider provideDispatcherProvider() { return new DefaultDispatcherProvider(); }

    @Provides
    public PlanckUIArtefactCache provideUiCache(K9 application) {
        return PlanckUIArtefactCache.getInstance(application);
    }

    @Provides
    @ApplicationContext
    Context provideAppContext(K9 k9) {
        return k9.getApplicationContext();
    }

}
