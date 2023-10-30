package security.planck.timer

interface Timer {
    fun startOrReset(timeout: Long, block: () -> Unit)
    fun cancel()
}