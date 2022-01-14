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
import javax.inject.Named

class ExportpEpSupportDataPresenter @Inject constructor(
    @Named("AppContext") private val context: Context,
    private val supportDataExporter: PEpSupportDataExporter,
) : LifecycleObserver, NonConfigurationInstance {
    private lateinit var view: ExportpEpSupportDataView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sdf = SimpleDateFormat("yyyyMMdd-HH:MM", Locale.getDefault())
    private var step: ExportPEpDatabasesStep = ExportPEpDatabasesStep.Initial
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
        renderStep()
    }

    fun renderStep(step: ExportPEpDatabasesStep = this.step) {
        this.step = step
        runWithLifecycleSafety {
            when (step) {
                is ExportPEpDatabasesStep.Initial -> {
                    // NOP
                }
                is ExportPEpDatabasesStep.Exporting -> {
                    view.showLoading()
                }
                is ExportPEpDatabasesStep.Success -> {
                    view.hideLoading()
                    view.showSuccess()
                }
                is ExportPEpDatabasesStep.Failed -> {
                    view.hideLoading()
                    if (step.cause is NotEnoughSpaceInDeviceException) {
                        view.showNotEnoughSpaceInDevice(
                            step.cause.neededSpace / 1024,
                            step.cause.availableSpace / 1024,
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
            renderStep(ExportPEpDatabasesStep.Exporting)
            exportInternal()
                .onSuccess { success ->
                    if (success) {
                        renderStep(ExportPEpDatabasesStep.Success)
                    } else {
                        renderStep(ExportPEpDatabasesStep.Failed())
                    }
                }.onFailure {
                    renderStep(ExportPEpDatabasesStep.Failed(it))
                }
        }
    }

    fun cancel() {
        view.finish()
    }

    private suspend fun exportInternal(): Result<Boolean> {
        val toFolder = context.getExternalFilesDir(
            "${Environment.DIRECTORY_DOCUMENTS}/pEp/db-export/${sdf.format(Date())}"
        )
        val homeDir = context.getDir("home", Context.MODE_PRIVATE)
        val fromFolder = File(homeDir, ".pEp")
        return toFolder?.let {
            supportDataExporter.export(fromFolder, toFolder)
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

sealed class ExportPEpDatabasesStep {
    object Initial : ExportPEpDatabasesStep()
    object Exporting : ExportPEpDatabasesStep()
    object Success : ExportPEpDatabasesStep()
    class Failed(val cause: Throwable? = null) : ExportPEpDatabasesStep()
}
