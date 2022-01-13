package security.pEp.ui.support.export

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class PEpSupportDataExporter @Inject constructor() {
    suspend fun export(
        fromFolder: File,
        toFolder: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            toFolder.mkdirs()
            copyFolder(fromFolder, toFolder)
            File(toFolder, "management.db").exists()
                    && File(toFolder, "keys.db").exists()
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    private fun copyFolder(fromFolder: File, toFolder: File) {
        FileUtils.copyDirectory(fromFolder, toFolder)
    }
}
