package com.fsck.k9.planck

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class UriUtilsTest {
    private val uri: Uri = mockk()
    private val context: Context = mockk()
    private val contentResolver: ContentResolver = mockk()
    private val cursor: Cursor = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { context.contentResolver }.returns(contentResolver)
    }

    @Test
    fun `getMediaStoreAbsoluteFilePathOrNull() gets path by querying MediaStore data column`() {
        every {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }.returns(cursor)
        every { cursor.moveToFirst() }.returns(true)
        every { cursor.getColumnIndexOrThrow(any()) }.returns(0)
        every { cursor.getString(0) }.returns("path")


        val result = uri.getMediaStoreAbsoluteFilePathOrNull(context)


        verify {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }
        assertEquals("path", result)
    }

    @Test
    fun `getMediaStoreAbsoluteFilePathOrNull() returns null if cursor is null`() {
        every {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }.returns(null)


        val result = uri.getMediaStoreAbsoluteFilePathOrNull(context)


        verify {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }
        assertEquals(null, result)
    }

    @Test
    fun `getMediaStoreAbsoluteFilePathOrNull() returns null if no result found`() {
        every {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }.returns(cursor)
        every { cursor.moveToFirst() }.returns(false)


        val result = uri.getMediaStoreAbsoluteFilePathOrNull(context)


        verify {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }
        assertEquals(null, result)
    }

    @Test
    fun `getMediaStoreAbsoluteFilePathOrNull() returns null if an exception is thrown`() {
        every {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }.returns(cursor)
        every { cursor.moveToFirst() }.throws(RuntimeException())


        val result = uri.getMediaStoreAbsoluteFilePathOrNull(context)


        verify {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )
        }
        assertEquals(null, result)
    }
}