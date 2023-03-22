package com.fsck.k9.pEp

import com.fsck.k9.pEp.infrastructure.threading.PEpDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

interface DispatcherProvider {

    fun main(): CoroutineDispatcher = Dispatchers.Main
    fun default(): CoroutineDispatcher = Dispatchers.Default
    fun io(): CoroutineDispatcher = Dispatchers.IO
    fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
    fun pEpDispatcher(): CoroutineDispatcher = PEpDispatcher

}

class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider