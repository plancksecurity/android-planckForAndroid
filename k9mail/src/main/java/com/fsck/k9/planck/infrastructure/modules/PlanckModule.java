package com.fsck.k9.planck.infrastructure.modules;


import android.content.Context;

import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper;

import dagger.Module;
import dagger.Provides;
import security.planck.permissions.PermissionChecker;
import security.planck.ui.permissions.PlanckPermissionChecker;

@Module
public class PlanckModule {
    private final Context context;
    private final LoaderManager loaderManager;
    private final FragmentManager fragmentManager;

    public PlanckModule(Context context, LoaderManager loaderManager, FragmentManager fragmentManager) {
        this.context = context;
        this.loaderManager = loaderManager;
        this.fragmentManager = fragmentManager;
    }

    @Provides
    public SimpleMessageLoaderHelper providesSimpleMessageLoaderHelper() {
        return new SimpleMessageLoaderHelper(context, loaderManager, fragmentManager);
    }

    @Provides
    public PermissionChecker providepEpPermissionChecker() {
        return new PlanckPermissionChecker(context.getApplicationContext());
    }

    @Provides
    public MessagingController provideMessagingController() {
        return MessagingController.getInstance(context);
    }
}
