package com.fsck.k9

import javax.inject.Inject

/**
 * An interface to provide the current time.
 */
interface Clock {
    val time: Long
}

class RealClock @Inject constructor() : Clock {
    override val time: Long
        get() = System.currentTimeMillis()
}
