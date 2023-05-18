package com.fsck.k9.planck.infrastructure.threading

open class AutoCloseableThread<T : AutoCloseable>(
    private val closeable: T,
    target: Runnable?
) : Thread(target) {
    override fun run() {
        closeable.use {
            super.run()
        }
    }
}