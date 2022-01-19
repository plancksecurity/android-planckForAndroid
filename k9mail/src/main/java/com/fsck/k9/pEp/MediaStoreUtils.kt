package com.fsck.k9.pEp

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.*

@RequiresApi(Build.VERSION_CODES.Q)
@Throws(IOException::class)
fun InputStream.saveToDocuments(
    context: Context,
    mimeType: String,
    displayName: String,
    subFolder: String? = null,
): Uri? {
    val inputStream = this
    var relativeLocation = Environment.DIRECTORY_DOCUMENTS
    if (!subFolder.isNullOrEmpty()) {
        relativeLocation += File.separator + subFolder
    }
    val contentValues = ContentValues()
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
    contentValues.put(MediaStore.MediaColumns.TITLE, displayName)
    contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis())
    val resolver: ContentResolver = context.contentResolver

    var outputStream: OutputStream? = null
    var uri: Uri? = null
    try {
        val contentUri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        uri = resolver.insert(contentUri, contentValues) ?: let {
            Timber.e("Error: insertion query in Files collection returned null")
            return null
        }
        Timber.d("inserted uri in Files collection is $uri")

        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                ?: throw IllegalStateException("Error: parcel file descriptor is null!!")

            val out = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(4 * 1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                out.write(buffer, 0, length)
            }
            out.close()
            inputStream.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
            throw IOException("Failed to get output stream.")
        }
        return uri
    } catch (e: IOException) {
        // Don't leave an orphan entry in the MediaStore
        uri?.let {
            resolver.delete(it, null, null)
        }
        throw e
    } finally {
        outputStream?.close()
    }
}