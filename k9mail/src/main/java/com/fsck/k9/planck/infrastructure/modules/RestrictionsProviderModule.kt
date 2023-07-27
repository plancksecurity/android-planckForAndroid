package com.fsck.k9.planck.infrastructure.modules

import android.content.Context
import android.content.RestrictionsManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import security.planck.mdm.PlanckRestrictions
import security.planck.mdm.RestrictionsProvider

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface RestrictionsProviderModule {

    @Binds
    fun bindsRestrictionsProvider(
        planckRestrictions: PlanckRestrictions
    ): RestrictionsProvider

    companion object {
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
}
