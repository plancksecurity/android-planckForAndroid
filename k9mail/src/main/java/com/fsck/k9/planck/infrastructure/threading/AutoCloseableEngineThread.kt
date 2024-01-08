package com.fsck.k9.planck.infrastructure.threading

import com.fsck.k9.Globals
import com.fsck.k9.K9

open class AutoCloseableEngineThread(
    target: Runnable?,
) : Thread(target) {
    override fun run() {
        EngineThreadLocal.getInstance(Globals.getContext() as K9).use {
            super.run()
        }
    }
}