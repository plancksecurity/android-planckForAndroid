package com.fsck.k9.planck

import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

interface DispatcherProvider {

    fun main(): CoroutineDispatcher = Dispatchers.Main
    fun default(): CoroutineDispatcher = Dispatchers.Default
    fun io(): CoroutineDispatcher = Dispatchers.IO
    fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
    fun planckDispatcher(): CoroutineDispatcher = PlanckDispatcher

}

class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider