package security.planck.ui.support.export

import android.app.Activity
import android.os.Environment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.fsck.k9.activity.misc.NonConfigurationInstance
import com.fsck.k9.planck.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ExportPlanckSupportDataPresenter @Inject constructor(
    private val exportPlanckSupportData: ExportPlanckSupportData,
) : LifecycleObserver, NonConfigurationInstance {
    private lateinit var view: ExportpEpSupportDataView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sdf = SimpleDateFormat("yyyyMMdd-HH_mm", Locale.getDefault())
    private var state: ExportpEpDataState = ExportpEpDataState.Initial
    private lateinit var lifecycle: Lifecycle

    fun initialize(
        view: ExportpEpSupportDataView,
        lifecycle: Lifecycle,
    ) {
        this.view = view
        this.lifecycle = lifecycle
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    @Suppress("unused")
    private fun showCurrentScreen() {
        renderState()
    }

    fun renderState(state: ExportpEpDataState = this.state) {
        this.state = state
        runWithLifecycleSafety {
            when (state) {
                is ExportpEpDataState.Initial -> {
                    // NOP
                }
                is ExportpEpDataState.Exporting -> {
                    view.showLoading()
                }
                is ExportpEpDataState.Succeeded -> {
                    view.hideLoading()
                    view.showSuccess()
                }
                is ExportpEpDataState.Failed -> {
                    view.hideLoading()
                    Timber.e(state.cause)
                    if (state.cause is NotEnoughSpaceInDeviceException) {
                        view.showNotEnoughSpaceInDevice()
                    } else {
                        view.showFailed()
                    }
                }
            }
        }
    }

    fun export() {
        scope.launch {
            renderState(ExportpEpDataState.Exporting)
            exportInternal()
                .onSuccess {
                    renderState(ExportpEpDataState.Succeeded)
                }.onFailure {
                    renderState(ExportpEpDataState.Failed(it))
                }
        }
    }

    fun cancel() {
        view.finish()
    }

    private suspend fun exportInternal(): Result<Unit> {
        val documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val subFolder = "pEp/db-export/${sdf.format(Date())}"
        return exportPlanckSupportData(documentsFolder, subFolder)
    }

    private fun runWithLifecycleSafety(block: () -> Unit) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            block()
        }
    }

    override fun retain(): Boolean {
        return true
    }

    override fun restore(activity: Activity?) {
        // NOP
    }
}

sealed class ExportpEpDataState {
    object Initial : ExportpEpDataState()
    object Exporting : ExportpEpDataState()
    object Succeeded : ExportpEpDataState()
    class Failed(val cause: Throwable) : ExportpEpDataState()
}
