package com.fsck.k9.planck.infrastructure.modules

import com.fsck.k9.Clock
import com.fsck.k9.RealClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import security.planck.audit.AuditLogger
import security.planck.audit.PlanckAuditLogger
import security.planck.sync.PlanckSyncRepository
import security.planck.sync.SyncRepository
import security.planck.timer.PlanckTimer
import security.planck.timer.Timer

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface ApplicationBindersModule {
    @Binds
    fun bindSyncRepository(repository: PlanckSyncRepository): SyncRepository
    @Binds
    fun bindClock(clock: RealClock): Clock
    @Binds
    fun bindTimer(timer: PlanckTimer): Timer
    @Binds
    fun bindAuditLogger(auditLogger: PlanckAuditLogger): AuditLogger
}