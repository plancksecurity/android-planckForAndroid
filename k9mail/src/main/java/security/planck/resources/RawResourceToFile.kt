package security.planck.resources

import android.content.Context
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class RawResourceToFile @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun copyRawResourceToFile(@RawRes resourceId: Int, outputFileName: String): File {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        val outputFile = File(context.cacheDir, outputFileName)
        inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        return outputFile
    }
}