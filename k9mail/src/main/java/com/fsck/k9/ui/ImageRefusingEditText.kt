package com.fsck.k9.ui

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import security.pEp.ui.input.utils.InputConnectionProvider
import security.pEp.ui.input.utils.InputConnectionProviderImpl

/**
 * An [AppCompatEditText] extension with methods that blocks images from being added through the keyboard
 */
class ImageRefusingEditText(context: Context, attrs: AttributeSet) : EolConvertingEditText(context, attrs) {

    private val inputConnectionProvider: InputConnectionProvider = InputConnectionProviderImpl(context)

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic = super.onCreateInputConnection(editorInfo)!!
        return inputConnectionProvider.provideInputConnection(ic, editorInfo)
    }

}