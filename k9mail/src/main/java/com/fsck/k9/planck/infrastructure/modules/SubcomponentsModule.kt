package com.fsck.k9.planck.infrastructure.modules

import com.fsck.k9.planck.infrastructure.components.PlanckComponent
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

@Module(subcomponents = [PlanckComponent::class])
@DisableInstallInCheck
interface SubComponentsModule