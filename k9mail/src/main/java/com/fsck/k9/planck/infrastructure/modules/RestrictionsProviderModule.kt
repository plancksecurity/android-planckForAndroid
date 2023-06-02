package com.fsck.k9.planck.infrastructure.modules

import android.content.Context
import android.content.RestrictionsManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import security.planck.mdm.PlanckRestrictions
import security.planck.mdm.RestrictionsProvider

@Suppress("unused")
@Module
interface RestrictionsProviderModule {

    @Binds
    fun bindsRestrictionsProvider(
        planckRestrictions: PlanckRestrictions
    ): RestrictionsProvider

    @Module
    companion object {
        @Provides
        @JvmStatic
        fun provideSystemRestrictionsManager(
            @ApplicationContext application: Context
        ): RestrictionsManager {
            return application.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        }

        @Provides
        @JvmStatic
        fun providePackageName(
            @ApplicationContext application: Context
        ): String {
            return application.packageName
        }
    }
}
