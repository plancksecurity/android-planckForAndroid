package com.fsck.k9.planck.infrastructure.components;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.SettingsActivity;
import com.fsck.k9.activity.setup.AccountSetupAccountType;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.planck.infrastructure.PerActivity;
import com.fsck.k9.planck.infrastructure.modules.ActivityModule;
import com.fsck.k9.planck.infrastructure.modules.PlanckModule;
import com.fsck.k9.planck.manualsync.ImportWizardFrompEp;
import com.fsck.k9.planck.ui.blacklist.PlanckBlacklist;
import com.fsck.k9.planck.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupOptionsFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupOutgoingFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupSelectAuthFragment;
import com.fsck.k9.planck.ui.fragments.ChooseAccountTypeFragment;
import com.fsck.k9.planck.ui.keys.PlanckExtraKeys;
import com.fsck.k9.planck.ui.keysync.KeysyncManagement;
import com.fsck.k9.planck.ui.keysync.PlanckAddDevice;
import com.fsck.k9.planck.ui.privacy.status.PlanckStatus;
import com.fsck.k9.ui.messageview.MessageContainerView;
import com.fsck.k9.ui.messageview.MessageViewFragment;

import javax.inject.Named;

import dagger.BindsInstance;
import dagger.Subcomponent;
import security.planck.group.GroupTestScreen;
import security.planck.ui.about.AboutActivity;
import security.planck.ui.calendar.CalendarInviteLayout;
import security.planck.ui.intro.IntroFirstFragment;
import security.planck.ui.intro.IntroFourthFragment;
import security.planck.ui.keyimport.KeyImportActivity;
import security.planck.ui.mdm.MdmSettingsFeedbackActivity;
import security.planck.ui.passphrase.PassphraseActivity;
import security.planck.ui.permissions.PermissionsActivity;
import security.planck.ui.support.export.ExportpEpSupportDataActivity;
import security.planck.ui.toolbar.ToolBarCustomizer;

@PerActivity
@Subcomponent(modules = {
        ActivityModule.class, PlanckModule.class,
})
public interface PlanckComponent {

    @Subcomponent.Factory
    interface Factory {
        PlanckComponent create(
                @BindsInstance Activity activity,
                @BindsInstance @Named("ActivityContext") Context context,
                @BindsInstance LoaderManager loaderManager,
                @BindsInstance FragmentManager fragmentManager
        );
    }

    ToolBarCustomizer toolbarCustomizer();

    void inject(PlanckStatus activity);

    void inject(PlanckAddDevice activity);

    void inject(KeysyncManagement activity);

    void inject(AboutActivity activity);

    void inject(PermissionsActivity activity);

    void inject(PlanckExtraKeys activity);

    void inject(PlanckBlacklist activity);

    void inject(AccountSetupNames activity);

    void inject(AccountSetupAccountType activity);

    void inject(AccountSetupBasicsFragment accountSetupBasicsFragment);

    void inject(AccountSetupIncomingFragment accountSetupIncomingFragment);

    void inject(MessageViewFragment fragment);

    void inject(AccountSetupOutgoingFragment accountSetupOutgoingFragment);

    void inject(AccountSetupBasics accountSetupBasics);

    void inject(AccountSetupOptionsFragment accountSetupOptionsFragment);

    void inject(MessageList activity);

    void inject(KeyImportActivity activity);

    void inject(ImportWizardFrompEp activity);

    void inject(MessageCompose messageCompose);

    void inject(MessageListFragment fragment);

    void inject(AccountSetupCheckSettings fragment);

    void inject(ChooseAccountTypeFragment fragment);

    void inject(SettingsActivity activity);

    void inject(IntroFirstFragment frag);

    void inject(IntroFourthFragment frag);

    void inject(PassphraseActivity activity);

    void inject(ExportpEpSupportDataActivity activity);

    void inject(CalendarInviteLayout layout);

    void inject(MdmSettingsFeedbackActivity activity);

    void inject(AccountSetupSelectAuthFragment fragment);

    void inject(GroupTestScreen activity);

    void inject(MessageContainerView messageContainerView);
}