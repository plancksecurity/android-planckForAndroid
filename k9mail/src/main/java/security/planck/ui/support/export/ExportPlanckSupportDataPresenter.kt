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

const val SUPPORT_EXPORT_TARGET_SUBFOLDER = "planck/db-export"

class ExportPlanckSupportDataPresenter @Inject constructor(
    private val exportPlanckSupportData: ExportPlanckSupportData,
) : LifecycleObserver, NonConfigurationInstance {
    private lateinit var view: ExportPlanckSupportDataView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sdf = SimpleDateFormat("yyyyMMdd-HH_mm", Locale.getDefault())
    private var state: ExportPlanckDataState = ExportPlanckDataState.Initial
    private lateinit var lifecycle: Lifecycle

    fun initialize(
        view: ExportPlanckSupportDataView,
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

    fun renderState(state: ExportPlanckDataState = this.state) {
        this.state = state
        runWithLifecycleSafety {
            when (state) {
                is ExportPlanckDataState.Initial -> {
                    // NOP
                }
                is ExportPlanckDataState.Exporting -> {
                    view.showLoading()
                }
                is ExportPlanckDataState.Succeeded -> {
                    view.hideLoading()
                    view.showSuccess()
                }
                is ExportPlanckDataState.Failed -> {
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
            renderState(ExportPlanckDataState.Exporting)
            exportInternal()
                .onSuccess {
                    renderState(ExportPlanckDataState.Succeeded)
                }.onFailure {
                    renderState(ExportPlanckDataState.Failed(it))
                }
        }
    }

    fun cancel() {
        view.finish()
    }

    private suspend fun exportInternal(): Result<Unit> {
        val documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val subFolder = "$SUPPORT_EXPORT_TARGET_SUBFOLDER/${sdf.format(Date())}"
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

sealed class ExportPlanckDataState {
    object Initial : ExportPlanckDataState()
    object Exporting : ExportPlanckDataState()
    object Succeeded : ExportPlanckDataState()
    class Failed(val cause: Throwable) : ExportPlanckDataState()
}
