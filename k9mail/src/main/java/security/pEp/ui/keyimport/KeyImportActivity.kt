package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.fsck.k9.R
import com.fsck.k9.pEp.PEpImporterActivity
import com.fsck.k9.pEp.PepActivity
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import kotlinx.android.synthetic.main.import_key_dialog.*
import javax.inject.Inject

const val ACCOUNT_EXTRA = "ACCOUNT_EXTRA"
const val ACTIVITY_REQUEST_PICK_KEY_FILE = 8
const val DIALOG_NO_FILE_MANAGER = 4


class KeyImportActivity : PepActivity(), KeyImportView {

    @Inject
    internal lateinit var presenter: KeyImportPresenter

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
            showDialog(DIALOG_NO_FILE_MANAGER)
        }
    }

    override fun showNegativeFeedback(message: String) {
        FeedbackTools.showLongFeedback(rootView, getString(R.string.pgp_key_import_dialog_empty_edittext))
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

