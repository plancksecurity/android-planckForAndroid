package com.fsck.k9.planck.infrastructure

import javax.inject.Qualifier

@Qualifier
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ComposeView()

@Qualifier
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class MessageView()
