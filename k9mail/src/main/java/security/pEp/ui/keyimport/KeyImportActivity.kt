package security.pEp.ui.keyimport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.fsck.k9.R
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.manualsync.WizardActivity
import foundation.pEp.jniadapter.Identity
import kotlinx.android.synthetic.main.import_key_dialog.*
import kotlinx.android.synthetic.main.key_import_progress_dialog.*
import javax.inject.Inject


const val ACCOUNT_UUID_EXTRA = "ACCOUNT_UUID_EXTRA"
const val ACTIVITY_REQUEST_PICK_KEY_FILE = 8
const val ANDROID_MARKET_URL = "https://play.google.com/store/apps/details?id=org.openintents.filemanager"


class KeyImportActivity : WizardActivity(), KeyImportView {

    @Inject
    internal lateinit var presenter: KeyImportPresenter

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_key_dialog)
        if (isValidKeyImportIntent(intent)) {
            val accountUuid: String = intent.getStringExtra(ACCOUNT_UUID_EXTRA) ?: ""
            presenter.initialize(this, accountUuid)
        }
    }

    override fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val packageManager = packageManager
        val infos = packageManager.queryIntentActivities(intent, 0)
        if (infos.isNotEmpty()) {
            startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_REQUEST_PICK_KEY_FILE)
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
                    openMarketIntent(this@KeyImportActivity)
                }
                .create()
                .show()
    }

    override fun showKeyImportConfirmationDialog(importedIdentities: List<Identity>, filename: String) {
        val identityTextLines = importedIdentities.map { identity ->
            getString(R.string.pep_user_address_format, identity.username, identity.address)
        }.joinToString("\n")
        addressText.text = identityTextLines
        fingerprintTextView.text = PEpUtils.formatFpr(importedIdentities.first().fpr)
        acceptButton.setOnClickListener {
            layout.visibility = View.GONE
            presenter.onKeyImportAccepted(importedIdentities, filename)
        }
        cancelButton.setOnClickListener {
            layout.visibility = View.GONE
            presenter.onKeyImportRejected()
        }
        layout.visibility = View.VISIBLE
    }

    override fun showCorrectKeyImport(fingerprint: String, filename: String?) {
        AlertDialog.Builder(this)
                .setTitle(R.string.settings_import_success_header)
                .setMessage(getString(R.string.key_import_success))
                .setCancelable(false)
                .setPositiveButton(R.string.okay_action) { _, _ -> finish() }
                .create()
                .show()
    }

    override fun showKeyImportResult(result: Map<Identity, Boolean>, filename: String) {
        val goodSb = StringBuilder()
        val badSb = StringBuilder()
        result.forEach {
            val line = getString(R.string.pep_user_address_format, it.key.username, it.key.address)
            val identityLine = getString(R.string.bullet_text, line)
            if(it.value) {
                goodSb.append(identityLine)
                goodSb.append("\n")
            }
            else {
                badSb.append(identityLine)
                badSb.append("\n")
            }
        }
        val resultSb = StringBuilder()
        if(goodSb.isNotEmpty()) {
            resultSb.append(getString(R.string.key_import_success_general_settings, goodSb))
        }
        if(badSb.isNotEmpty()) {
            resultSb.append(getString(R.string.key_could_not_be_imported_for_accounts_general_settings, badSb))
        }
        AlertDialog.Builder(this)
                .setTitle(R.string.key_import_result_header)
                .setMessage(resultSb)
                .setCancelable(false)
                .setPositiveButton(R.string.okay_action) { _, _ -> finish() }
                .create()
                .show()
    }

    override fun showFailedKeyImport(filename: String?) {
        AlertDialog.Builder(this)
                .setTitle(R.string.settings_import_failed_header)
                .setMessage(getString(R.string.key_import_failure))
                .setCancelable(false)
                .setPositiveButton(R.string.okay_action) { _, _ -> finish() }
                .create()
                .show()

    }

    override fun showLoading() {
        val title = getString(R.string.settings_import_dialog_title)
        val message = getString(R.string.settings_import_scanning_file)

        confirmationLayout.visibility = View.INVISIBLE
        keyImportLoadingLayout.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        keyImportLoadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.VISIBLE
    }

    private fun isValidKeyImportIntent(intent: Intent): Boolean = when {
        intent.hasExtra(ACCOUNT_UUID_EXTRA) -> true
        else -> throw IllegalArgumentException("The provided intent does not contain the required extras")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val ACTIVITY_REQUEST_PICK_KEY_FILE = 8
        const val ANDROID_FILE_MANAGER_MARKET_URL = "https://play.google.com/store/apps/details?id=org.openintents.filemanager"

        private fun openMarketIntent(activity: Activity) {
            val uri = Uri.parse(ANDROID_FILE_MANAGER_MARKET_URL)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity.startActivity(intent)
        }

        fun showImportKeyDialog(activity: Activity?, account: String) {
            val intent = Intent(activity, KeyImportActivity::class.java)
            intent.putExtra(ACCOUNT_UUID_EXTRA, account)
            activity?.startActivity(intent)
        }

        fun showImportKeyDialog(activity: Activity?) {
            val intent = Intent(activity, KeyImportActivity::class.java)
            intent.putExtra(ACCOUNT_UUID_EXTRA, "")
            activity?.startActivity(intent)
        }
    }
}



