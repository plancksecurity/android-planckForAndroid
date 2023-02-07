package security.pEp.ui.support.export

import android.content.Context
import android.os.Build
import android.webkit.MimeTypeMap
import com.fsck.k9.pEp.infrastructure.exceptions.CouldNotExportPEpDataException
import com.fsck.k9.pEp.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import com.fsck.k9.pEp.infrastructure.extensions.flatMap
import com.fsck.k9.pEp.saveToDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import security.pEp.file.PEpSystemFileLocator
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class ExportpEpSupportData @Inject constructor(
    @Named("AppContext") private val context: Context,
    private val systemFileLocator: PEpSystemFileLocator,
) {
    suspend operator fun invoke(
        baseFolder: File,
        subFolder: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fromFolders = listOf(
                systemFileLocator.pEpFolder,
                systemFileLocator.trustwordsFolder
            )
            val toFolder = File(baseFolder, subFolder)

            if (toFolder.exists() || toFolder.mkdirs()) {
                checkSpaceAvailability(fromFolders, baseFolder).flatMap {
                    fromFolders.forEach { copyFolder(it, baseFolder, subFolder) }
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

    private fun copyFolder(
        fromFolder: File,
        destinationBaseFolder: File,
        destinationSubFolder: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fromFolder.listFiles()?.forEach { file ->
                FileUtils.openInputStream(file).saveToDocuments(
                    context,
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension).orEmpty(),
                    file.name,
                    destinationSubFolder
                )
            }
        } else {
            val toFolder = File(destinationBaseFolder, destinationSubFolder)
            FileUtils.copyDirectory(fromFolder, toFolder)
        }
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
}
