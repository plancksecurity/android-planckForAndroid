package com.fsck.k9.planck.infrastructure.modules

import android.content.Context
import android.content.RestrictionsManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import security.planck.mdm.PlanckRestrictions
import security.planck.mdm.RestrictionsProvider
import javax.inject.Named

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
            @Named("AppContext") application: Context
        ): RestrictionsManager {
            return application.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        }

        @Provides
        @JvmStatic
        fun providePackageName(
            @Named("AppContext") application: Context
        ): String {
            return application.packageName
        }
    }
}
