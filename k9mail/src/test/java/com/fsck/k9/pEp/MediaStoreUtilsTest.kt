package com.fsck.k9.pEp

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.nhaarman.mockito_kotlin.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileDescriptor
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [29], application = Application::class)
class MediaStoreUtilsTest {

    private val inputStream: InputStream = mock()
    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private val uri: Uri = mock()
    private val pfd: ParcelFileDescriptor = mock()
    private val outputStream: OutputStream = mock()


    @Before
    fun setUp() {
        doReturn(contentResolver).`when`(context).contentResolver
        doReturn(mock<FileDescriptor>()).`when`(pfd).fileDescriptor
    }

    @Test
    fun `saveToDocuments() inserts to MediaStore_Files collection`() {
        doReturn(uri).`when`(contentResolver).insert(any(), any())
        doReturn(pfd).`when`(contentResolver).openFileDescriptor(uri, "w")
        doReturn(outputStream).`when`(contentResolver).openOutputStream(uri)


        inputStream.saveToDocuments(context, MIME_TYPE, DISPLAY_NAME, null)


        verifyInsertedInFilesCollection(DISPLAY_NAME, MIME_TYPE, null)
    }

    @Test
    fun `saveToDownloads() inserts to MediaStore_Downloads collection`() {
        doReturn(uri).`when`(contentResolver).insert(any(), any())
        doReturn(pfd).`when`(contentResolver).openFileDescriptor(uri, "w")
        doReturn(outputStream).`when`(contentResolver).openOutputStream(uri)


        inputStream.saveToDownloads(context, MIME_TYPE, DISPLAY_NAME, null)


        verifyInsertedInDownloadsCollection(DISPLAY_NAME, MIME_TYPE, null)
    }

    @Test
    fun `saveToDocuments() inserts to MediaStore_Files collection using relative path`() {
        doReturn(uri).`when`(contentResolver).insert(any(), any())
        doReturn(pfd).`when`(contentResolver).openFileDescriptor(uri, "w")
        doReturn(outputStream).`when`(contentResolver).openOutputStream(uri)


        inputStream.saveToDocuments(context, MIME_TYPE, DISPLAY_NAME, SUBFOLDER)


        verifyInsertedInFilesCollection(DISPLAY_NAME, MIME_TYPE, SUBFOLDER)
    }

    @Test
    fun `saveToDocuments() returns the uri from inserting to MediaStore_Files collection`() {
        doReturn(uri).`when`(contentResolver).insert(any(), any())
        doReturn(pfd).`when`(contentResolver).openFileDescriptor(uri, "w")
        doReturn(outputStream).`when`(contentResolver).openOutputStream(uri)


        val result = inputStream.saveToDocuments(context, MIME_TYPE, DISPLAY_NAME, null)


        assertEquals(uri, result)
    }

    @Test
    fun `saveToDocuments() closes used closeables`() {
        doReturn(uri).`when`(contentResolver).insert(any(), any())
        doReturn(pfd).`when`(contentResolver).openFileDescriptor(uri, "w")
        doReturn(outputStream).`when`(contentResolver).openOutputStream(uri)


        inputStream.saveToDocuments(context, MIME_TYPE, DISPLAY_NAME, SUBFOLDER)


        verify(contentResolver).openFileDescriptor(uri, "w")
        verify(inputStream).close()
        verify(pfd).close()
        verify(contentResolver).openOutputStream(uri)
        verify(outputStream).close()
    }

    @Test
    fun `saveToDocuments() throws IOException if ContentResolver_openFileDescriptor is null`() {
        doReturn(uri).`when`(contentResolver).insert(any(), any())
        doReturn(null).`when`(contentResolver).openFileDescriptor(uri, "w")
        doReturn(outputStream).`when`(contentResolver).openOutputStream(uri)


        val exception = Assert.assertThrows(IOException::class.java) {
            inputStream.saveToDocuments(context, MIME_TYPE, DISPLAY_NAME, null)
        }


        assertEquals("Error: parcel file descriptor is null!!", exception.message)
    }

    @Test
    fun `saveToDocuments() throws IOException if ContentResolver_openOutputStream is null`() {
        doReturn(uri).`when`(contentResolver).insert(any(), any())
        doReturn(pfd).`when`(contentResolver).openFileDescriptor(uri, "w")
        doReturn(null).`when`(contentResolver).openOutputStream(uri)


        val exception = Assert.assertThrows(IOException::class.java) {
            inputStream.saveToDocuments(context, MIME_TYPE, DISPLAY_NAME, null)
        }


        assertEquals("Failed to get output stream.", exception.message)
    }

    @Suppress("SameParameterValue")
    private fun verifyInsertedInFilesCollection(
        name: String,
        mimeType: String,
        subFolder: String?
    ) {
        val documentsUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        verifyInsertedInCollection(
            documentsUri,
            Environment.DIRECTORY_DOCUMENTS,
            name,
            mimeType,
            subFolder
        )
    }

    @Suppress("SameParameterValue")
    private fun verifyInsertedInDownloadsCollection(
        name: String,
        mimeType: String,
        subFolder: String?
    ) {
        val downloadsUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        verifyInsertedInCollection(
            downloadsUri,
            Environment.DIRECTORY_DOWNLOADS,
            name,
            mimeType,
            subFolder
        )
    }

    private fun verifyInsertedInCollection(
        mediaStoreUri: Uri,
        mediaRoot: String,
        name: String,
        mimeType: String,
        subFolder: String?
    ) {
        val captor = argumentCaptor<ContentValues>()
        verify(contentResolver).insert(eq(mediaStoreUri), captor.capture())

        val contentValues = captor.firstValue

        assertEquals(name, contentValues.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
        assertEquals(
            mimeType,
            contentValues.getAsString(MediaStore.MediaColumns.MIME_TYPE)
        )
        assertEquals(
            mediaRoot + (subFolder?.let { "/$SUBFOLDER" } ?: ""),
            contentValues.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)
        )
        assertTrue(contentValues.getAsLong(MediaStore.MediaColumns.DATE_MODIFIED) > 0L)
    }
    
    companion object {
        private const val MIME_TYPE = "mimeType"
        private const val DISPLAY_NAME = "name"
        private const val SUBFOLDER = "subFolder"
    }
}