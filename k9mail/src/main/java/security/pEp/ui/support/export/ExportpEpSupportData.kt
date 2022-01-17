package security.pEp.ui.support.export

import com.fsck.k9.pEp.infrastructure.exceptions.CouldNotExportPEpDataException
import com.fsck.k9.pEp.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ExportpEpSupportData @Inject constructor() {
    suspend operator fun invoke(
        fromFolders: List<File>,
        toFolder: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (toFolder.exists() || toFolder.mkdirs()) {
                checkSpaceAvailability(fromFolders, toFolder).flatMap {
                    fromFolders.forEach { copyFolder(it, toFolder) }
                    if (areFilesCreated(toFolder)) Result.success(Unit)
                    else Result.failure(CouldNotExportPEpDataException())
                }
            } else {
                Result.failure(CouldNotExportPEpDataException())
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(CouldNotExportPEpDataException(e))
        }
    }

    private fun areFilesCreated(folder: File) = (File(folder, "management.db").exists()
            && File(folder, "keys.db").exists()
            && File(folder, "system.db").exists())

    private fun checkSpaceAvailability(
        fromFolders: List<File>,
        toFolder: File
    ): Result<Unit> {
        val neededBytes = fromFolders.sumOf { it.folderSize }
        val availableSizeInBytes = toFolder.freeSpace
        return if (neededBytes < availableSizeInBytes) Result.success(Unit)
        else Result.failure(NotEnoughSpaceInDeviceException(neededBytes, availableSizeInBytes))
    }

    private fun copyFolder(fromFolder: File, toFolder: File) {
        FileUtils.copyDirectory(fromFolder, toFolder)
    }

    private val File.folderSize: Long
        get() {
            var total = 0L
            listFiles()?.forEach { file ->
                total += if (file.isDirectory) {
                    file.folderSize
                } else {
                    file.length()
                }
            }
            return total
        }

    private fun <Type, NewType> Result<Type>.flatMap(block: (Type) -> Result<NewType>): Result<NewType> =
        fold(
            onSuccess = { block(it) },
            onFailure = { Result.failure(it) }
        )
}
