package com.fsck.k9.pEp.infrastructure.components;

import com.fsck.k9.pEp.infrastructure.PerActivity;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.PEpStatus;
import com.fsck.k9.pEp.ui.PEpTrustwords;
import com.fsck.k9.pEp.ui.pEpAddDevice;

import dagger.Component;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = {
        ActivityModule.class, PEpModule.class
}) public interface PEpComponent extends ActivityComponent {

    void inject(PEpStatus pEpStatus);

    void inject(PEpTrustwords pEpTrustwords);

    void inject(pEpAddDevice pEpStatus);
}