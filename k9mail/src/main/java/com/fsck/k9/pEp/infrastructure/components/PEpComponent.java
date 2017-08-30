package com.fsck.k9.pEp.infrastructure.components;

import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.pEp.filepicker.SelectPathFragment;
import com.fsck.k9.pEp.infrastructure.PerActivity;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.About;
import com.fsck.k9.pEp.ui.activities.PermissionsActivity;
import com.fsck.k9.pEp.ui.blacklist.PepBlacklist;
import com.fsck.k9.pEp.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOptionsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOutgoingFragment;
import com.fsck.k9.pEp.ui.keys.PepExtraKeys;
import com.fsck.k9.pEp.ui.keysync.KeysyncManagement;
import com.fsck.k9.pEp.ui.keysync.PEpAddDevice;
import com.fsck.k9.pEp.ui.privacy.status.PEpStatus;
import com.fsck.k9.pEp.ui.privacy.status.PEpTrustwords;

import dagger.Component;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = {
        ActivityModule.class, PEpModule.class
}) public interface PEpComponent extends ActivityComponent {

    void inject(PEpStatus pEpStatus);

    void inject(PEpTrustwords pEpTrustwords);

    void inject(PEpAddDevice pEpStatus);

    void inject(KeysyncManagement pEpStatus);

    void inject(About about);

    void inject(PermissionsActivity permissionsActivity);

    void inject(PepExtraKeys pepExtraKeys);

    void inject(PepBlacklist pepBlacklist);

    void inject(AccountSetupBasicsFragment accountSetupBasicsFragment);

    void inject(AccountSetupIncomingFragment accountSetupIncomingFragment);

    void inject(AccountSetupOutgoingFragment accountSetupOutgoingFragment);

    void inject(AccountSetupBasics accountSetupBasics);

    void inject(AccountSetupOptionsFragment accountSetupOptionsFragment);

    void inject(MessageList messageList);

    void inject(SelectPathFragment selectPathFragment);
}