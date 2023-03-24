package com.fsck.k9.pEp.infrastructure.threading

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val DEFAULT_POOL_SIZE = 20
private val AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors()
private val CORE_POOL_SIZE = DEFAULT_POOL_SIZE.coerceAtMost(AVAILABLE_PROCESSORS)
private val MAX_POOL_SIZE = CORE_POOL_SIZE * 2
private const val KEEP_ALIVE_TIME = 1L

val PEpDispatcher by lazy { PEpEnginePool().asCoroutineDispatcher() }

class PEpEnginePool(
    engineThreadFactory: EngineThreadFactory = EngineThreadFactory()
) : ThreadPoolExecutor(
    CORE_POOL_SIZE,
    MAX_POOL_SIZE,
    KEEP_ALIVE_TIME,
    TimeUnit.SECONDS,
    LinkedBlockingQueue(),
    engineThreadFactory,
)

class EngineThreadFactory : ThreadFactory {
    var current = 0

    override fun newThread(r: Runnable?): Thread {
        return AutoCloseableEngineThread(r)
            .apply { name = "PEpDispatcher thread ${++current}" }
    }
}