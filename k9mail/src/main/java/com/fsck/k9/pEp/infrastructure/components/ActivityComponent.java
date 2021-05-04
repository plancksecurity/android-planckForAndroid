package com.fsck.k9.pEp.infrastructure.components;

import com.fsck.k9.pEp.infrastructure.PerActivity;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.ui.messageview.MessageContainerView;

import dagger.Component;
import security.pEp.ui.toolbar.ToolBarCustomizer;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {
    ToolBarCustomizer toolbarCustomizer();

    void inject(MessageContainerView messageContainerView);
}
