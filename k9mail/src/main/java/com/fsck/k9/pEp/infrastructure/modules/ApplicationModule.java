package com.fsck.k9.pEp.infrastructure.modules;


import android.content.Context;

import com.fsck.k9.K9;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private final K9 application;

    public ApplicationModule(K9 application) {
        this.application = application;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return application;
    }
}
