package com.fsck.k9.planck.infrastructure.components;

import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.AlternateRecipientAdapter;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.compose.RecipientSelectView;
import com.fsck.k9.planck.DispatcherProvider;
import com.fsck.k9.planck.PlanckActivity;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.infrastructure.modules.ApplicationModule;
import com.fsck.k9.planck.infrastructure.modules.RestrictionsProviderModule;
import com.fsck.k9.planck.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.planck.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.planck.ui.activities.provisioning.ProvisioningActivity;
import com.fsck.k9.planck.ui.fragments.PlanckFragment;
import com.fsck.k9.planck.ui.fragments.PlanckSettingsChecker;
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator;
import com.fsck.k9.view.MessageHeader;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import security.planck.file.PlanckSystemFileLocator;
import security.planck.mdm.ConfigurationManager;
import security.planck.mdm.RestrictionsProvider;
import security.planck.provisioning.ProvisioningManager;
import security.planck.provisioning.ProvisioningSettings;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        PlanckComponent.InstallerModule.class,
        RestrictionsProviderModule.class
})
public interface ApplicationComponent {

    void inject(K9Activity k9Activity);

    void inject(PlanckFragment planckFragment);

    ThreadExecutor getThreadExecutor();
    void inject(PlanckActivity activity);
    PlanckComponent.Factory planckComponentFactory();

    PostExecutionThread getPostExecutionThread();

    PlanckSettingsChecker settingsChecker();
    AccountSetupNavigator accountSetupNavigator();

    DispatcherProvider dispatcherProvider();

    ProvisioningManager provisioningManager();

    ProvisioningSettings provisioningSettings();

    PlanckSystemFileLocator pEpSystemFileLocator();
    Preferences preferences();
    ConfigurationManager.Factory configurationManagerFactory();
    RestrictionsProvider restrictionsProvider();
    PlanckProvider pEpProvider();
    PlanckUIArtefactCache planckUiCache();

    @Component.Factory
    interface Factory {
        ApplicationComponent create(@BindsInstance K9 application);
    }

    @Named("AppContext")
    Context getContext();

    K9 getK9();

    // TODO: 05/05/2020 check if this belongs here.
    void inject(MessageHeader messageHeader);

    void inject(RecipientSelectView recipientSelectView);

    void inject(AlternateRecipientAdapter alternateRecipientAdapter);

    void inject(ProvisioningActivity activity);
}
