package com.fsck.k9.pEp.infrastructure.modules;


import android.content.Context;

import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.manualsync.ImportKeyController;
import com.fsck.k9.pEp.manualsync.ImportKeyControllerFactory;
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsCheck;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsChecker;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import security.pEp.permissions.PermissionChecker;
import security.pEp.ui.permissions.PEpPermissionChecker;
import security.pEp.ui.resources.PEpResourcesProvider;
import security.pEp.ui.resources.ResourcesProvider;

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
    public PEpSettingsChecker providepEpSettingsCheck() {
        return new PEpSettingsCheck(context.getApplicationContext());
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
    @Named("Background")
    public PEpProvider providepEpProviderBackground() {
        return MessagingController.getInstance(context).getpEpProvider();
    }

    @Provides
    public MessagingController provideMessagingController() {
        return MessagingController.getInstance(context);
    }

    @Provides
    public ImportKeyController provideImportkeyController(@Named("Background") PEpProvider pEp) {
        return ImportKeyControllerFactory.getInstance().getImportKeyController(context, pEp);
    }

    @Provides
    public Preferences providePreferences() {
        return Preferences.getPreferences(context);
    }

    @Provides
    public ResourcesProvider providepEpResourcesProvider() {
        return new PEpResourcesProvider(context.getApplicationContext());
    }

}
