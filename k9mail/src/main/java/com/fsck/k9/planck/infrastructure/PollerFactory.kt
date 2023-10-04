package com.fsck.k9.planck.infrastructure

import android.os.Handler
import android.os.Looper
import javax.inject.Inject

/**
 * PollerFactory
 *
 * Factory that creates [Poller] objects.
 */
class PollerFactory @Inject constructor() {
    /**
     * createPoller
     *
     * Create a new instance of [Poller].
     */
    fun createPoller(): Poller = Poller(Handler(Looper.getMainLooper()))
}