package security.pEp.ui.keyimport

import android.content.Context

interface KeyImportView {

    fun openFileChooser()

    fun showEmptyInputError()

    fun showCorrectKeyImport(fingerprint: String, filename: String?)

    fun showFailedKeyImport(filename: String?)

    fun finish()

    fun getApplicationContext(): Context

    fun showDialog()

    fun removeDialog()
}