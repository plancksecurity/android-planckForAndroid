package security.pEp.ui.support.export

import android.content.Context
import android.os.Environment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class ExportpEpSupportDataPresenter @Inject constructor(
    @Named("AppContext") private val context: Context,
) : LifecycleObserver {
    private lateinit var view: ExportpEpSupportDataView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sdf = SimpleDateFormat("yyyyMMdd-HH:MM", Locale.getDefault())
    private var step = ExportPEpDatabasesStep.INITIAL
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
                ExportPEpDatabasesStep.INITIAL -> {
                    // NOP
                }
                ExportPEpDatabasesStep.EXPORTING -> {
                    view.showLoading()
                }
                ExportPEpDatabasesStep.SUCCESS -> {
                    view.hideLoading()
                    view.showSuccess()
                }
                ExportPEpDatabasesStep.FAILED -> {
                    view.hideLoading()
                    view.showFailed()
                }
            }
        }
    }

    fun export() {
        scope.launch {
            renderStep(ExportPEpDatabasesStep.EXPORTING)
            if (exportSuspend()) {
                renderStep(ExportPEpDatabasesStep.SUCCESS)
            } else {
                renderStep(ExportPEpDatabasesStep.FAILED)
            }
        }
    }

    fun cancel() {
        view.finish()
    }

    private suspend fun exportSuspend(): Boolean = withContext(Dispatchers.IO) {
        try {
            val documentsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val toFolder = File(documentsFolder, "pEp/db-export/${sdf.format(Date())}")
            toFolder.mkdirs()

            val homeDir = context.getDir("home", Context.MODE_PRIVATE)
            val fromFolder = File(homeDir, ".pEp")

            copyFolder(fromFolder, toFolder)
            File(toFolder, "management.db").exists() && File(toFolder, "keys.db").exists()
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    private fun copyFolder(fromFolder: File, toFolder: File) {
        FileUtils.copyDirectory(fromFolder, toFolder)
    }

    private fun runWithLifecycleSafety(block: () -> Unit) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            block()
        }
    }
}

enum class ExportPEpDatabasesStep {
    INITIAL, EXPORTING, SUCCESS, FAILED
}
