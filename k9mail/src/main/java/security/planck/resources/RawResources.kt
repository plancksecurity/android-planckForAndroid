package security.planck.resources

import android.content.Context
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

class RawResources @Inject constructor(
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

    fun readTextFileFromRaw(@RawRes resourceId: Int): String = runBlocking {
        withContext(Dispatchers.IO) {
            val inputStream = context.resources.openRawResource(resourceId)
            inputStream.bufferedReader().use { it.readText() }
        }
    }
}