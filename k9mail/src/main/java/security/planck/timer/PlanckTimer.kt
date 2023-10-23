package security.planck.timer

import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.concurrent.schedule


class PlanckTimer(
    private val timer: Timer = Timer(),
) : security.planck.timer.Timer {
    @Inject
    @Suppress("unused")
    constructor() : this(Timer())

    private var currentTimerTask: TimerTask? = null

    override fun startOrReset(timeout: Long, block: () -> Unit) {
        cancel()
        start(timeout, block)
    }

    private fun start(timeout: Long, block: () -> Unit) {
        currentTimerTask = timer.schedule(timeout) { block() }
    }

    override fun cancel() {
        currentTimerTask?.cancel()
        currentTimerTask = null
    }
}