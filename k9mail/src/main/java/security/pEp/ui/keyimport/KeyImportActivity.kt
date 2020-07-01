package security.pEp.ui.keyimport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.fsck.k9.R
import com.fsck.k9.pEp.manualsync.WizardActivity
import kotlinx.android.synthetic.main.import_key_dialog.*
import security.pEp.ui.dialog.PEpProgressDialog
import security.pEp.ui.dialog.showProgressDialog
import javax.inject.Inject


const val ACCOUNT_EXTRA = "ACCOUNT_EXTRA"
const val ACTIVITY_REQUEST_PICK_KEY_FILE = 8
const val ANDROID_MARKET_URL = "https://play.google.com/store/apps/details?id=org.openintents.filemanager"


class KeyImportActivity : WizardActivity(), KeyImportView {

    @Inject
    internal lateinit var presenter: KeyImportPresenter

    private var progressDialog: PEpProgressDialog? = null

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_key_dialog)
        if (isValidKeyImportIntent(intent)) {
            val account: String = intent.getStringExtra(ACCOUNT_EXTRA) ?: ""
            presenter.initialize(this, account)
        }
        setView()
    }

    private fun setView() {
        cancelButton.setOnClickListener { presenter.onReject() }
        acceptButton.setOnClickListener { presenter.onAccept(fingerprintEditText.text.toString()) }
        fingerprintEditText.afterTextChanged { text ->
            fingerprintEditText.error = null
            acceptButton.isEnabled = text.isNotEmpty()
        }
    }

    override fun openFileChooser() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        val packageManager = packageManager
        val infos = packageManager.queryIntentActivities(i, 0)
        if (infos.isNotEmpty()) {
            startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_PICK_KEY_FILE)
        } else {
            showNoFileManager()
        }
    }

    private fun showNoFileManager() {
        AlertDialog.Builder(this)
                .setTitle(R.string.import_dialog_error_title)
                .setMessage(R.string.import_dialog_error_message)
                .setCancelable(false)
                .setNegativeButton(R.string.close) { dialogInterface, _ -> dialogInterface.dismiss() }
                .setPositiveButton(R.string.open_market) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    openMarketIntent()
                }
                .create()
                .show()
    }


    override fun showEmptyInputError() {
        fingerprintEditText.error = getString(R.string.pgp_key_import_dialog_empty_edittext)
    }

    override fun showCorrectKeyImport(fingerprint: String, filename: String?) {
        AlertDialog.Builder(this)
                .setTitle(R.string.settings_import_success_header)
                .setMessage(getString(R.string.key_import_success, fingerprint, filename))
                .setCancelable(false)
                .setPositiveButton(R.string.okay_action) { _, _ -> finish() }
                .create()
                .show()
    }

    override fun showFailedKeyImport(fingerprint: String, filename: String?) {
        AlertDialog.Builder(this)
                .setTitle(R.string.settings_import_failed_header)
                .setMessage(getString(R.string.key_import_failure, filename, fingerprint))
                .setCancelable(false)
                .setPositiveButton(R.string.okay_action) { _, _ -> finish() }
                .create()
                .show()

    }

    override fun showDialog() {
        val title = getString(R.string.settings_import_dialog_title)
        val message = getString(R.string.settings_import_scanning_file)
        progressDialog = showProgressDialog(title, message, true)
        //   progressDialog = ProgressDialog.show(this, title, message, true)
    }

    override fun removeDialog() {
        progressDialog?.closeDialog()
    }

    private fun isValidKeyImportIntent(intent: Intent): Boolean = when {
        intent.hasExtra(ACCOUNT_EXTRA) -> true
        else -> throw IllegalArgumentException("The provided intent does not contain the required extras")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }
}


private fun Activity.openMarketIntent() {
    val uri = Uri.parse(ANDROID_MARKET_URL)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(intent)
}

fun Activity.showImportKeyDialog(account: String) {
    val intent = Intent(this, KeyImportActivity::class.java)
    intent.putExtra(ACCOUNT_EXTRA, account)
    startActivity(intent)
}

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

