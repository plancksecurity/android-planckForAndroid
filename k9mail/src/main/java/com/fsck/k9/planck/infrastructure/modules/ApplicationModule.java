package com.fsck.k9.planck.infrastructure.modules;


import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.auth.OAuthConfigurationsKt;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.NamedThreadFactory;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.oauth.OAuthConfigurationProvider;
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
import com.fsck.k9.preferences.Storage;

import net.openid.appauth.AuthState;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import security.planck.audit.AuditLogger;
import security.planck.permissions.PermissionChecker;
import security.planck.ui.permissions.PlanckPermissionChecker;

@Module
@InstallIn(SingletonComponent.class)
@SuppressWarnings("unused")
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
    public Storage provideStorage(Preferences preferences) {
        return preferences.getStorage();
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
    K9 provideK9(Application context) {
        return (K9) context;
    }

    @Provides
    IntentFilter provideNewIntentFilter() { return new IntentFilter(); }

    @Provides
    public ExecutorService provideSettingsThreadExecutor() {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory("SaveSettings"));
    }

    @Provides
    @Singleton
    public StorageManager provideStorageManager(@ApplicationContext Context application) {
        return StorageManager.getInstance(application);
    }

    @Provides
    public Resources provideAppResources(@ApplicationContext Context application) {
        return application.getResources();
    }

    @Provides
    public TransportProvider provideTransportProvider() {
        return TransportProvider.getInstance();
    }

    @Provides
    public MessagingController provideMessagingController(@ApplicationContext Context application) {
        return MessagingController.getInstance(application);
    }

    @Provides
    @Singleton
    public OAuthConfigurationProvider provideOAuthConfigurationProvider() {
        return OAuthConfigurationsKt.createOAuthConfigurationProvider();
    }

    @Provides
    public AuthState provideAuthState() {
        return new AuthState();
    }

    @Provides
    public AuditLogger provideAuditLogger(K9 k9, PlanckProvider planckProvider) {
        return new AuditLogger(
                planckProvider,
                new File(k9.getFilesDir(), AuditLogger.AUDIT_LOGGER_ROUTE),
                k9.getAuditLogDataTimeRetentionValue()
        );
    }
}
