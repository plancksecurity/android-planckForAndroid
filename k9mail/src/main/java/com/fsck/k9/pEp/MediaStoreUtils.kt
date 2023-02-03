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
    return saveToMediaStore(
        context,
        mimeType,
        displayName,
        MediaStore.Files::class.java,
        Environment.DIRECTORY_DOCUMENTS,
        subFolder
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Throws(IOException::class)
fun InputStream.saveToDownloads(
    context: Context,
    mimeType: String,
    displayName: String,
    subFolder: String? = null,
): Uri? {
    return saveToMediaStore(
        context,
        mimeType,
        displayName,
        MediaStore.Downloads::class.java,
        Environment.DIRECTORY_DOWNLOADS,
        subFolder
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Throws(IOException::class)
private fun InputStream.saveToMediaStore(
    context: Context,
    mimeType: String,
    displayName: String,
    mediaType: Class<*>,
    rootPath: String? = null,
    subFolder: String? = null,
): Uri? {
    val inputStream = this
    var relativeLocation: String? = null
    if (!rootPath.isNullOrBlank()) {
        relativeLocation = rootPath
    }
    if (!subFolder.isNullOrBlank()) {
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
        val contentUri: Uri = getContentUri(mediaType)
        uri = resolver.insert(contentUri, contentValues) ?: let {
            Timber.e("Error: insertion query in $mediaType collection returned null")
            return null
        }
        Timber.d("inserted uri in $mediaType collection is $uri")

        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                ?: error("Error: parcel file descriptor is null!!")

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

@RequiresApi(Build.VERSION_CODES.Q)
private fun getContentUri(mediaType: Class<*>): Uri = when (mediaType) {
    MediaStore.Downloads::class.java ->
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    MediaStore.Files::class.java ->
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    else -> error("Media type $mediaType does not exist or is missing here!")
}
