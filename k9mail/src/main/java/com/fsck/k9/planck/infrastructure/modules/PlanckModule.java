package com.fsck.k9.planck.infrastructure.modules;


import android.content.Context;

import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.migration.DisableInstallInCheck;

@Module
@DisableInstallInCheck
public class PlanckModule {
    @Provides
    public SimpleMessageLoaderHelper providesSimpleMessageLoaderHelper(
            @ActivityContext Context context,
            LoaderManager loaderManager,
            FragmentManager fragmentManager
    ) {
        return new SimpleMessageLoaderHelper(context, loaderManager, fragmentManager);
    }

    @Provides
    public MessagingController provideMessagingController(@ActivityContext Context context) {
        return MessagingController.getInstance(context);
    }
}
