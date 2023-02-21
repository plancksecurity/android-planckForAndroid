package com.fsck.k9.pEp

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

fun Uri.getMediaStoreAbsoluteFilePathOrNull(context: Context): String? {
    return try {
        getDataColumn(context, this, null, null)
    } catch (ex: Throwable) {
        null
    }
}

@Suppress("SameParameterValue")
private fun getDataColumn(
    context: Context, uri: Uri,
    selection: String?, selectionArgs: Array<String?>?
): String? {
    val column = MediaStore.MediaColumns.DATA
    val projection = arrayOf(column)
    return context.contentResolver.query(
        uri, projection,
        selection, selectionArgs, null
    ).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            cursor.getString(index)
        } else null
    }
}
