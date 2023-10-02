package com.fsck.k9.planck.infrastructure

import android.os.Handler
import android.os.Looper
import javax.inject.Inject

class PollerFactory @Inject constructor() {
    fun createPoller(): Poller = Poller(Handler(Looper.getMainLooper()))
}