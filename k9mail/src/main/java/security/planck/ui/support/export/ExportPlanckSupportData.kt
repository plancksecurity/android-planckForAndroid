package security.planck.ui.support.export

import android.content.Context
import android.webkit.MimeTypeMap
import com.fsck.k9.planck.infrastructure.exceptions.CouldNotExportPEpDataException
import com.fsck.k9.planck.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import com.fsck.k9.planck.infrastructure.extensions.flatMap
import com.fsck.k9.planck.saveToDocuments
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import security.planck.file.PlanckSystemFileLocator
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ExportPlanckSupportData @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemFileLocator: PlanckSystemFileLocator,
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
                    fromFolders.forEach { copyFolder(it, subFolder) }
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
        destinationSubFolder: String,
    ) {
        fromFolder.listFiles()?.forEach { file ->
            FileUtils.openInputStream(file).saveToDocuments(
                context,
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension).orEmpty(),
                file.name,
                destinationSubFolder
            )
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
