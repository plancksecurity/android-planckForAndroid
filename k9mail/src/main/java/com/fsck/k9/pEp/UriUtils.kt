package com.fsck.k9.pEp

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import java.io.File

object UriUtils {

    @JvmStatic
    fun getPathFromSAFCreateDocumentUri(context: Context, uri: Uri): String? {
        check(Build.VERSION.SDK_INT < 29) { "Do not use after api 28!" }
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val fullPath = getPathFromExtSD(split)
                return if (fullPath.isNotEmpty()) {
                    fullPath
                } else {
                    null
                }
            } else if (isDownloadsDocument(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val id: String = DocumentsContract.getDocumentId(uri).orEmpty()
                    if (!TextUtils.isEmpty(id)) {
                        if (id.contains("raw:")) {
                            return id.substring(id.indexOf(File.separator))
                        } else {
                            var cursor: Cursor? = null
                            try {
                                cursor = context.contentResolver.query(
                                    uri,
                                    arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                                    null,
                                    null,
                                    null
                                )
                                if (cursor != null && cursor.moveToFirst()) {
                                    val fileName = cursor.getString(0)
                                    val path = Environment.getExternalStorageDirectory()
                                        .toString() + "/Download/" + fileName
                                    if (!TextUtils.isEmpty(path)) {
                                        return path
                                    }
                                }
                            } finally {
                                cursor?.close()
                            }
                        }
                    }
                } else {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "")
                    }
                    var contentUri: Uri? = null
                    try {
                        contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            id.toLong()
                        )
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    if(contentUri != null) {
                        return getDataColumn(context, contentUri, null, null)
                    }
                }
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun fileExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    private fun getPathFromExtSD(pathData: List<String>): String {
        val type = pathData[0]
        val relativePath = File.separator + pathData[1]
        var fullPath = ""
        if ("primary".equals(type, ignoreCase = true)) {
            fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
            if (fileExists(fullPath)) {
                return fullPath
            }
        }
        // sd card
        val rootId = Environment.getExternalStorageDirectory().toString().substringAfter(File.separator).substringBefore(File.separator)
        val root = File.separator+rootId
        fullPath = root+File.separator+type+relativePath
        return fullPath
    }

    @Suppress("SameParameterValue")
    private fun getDataColumn(
        context: Context, uri: Uri,
        selection: String?, selectionArgs: Array<String?>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(
                uri, projection,
                selection, selectionArgs, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
}