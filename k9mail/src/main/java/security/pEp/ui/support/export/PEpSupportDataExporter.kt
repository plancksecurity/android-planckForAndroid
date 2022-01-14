package security.pEp.ui.support.export

import android.os.StatFs
import com.fsck.k9.pEp.infrastructure.exceptions.NotEnoughSpaceInDeviceException
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
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            checkEnoughSpaceToCopy(fromFolder, toFolder).map {
                toFolder.mkdirs()
                if (!toFolder.exists()) {
                    false
                } else {
                    copyFolder(fromFolder, toFolder)
                    File(toFolder, "management.db").exists()
                            && File(toFolder, "keys.db").exists()
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.success(false)
        }
    }

    private fun checkEnoughSpaceToCopy(
        fromFolder: File,
        toFolder: File
    ): Result<Unit> {
        val neededBytes = fromFolder.folderSize()
        val availableSizeInBytes = StatFs(toFolder.absolutePath).availableBytes
        return if (neededBytes < availableSizeInBytes) Result.success(Unit)
        else Result.failure(NotEnoughSpaceInDeviceException(neededBytes, availableSizeInBytes))
    }

    private fun copyFolder(fromFolder: File, toFolder: File) {
        FileUtils.copyDirectory(fromFolder, toFolder)
    }

    private fun File.folderSize(): Long {
        var total = length()
        listFiles()?.forEach { file ->
            total += if (file.isDirectory) {
                file.folderSize()
            } else {
                file.length()
            }
        }
        return total
    }
}
