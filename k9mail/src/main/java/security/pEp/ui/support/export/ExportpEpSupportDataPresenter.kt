package security.pEp.ui.support.export

import android.app.Activity
import android.content.Context
import android.os.Environment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.fsck.k9.activity.misc.NonConfigurationInstance
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
            if (exportInternal()) {
                renderStep(ExportPEpDatabasesStep.SUCCESS)
            } else {
                renderStep(ExportPEpDatabasesStep.FAILED)
            }
        }
    }

    fun cancel() {
        view.finish()
    }

    private suspend fun exportInternal(): Boolean {
        val documentsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val toFolder = File(documentsFolder, "pEp/db-export/${sdf.format(Date())}")
        val homeDir = context.getDir("home", Context.MODE_PRIVATE)
        val fromFolder = File(homeDir, ".pEp")

        return supportDataExporter.export(fromFolder, toFolder)
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

enum class ExportPEpDatabasesStep {
    INITIAL, EXPORTING, SUCCESS, FAILED
}
