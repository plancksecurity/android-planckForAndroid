package com.fsck.k9.pEp.infrastructure.modules

import dagger.Binds
import dagger.Module
import security.pEp.mdm.FakeRestrictionsManager
import security.pEp.mdm.RestrictionsManagerContract
import javax.inject.Singleton

@Suppress("unused")
@Module
interface TestRestrictionsManagerModule {

    @Binds
    @Singleton
    fun bindsRestrictionsManager(
        fakeRestrictionsManager: FakeRestrictionsManager
    ): RestrictionsManagerContract
}
