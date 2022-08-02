package com.fsck.k9.pEp.infrastructure.modules

import dagger.Binds
import dagger.Module
import security.pEp.mdm.FakeRestrictionsManager
import security.pEp.mdm.RestrictionsProvider
import javax.inject.Singleton

@Suppress("unused")
@Module
interface TestRestrictionsProviderModule {

    @Binds
    @Singleton
    fun bindsRestrictionsProvider(
        fakeRestrictionsManager: FakeRestrictionsManager
    ): RestrictionsProvider
}