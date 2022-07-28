package com.fsck.k9.pEp.infrastructure.modules

import dagger.Module
import dagger.Provides
import security.pEp.mdm.FakeRestrictionsManager
import security.pEp.mdm.RestrictionsManagerContract
import javax.inject.Singleton

@Module
class TestRestrictionsManagerModule {

    @Provides
    @Singleton
    fun provideRestrictionsManager(): RestrictionsManagerContract {
        return FakeRestrictionsManager()
    }
}
