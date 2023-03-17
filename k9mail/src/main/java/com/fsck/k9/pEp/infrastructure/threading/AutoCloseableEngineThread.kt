package com.fsck.k9.pEp.infrastructure.threading

import com.fsck.k9.Globals
import com.fsck.k9.K9

class AutoCloseableEngineThread @JvmOverloads constructor(
    target: Runnable?,
    engine: EngineThreadLocal = EngineThreadLocal.getInstance(Globals.getContext() as K9)
) : AutoCloseableThread<EngineThreadLocal>(engine, target)