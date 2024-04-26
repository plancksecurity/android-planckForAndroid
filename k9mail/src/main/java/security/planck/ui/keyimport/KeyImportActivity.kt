package security.planck.ui.keyimport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.fsck.k9.R
import com.fsck.k9.databinding.ImportKeyDialogBinding
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.manualsync.WizardActivity
import dagger.hilt.android.AndroidEntryPoint
import foundation.pEp.jniadapter.Identity
import javax.inject.Inject

@AndroidEntryPoint
class KeyImportActivity : WizardActivity(), KeyImportView {
    private lateinit var binding: ImportKeyDialogBinding

    @Inject
    internal lateinit var presenter: KeyImportPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ImportKeyDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (isValidKeyImportIntent(intent)) {
            val accountUuid: String = intent.getStringExtra(ACCOUNT_UUID_EXTRA) ?: ""
            presenter.initialize(this, accountUuid)
        }
        presenter.restoreInstanceState(savedInstanceState)
        presenter.resumeImport()
        setClickListeners()
    }

    private fun setClickListeners() {
        binding.acceptButton.setOnClickListener {
            presenter.onKeyImportAccepted()
        }
        binding.cancelButton.setOnClickListener {
            presenter.onKeyImportRejected()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.onSaveInstanceState(outState)
    }

    override fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val packageManager = packageManager
        val infos = packageManager.queryIntentActivities(intent, 0)
        if (infos.isNotEmpty()) {
            startActivityForResult(Intent.createChooser(intent, null), ACTIVITY_REQUEST_PICK_KEY_FILE)
            presenter.fileManagerOpened()
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

    override fun showKeyImportConfirmationDialog(firstIdentity: Identity, filename: String) {
        binding.addressText.text = getString(R.string.pep_user_address_format, firstIdentity.username, firstIdentity.address)
        binding.fingerprintTextView.text = PlanckUtils.formatFpr(firstIdentity.fpr)
    }

    override fun showLayout() {
        binding.layout.visibility = View.VISIBLE
    }

    override fun hideLayout() {
        binding.layout.visibility = View.GONE
    }

    override fun showCorrectKeyImport() {
        AlertDialog.Builder(this)
                .setTitle(R.string.settings_import_success_header)
                .setMessage(getString(R.string.key_import_success))
                .setCancelable(false)
                .setPositiveButton(R.string.okay_action) { _, _ -> finish() }
                .create()
                .show()
    }

    override fun showFailedKeyImport() {
        AlertDialog.Builder(this)
                .setTitle(R.string.settings_import_failed_header)
                .setMessage(getString(R.string.key_import_failure))
                .setCancelable(false)
                .setPositiveButton(R.string.okay_action) { _, _ -> finish() }
                .create()
                .show()

    }

    override fun showLoading() {
        binding.confirmationLayout.visibility = View.INVISIBLE
        binding.keyImportProgressDialog.keyImportLoadingLayout.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.keyImportProgressDialog.keyImportLoadingLayout.visibility = View.GONE
        binding.confirmationLayout.visibility = View.VISIBLE
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
        const val IMPORT_STATE = "IMPORT_STATE"
        const val ACCOUNT_UUID_EXTRA = "ACCOUNT_UUID_EXTRA"
        const val ACTIVITY_REQUEST_PICK_KEY_FILE = 8
        const val SAVED_STATE_URI = "SAVED_STATE_URI"
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
    }
}



