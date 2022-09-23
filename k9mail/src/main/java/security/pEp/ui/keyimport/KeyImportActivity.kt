package security.pEp.ui.keyimport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.fsck.k9.R
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.manualsync.WizardActivity
import foundation.pEp.jniadapter.Identity
import javax.inject.Inject


class KeyImportActivity : WizardActivity(), KeyImportView {

    @Inject
    internal lateinit var presenter: KeyImportPresenter
    private var acceptButton: Button? = null
    private var cancelButton: Button? = null
    private var addressText: TextView? = null
    private var fingerprintTextView: TextView? = null
    private var layout: View? = null
    private var confirmationLayout: View? = null
    private var keyImportLoadingLayout: View? = null


    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_key_dialog)
        setupViews()
        if (isValidKeyImportIntent(intent)) {
            val accountUuid: String = intent.getStringExtra(ACCOUNT_UUID_EXTRA) ?: ""
            presenter.initialize(this, accountUuid)
        }
        presenter.restoreInstanceState(savedInstanceState)
        presenter.resumeImport()
        setClickListeners()
    }

    private fun setupViews() {
        acceptButton = findViewById(R.id.acceptButton)
        cancelButton = findViewById(R.id.cancelButton)
        addressText = findViewById(R.id.addressText)
        fingerprintTextView = findViewById(R.id.fingerprintTextView)
        layout = findViewById(R.id.layout)
        confirmationLayout = findViewById(R.id.confirmationLayout)
        keyImportLoadingLayout = findViewById(R.id.keyImportLoadingLayout)
    }

    private fun setClickListeners() {
        acceptButton?.setOnClickListener {
            presenter.onKeyImportAccepted()
        }
        cancelButton?.setOnClickListener {
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
        addressText?.text = getString(R.string.pep_user_address_format, firstIdentity.username, firstIdentity.address)
        fingerprintTextView?.text = PEpUtils.formatFpr(firstIdentity.fpr)
    }

    override fun showLayout() {
        layout?.visibility = View.VISIBLE
    }

    override fun hideLayout() {
        layout?.visibility = View.GONE
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
        confirmationLayout?.visibility = View.INVISIBLE
        keyImportLoadingLayout?.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        keyImportLoadingLayout?.visibility = View.GONE
        confirmationLayout?.visibility = View.VISIBLE
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



