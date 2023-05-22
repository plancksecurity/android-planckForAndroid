package com.fsck.k9.planck.infrastructure.modules;

import android.app.Activity;
import android.content.Context;

import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.planck.infrastructure.PerActivity;
import com.fsck.k9.planck.infrastructure.ComposeView;
import com.fsck.k9.planck.infrastructure.MessageView;
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import security.planck.permissions.PermissionRequester;
import security.planck.ui.permissions.PlanckPermissionRequester;
import security.planck.ui.resources.PEpResourcesProvider;
import security.planck.ui.resources.ResourcesProvider;
import security.planck.ui.toolbar.PEpToolbarCustomizer;
import security.planck.ui.toolbar.ToolBarCustomizer;

@Module
public class ActivityModule {
    private final Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Provides
    @PerActivity
    Activity provideActivity() {
        return this.activity;
    }

    @Provides
    @PerActivity
    @Named("ActivityContext")
    Context provideActivityContext() {
        return this.activity;
    }

    @Provides
    PermissionRequester providepEpPermissionRequestProvider() {
        return new PlanckPermissionRequester(activity);
    }

    @Provides
    ToolBarCustomizer providepEpToolbarCustomizer() {
        return new PEpToolbarCustomizer(activity);
    }

    @Provides
    public ResourcesProvider providepEpResourcesProvider() {
        return new PEpResourcesProvider(activity);
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
