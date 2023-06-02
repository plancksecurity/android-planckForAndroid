package com.fsck.k9.planck.infrastructure.components;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.AlternateRecipientAdapter;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.compose.RecipientSelectView;
import com.fsck.k9.planck.PlanckActivity;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.infrastructure.modules.ApplicationModule;
import com.fsck.k9.planck.infrastructure.modules.RestrictionsProviderModule;
import com.fsck.k9.planck.infrastructure.modules.SubComponentsModule;
import com.fsck.k9.planck.ui.activities.provisioning.ProvisioningActivity;
import com.fsck.k9.planck.ui.fragments.PlanckFragment;
import com.fsck.k9.view.MessageHeader;

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
        SubComponentsModule.class,
        RestrictionsProviderModule.class
})
public interface ApplicationComponent {

    void inject(K9Activity k9Activity);

    void inject(PlanckFragment planckFragment);

    void inject(PlanckActivity activity);
    PlanckComponent.Factory planckComponentFactory();

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

    K9 getK9();

    // TODO: 05/05/2020 check if this belongs here.
    void inject(MessageHeader messageHeader);

    void inject(RecipientSelectView recipientSelectView);

    void inject(AlternateRecipientAdapter alternateRecipientAdapter);

    void inject(ProvisioningActivity activity);
}
