package security.pEp.ui.input.utils

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.fsck.k9.R

interface InputConnectionProvider {

    fun provideInputConnection(ic: InputConnection, editorInfo: EditorInfo): InputConnection
}

class InputConnectionProviderImpl(private val context: Context) : InputConnectionProvider {

    override fun provideInputConnection(ic: InputConnection, editorInfo: EditorInfo): InputConnection {
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/*"))

        val callback = { _: InputContentInfoCompat?, _: Int?, _: Bundle? ->
            val message = String.format("%s does not support image insertion", context.getString(R.string.pep))
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            true
        }
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }
}
