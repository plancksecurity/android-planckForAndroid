package com.fsck.k9.planck.infrastructure.modules

import android.content.Context
import android.content.RestrictionsManager
import com.fsck.k9.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import security.planck.mdm.FakeRestrictionsManager
import security.planck.mdm.PlanckRestrictions
import security.planck.mdm.RestrictionsProvider

@Suppress("unused")
@Module
class TestRestrictionsProviderModule {

    @Provides
    fun provideRestrictionsProvider(
        fakeRestrictionsManager: FakeRestrictionsManager,
        pEpRestrictions: PlanckRestrictions,
    ): RestrictionsProvider {
        return if (BuildConfig.USE_FAKE_RESTRICTIONS_MANAGER) {
            fakeRestrictionsManager
        } else {
            pEpRestrictions
        }
    }

    @Provides
    fun provideSystemRestrictionsManager(
        @ApplicationContext application: Context
    ): RestrictionsManager {
        return application.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    }

    @Provides
    fun providePackageName(
        @ApplicationContext application: Context
    ): String {
        return application.packageName
    }
}
