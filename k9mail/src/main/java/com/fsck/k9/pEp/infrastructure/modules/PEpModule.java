package com.fsck.k9.pEp.infrastructure.modules;


import android.content.Context;

import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProviderFactory;
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import security.pEp.permissions.PermissionChecker;
import security.pEp.ui.permissions.PEpPermissionChecker;

@Module
public class PEpModule {
    private final Context context;
    private final LoaderManager loaderManager;
    private final FragmentManager fragmentManager;

    public PEpModule(Context context, LoaderManager loaderManager, FragmentManager fragmentManager) {
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
        return new PEpPermissionChecker(context.getApplicationContext());
    }

    @Provides
    @Named("MainUI")
    public PEpProvider providepEpProvider() {
        return ((K9) context.getApplicationContext()).getpEpProvider();
    }

    @Provides
    @Named("NewInstance")
    public PEpProvider providepEpProviderNewInstance() {
        return (PEpProviderFactory.createAndSetupProvider(context));
    }

    @Provides
    public MessagingController provideMessagingController() {
        return MessagingController.getInstance(context);
    }
}
