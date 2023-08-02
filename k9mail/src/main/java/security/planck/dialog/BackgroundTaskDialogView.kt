package security.planck.dialog

interface BackgroundTaskDialogView {
    fun showState(state: State)

    enum class State {
        CONFIRMATION,
        LOADING,
        ERROR,
        SUCCESS
    }
}