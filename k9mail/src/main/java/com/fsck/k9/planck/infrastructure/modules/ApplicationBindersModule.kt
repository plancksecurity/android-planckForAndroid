package com.fsck.k9.planck.infrastructure.modules

import com.fsck.k9.Clock
import com.fsck.k9.RealClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import security.planck.sync.PlanckSyncRepository
import security.planck.sync.SyncRepository

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface ApplicationBindersModule {
    @Binds
    fun bindSyncRepository(repository: PlanckSyncRepository): SyncRepository
    @Binds
    fun bindClock(clock: RealClock): Clock
}