package security.pEp.ui.support.export

import android.app.Activity
import android.content.Context
import android.os.Environment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.fsck.k9.activity.misc.NonConfigurationInstance
import com.fsck.k9.pEp.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ExportpEpSupportDataPresenter @Inject constructor(
    private val exportpEpSupportData: ExportpEpSupportData,
) : LifecycleObserver, NonConfigurationInstance {
    private lateinit var view: ExportpEpSupportDataView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sdf = SimpleDateFormat("yyyyMMdd-HH:MM", Locale.getDefault())
    private var state: ExportPEpDatabasesState = ExportPEpDatabasesState.Initial
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

    fun renderState(state: ExportPEpDatabasesState = this.state) {
        this.state = state
        runWithLifecycleSafety {
            when (state) {
                is ExportPEpDatabasesState.Initial -> {
                    // NOP
                }
                is ExportPEpDatabasesState.Exporting -> {
                    view.showLoading()
                }
                is ExportPEpDatabasesState.Success -> {
                    view.hideLoading()
                    view.showSuccess()
                }
                is ExportPEpDatabasesState.Failed -> {
                    view.hideLoading()
                    if (state.cause is NotEnoughSpaceInDeviceException) {
                        view.showNotEnoughSpaceInDevice(
                            state.cause.neededSpace / 1024,
                            state.cause.availableSpace / 1024,
                        )
                    } else {
                        view.showFailed()
                    }
                }
            }
        }
    }

    fun export() {
        scope.launch {
            renderState(ExportPEpDatabasesState.Exporting)
            exportInternal()
                .onSuccess { success ->
                    if (success) {
                        renderState(ExportPEpDatabasesState.Success)
                    } else {
                        renderState(ExportPEpDatabasesState.Failed())
                    }
                }.onFailure {
                    renderState(ExportPEpDatabasesState.Failed(it))
                }
        }
    }

    fun cancel() {
        view.finish()
    }

    private suspend fun exportInternal(): Result<Boolean> {
        val context = view.getContext()
        val toFolder = context.getExternalFilesDir(
            "${Environment.DIRECTORY_DOCUMENTS}/pEp/db-export/${sdf.format(Date())}"
        )
        val homeDir = context.getDir("home", Context.MODE_PRIVATE)
        val pEpFolder = File(homeDir, ".pEp")
        val trustwordsFolder = context.getDir("trustwords", Context.MODE_PRIVATE)
        return toFolder?.let {
            exportpEpSupportData(listOf(pEpFolder, trustwordsFolder), toFolder)
        } ?: Result.success(false)
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

sealed class ExportPEpDatabasesState {
    object Initial : ExportPEpDatabasesState()
    object Exporting : ExportPEpDatabasesState()
    object Success : ExportPEpDatabasesState()
    class Failed(val cause: Throwable? = null) : ExportPEpDatabasesState()
}
