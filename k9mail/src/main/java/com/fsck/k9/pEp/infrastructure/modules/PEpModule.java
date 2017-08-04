package com.fsck.k9.pEp.infrastructure.modules;

import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;

import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsCheck;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsChecker;

import dagger.Module;
import dagger.Provides;

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
    public PEpSettingsChecker providePEpSettingsCheck() {
        return new PEpSettingsCheck(context.getApplicationContext());
    }
}
