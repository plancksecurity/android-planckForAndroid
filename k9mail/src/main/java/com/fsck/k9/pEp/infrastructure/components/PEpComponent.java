package com.fsck.k9.pEp.infrastructure.components;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.SettingsActivity;
import com.fsck.k9.activity.setup.AccountSetupAccountType;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.pEp.filepicker.SelectPathFragment;
import com.fsck.k9.pEp.infrastructure.PerActivity;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.manualsync.ImportWizardFrompEp;
import security.pEp.ui.about.AboutActivity;
import security.pEp.ui.about.LicenseActivity;
import com.fsck.k9.pEp.ui.blacklist.PepBlacklist;
import com.fsck.k9.pEp.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupCheckSettingsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragmentLegacy;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOptionsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOutgoingFragment;
import com.fsck.k9.pEp.ui.fragments.ChooseAccountTypeFragment;
import com.fsck.k9.pEp.ui.keys.PepExtraKeys;
import security.pEp.ui.keyimport.KeyImportActivity;
import com.fsck.k9.pEp.ui.keysync.KeysyncManagement;
import com.fsck.k9.pEp.ui.keysync.PEpAddDevice;
import com.fsck.k9.pEp.ui.privacy.status.PEpStatus;
import com.fsck.k9.ui.messageview.MessageViewFragment;
import com.fsck.k9.ui.settings.account.removeaccount.RemoveAccountActivity;

import dagger.Component;
import security.pEp.ui.intro.IntroFirstFragment;
import security.pEp.ui.intro.IntroFourthFragment;
import security.pEp.ui.passphrase.PassphraseActivity;
import security.pEp.ui.permissions.PermissionsActivity;
import security.pEp.ui.support.export.ExportpEpSupportDataActivity;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = {
        ActivityModule.class, PEpModule.class,
})
public interface PEpComponent extends ActivityComponent {

    void inject(PEpStatus activity);

    void inject(PEpAddDevice activity);

    void inject(KeysyncManagement activity);

    void inject(AboutActivity activity);

    void inject(LicenseActivity activity);

    void inject(PermissionsActivity activity);

    void inject(PepExtraKeys activity);

    void inject(PepBlacklist activity);

    void inject(AccountSetupNames activity);

    void inject(AccountSetupAccountType activity);

    void inject(AccountSetupBasicsFragment accountSetupBasicsFragment);

    void inject(AccountSetupIncomingFragment accountSetupIncomingFragment);

    void inject(MessageViewFragment fragment);

    void inject(AccountSetupIncomingFragmentLegacy accountSetupIncomingFragment);

    void inject(AccountSetupOutgoingFragment accountSetupOutgoingFragment);

    void inject(AccountSetupCheckSettingsFragment fragment);

    void inject(AccountSetupBasics accountSetupBasics);

    void inject(AccountSetupOptionsFragment accountSetupOptionsFragment);

    void inject(MessageList activity);

    void inject(SelectPathFragment selectPathFragment);

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

    void inject(RemoveAccountActivity activity);
}