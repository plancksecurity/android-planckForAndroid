package com.fsck.k9.pEp.infrastructure.components;

import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.AlternateRecipientAdapter;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.DispatcherProvider;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.modules.ApplicationModule;
import com.fsck.k9.pEp.infrastructure.modules.RestrictionsProviderModule;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.ui.PepColoredActivity;
import com.fsck.k9.pEp.ui.activities.provisioning.ProvisioningActivity;
import com.fsck.k9.pEp.ui.fragments.PEpFragment;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsChecker;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.activity.compose.RecipientSelectView;
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator;


import javax.inject.Named;
import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import security.planck.mdm.ConfigurationManager;
import security.planck.mdm.RestrictionsProvider;
import security.planck.provisioning.ProvisioningManager;
import security.planck.file.PlanckSystemFileLocator;
import security.planck.provisioning.ProvisioningSettings;

@Singleton
@Component(modules = {ApplicationModule.class, RestrictionsProviderModule.class})
public interface ApplicationComponent {

    void inject(K9Activity k9Activity);

    void inject(PepColoredActivity pepColoredActivity);

    void inject(PEpFragment pEpFragment);

    ThreadExecutor getThreadExecutor();

    PostExecutionThread getPostExecutionThread();

    PEpSettingsChecker settingsChecker();
    AccountSetupNavigator accountSetupNavigator();

    DispatcherProvider dispatcherProvider();

    ProvisioningManager provisioningManager();

    ProvisioningSettings provisioningSettings();

    PlanckSystemFileLocator pEpSystemFileLocator();
    Preferences preferences();
    ConfigurationManager.Factory configurationManagerFactory();
    RestrictionsProvider restrictionsProvider();
    PEpProvider pEpProvider();

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
