package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fsck.k9.R
import com.fsck.k9.pEp.PEpImporterActivity
import javax.inject.Inject
import javax.inject.Named

class KeyImportPresenter @Inject constructor(@Named("ActivityContext") private val context: Context) {

    private lateinit var fingerprint: String
    private lateinit var view: KeyImportView
    private lateinit var account: String

    fun initialize(view: KeyImportView, account: String) {
        this.view = view
        this.account = account
    }

    fun onAccept(fingerprint: String) {
        val trimmedFingerprint = fingerprint.replace(" ", "")
        if (trimmedFingerprint.isEmpty()) {
            view.showNegativeFeedback(context.getString(R.string.pgp_key_import_dialog_empty_edittext))
        } else {
            this.fingerprint = trimmedFingerprint
            view.openFileChooser()
        }
    }

    fun onReject() {
        view.finish()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }
        when (requestCode) {
            ACTIVITY_REQUEST_PICK_KEY_FILE -> data.data?.let { onKeyImport(it, account) }
        }
    }

    fun onKeyImport(uri: Uri, currentAccount: String) {
      //  val asyncTask = PEpImporterActivity.ListImportContentsAsyncTask(this, uri, currentAccount, true, fpr)
      //  setNonConfigurationInstance(asyncTask)
      //  asyncTask.execute()
    }

}