package com.fsck.k9.planck.infrastructure.components;

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
import com.fsck.k9.planck.infrastructure.modules.PEpModule;
import com.fsck.k9.planck.manualsync.ImportWizardFrompEp;
import com.fsck.k9.planck.ui.blacklist.PepBlacklist;
import com.fsck.k9.planck.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupOptionsFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupOutgoingFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupSelectAuthFragment;
import com.fsck.k9.planck.ui.fragments.ChooseAccountTypeFragment;
import com.fsck.k9.planck.ui.keys.PepExtraKeys;
import com.fsck.k9.planck.ui.keysync.KeysyncManagement;
import com.fsck.k9.planck.ui.keysync.PEpAddDevice;
import com.fsck.k9.planck.ui.privacy.status.PEpStatus;
import com.fsck.k9.ui.messageview.MessageViewFragment;

import dagger.Component;
import security.planck.ui.about.AboutActivity;
import security.planck.ui.calendar.CalendarInviteLayout;
import security.planck.ui.intro.IntroFirstFragment;
import security.planck.ui.intro.IntroFourthFragment;
import security.planck.ui.keyimport.KeyImportActivity;
import security.planck.ui.mdm.MdmSettingsFeedbackActivity;
import security.planck.ui.passphrase.PassphraseActivity;
import security.planck.ui.permissions.PermissionsActivity;
import security.planck.ui.support.export.ExportpEpSupportDataActivity;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = {
        ActivityModule.class, PEpModule.class,
})
public interface PEpComponent extends ActivityComponent {

    void inject(PEpStatus activity);

    void inject(PEpAddDevice activity);

    void inject(KeysyncManagement activity);

    void inject(AboutActivity activity);

    void inject(PermissionsActivity activity);

    void inject(PepExtraKeys activity);

    void inject(PepBlacklist activity);

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
}