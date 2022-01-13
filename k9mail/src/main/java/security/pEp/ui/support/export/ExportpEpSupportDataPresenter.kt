package security.pEp.ui.support.export

import android.content.Context
import android.os.Environment
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
) {
    private lateinit var view: ExportpEpSupportDataView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sdf = SimpleDateFormat("yyyyMMdd-HH:MM", Locale.getDefault())

    fun initialize(view: ExportpEpSupportDataView) {
        this.view = view
    }

    fun export() {
        scope.launch {
            view.showLoading()
            exportSuspend().also { success ->
                view.hideLoading()
                if (success) {
                    view.showSuccess()
                } else {
                    view.showFailed()
                }
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
}
