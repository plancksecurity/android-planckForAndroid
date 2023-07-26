package com.fsck.k9.planck.infrastructure.modules;

import android.app.Activity;

import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.planck.infrastructure.ComposeView;
import com.fsck.k9.planck.infrastructure.MessageView;
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import security.planck.permissions.PermissionRequester;
import security.planck.ui.permissions.PlanckPermissionRequester;
import security.planck.ui.resources.PlanckResourcesProvider;
import security.planck.ui.resources.ResourcesProvider;
import security.planck.ui.toolbar.ToolBarCustomizer;

@Module
@InstallIn(ActivityComponent.class)
public class ActivityModule {

   @Provides
    PermissionRequester providepEpPermissionRequestProvider(Activity activity) {
        return new PlanckPermissionRequester(activity);
    }

    @Provides
    ToolBarCustomizer provideToolbarCustomizer(Activity activity) {
        return new ToolBarCustomizer(activity);
    }

    @Provides
    public ResourcesProvider providepEpResourcesProvider(Activity activity) {
        return new PlanckResourcesProvider(activity);
    }

    @Provides
    @ComposeView
    public DisplayHtml provideDisplayHtmlForCompose(DisplayHtmlUiFactory factory) {
        return factory.createForMessageCompose();
    }

    @Provides
    @MessageView
    public DisplayHtml provideDisplayHtmlForMessageView(DisplayHtmlUiFactory factory) {
        return factory.createForMessageView();
    }
}
