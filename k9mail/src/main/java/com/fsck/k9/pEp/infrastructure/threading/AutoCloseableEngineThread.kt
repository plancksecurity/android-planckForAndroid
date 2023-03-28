package com.fsck.k9.pEp.infrastructure.threading

import com.fsck.k9.Globals
import com.fsck.k9.K9

open class AutoCloseableEngineThread(
    engine: EngineThreadLocal,
    target: Runnable?,
) : AutoCloseableThread<EngineThreadLocal>(engine, target) {
    constructor(target: Runnable?) : this(
        EngineThreadLocal.getInstance(Globals.getContext() as K9),
        target
    )
}