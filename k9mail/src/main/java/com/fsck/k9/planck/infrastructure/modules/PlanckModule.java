package com.fsck.k9.planck.infrastructure.modules;


import android.content.Context;

import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module
public class PlanckModule {
    @Provides
    public SimpleMessageLoaderHelper providesSimpleMessageLoaderHelper(
            @Named("ActivityContext") Context context,
            LoaderManager loaderManager,
            FragmentManager fragmentManager
    ) {
        return new SimpleMessageLoaderHelper(context, loaderManager, fragmentManager);
    }

    @Provides
    public MessagingController provideMessagingController(@Named("ActivityContext") Context context) {
        return MessagingController.getInstance(context);
    }
}
