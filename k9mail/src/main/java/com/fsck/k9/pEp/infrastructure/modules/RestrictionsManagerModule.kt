package com.fsck.k9.pEp.infrastructure.modules

import android.content.Context
import android.content.RestrictionsManager
import com.fsck.k9.K9
import dagger.Module
import dagger.Provides
import security.pEp.mdm.PEpRestrictionsManager
import security.pEp.mdm.RestrictionsManagerContract


@Module
class RestrictionsManagerModule(private val application: K9) {

    @Provides
    fun provideRestrictionsManager(): RestrictionsManagerContract {
        return PEpRestrictionsManager(
            application.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager,
            application.packageName
        )
    }
}
