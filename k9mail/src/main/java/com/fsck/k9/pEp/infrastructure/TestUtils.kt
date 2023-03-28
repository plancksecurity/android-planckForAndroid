package com.fsck.k9.pEp.infrastructure

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun assertWithTimeout(timeout: Long, step: Long = timeout/10, assertion: () -> Unit) {
    val initialTime = System.currentTimeMillis()
    do {
        try {
            assertion()
            return
        } catch (error: Throwable) {
            runBlocking { delay(step) }
        }
    } while (System.currentTimeMillis() - initialTime < timeout)
    assertion()
}