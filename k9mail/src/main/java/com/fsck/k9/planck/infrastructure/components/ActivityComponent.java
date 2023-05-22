package com.fsck.k9.planck.infrastructure.components;

import com.fsck.k9.planck.infrastructure.PerActivity;
import com.fsck.k9.planck.infrastructure.modules.ActivityModule;
import com.fsck.k9.ui.messageview.MessageContainerView;

import dagger.Component;
import security.planck.ui.toolbar.ToolBarCustomizer;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {
    ToolBarCustomizer toolbarCustomizer();

    void inject(MessageContainerView messageContainerView);
}
