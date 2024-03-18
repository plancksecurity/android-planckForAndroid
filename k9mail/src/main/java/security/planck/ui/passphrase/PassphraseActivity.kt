package security.planck.ui.passphrase

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import com.fsck.k9.R
import com.fsck.k9.planck.manualsync.WizardActivity
import com.takisoft.preferencex.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_passphrase.*
import timber.log.Timber
import javax.inject.Inject

const val REQUEST_TYPE_EXTRA: String = "requestTypeExtra"
const val PASSPHRASE_REQUEST_ACTION: String = "PASSPHRASE_REQUEST"
const val PASSPHRASE_RESULT_CODE: Int = 1500
const val PASSPHRASE_RESULT_KEY: String = "PASSPHRASE_RESULT_KEY"

fun PreferenceFragmentCompat.requestPassphraseForNewKeys() {
    val intent = Intent(this.context, PassphraseActivity::class.java)
    intent.action = PASSPHRASE_REQUEST_ACTION
    intent.putExtra(REQUEST_TYPE_EXTRA, PassphraseRequirementType.NEW_KEYS_PASSPHRASE)
    startActivityForResult(intent, PASSPHRASE_RESULT_CODE)
}

@AndroidEntryPoint
class PassphraseActivity : WizardActivity(), PassphraseInputView {
    @Inject
    lateinit var presenter: PassphrasePresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passphrase)
        val type = intent?.extras?.getSerializable(REQUEST_TYPE_EXTRA)
        presenter.init(this, type as PassphraseRequirementType)
    }

    override fun init() {
        setUpFloatingWindow(R.dimen.floating_height)
        afirmativeActionButton.isEnabled = false
    }

    override fun initAffirmativeListeners() {
        passphrase.doAfterTextChanged { inputText ->
            presenter.validateInput(inputText.toString())
        }

        afirmativeActionButton.setOnClickListener {
            presenter.deliverPassphrase(passphrase.text.toString())
        }
    }

    override fun enableSyncDismiss() {
        dismissActionButton.setText(R.string.passhphrase_action_disable_sync)
        dismissActionButton.setOnClickListener {
            presenter.cancelSync()
        }
    }

    override fun enableNonSyncDismiss() {
        dismissActionButton.setText(R.string.cancel_action)
        dismissActionButton.setOnClickListener {
            presenter.cancel()
        }
    }

    override fun finish(passphraseAdded: Boolean) {
        val returnIntent = Intent()
        returnIntent.putExtra(PASSPHRASE_RESULT_KEY, passphraseAdded)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun enableActionConfirmation(enabled: Boolean) {
        afirmativeActionButton.isEnabled = enabled
    }

    override fun showPassphraseError() {
        passphrase_container.error = "Passphrase needs to contain at least 12 characters, including uppercase and lowercase letters, numbers and symbols."
    }

    override fun hidePassphraseError() {
        passphrase_container.error = null
    }

    override fun showRetryPasswordRequest() {
        description.setText(R.string.passhphrase_body_wrong_passphrase)
    }

    override fun showPasswordRequest() {
        description.setText(R.string.passhphrase_body_insert_passphrase)
    }

    override fun showSyncPasswordRequest() {
        description.setText(R.string.passhphrase_body_sync_passphrase)
    }

    override fun showNewKeysPassphrase() {
        description.setText(R.string.passhphrase_body_new_keys_passphrase)
    }

    companion object {
        @JvmStatic
        fun notifyRequest(context: Context, type: PassphraseRequirementType) {
            Timber.e("pEpEngine-passphrase launch passphrase")
            val intent = Intent(PASSPHRASE_REQUEST_ACTION)
            intent.putExtra(REQUEST_TYPE_EXTRA, type)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.sendBroadcast(intent)
        }

        @JvmStatic
        fun launchIntent(context: Context, intent: Intent) {
            intent.setClass(context, PassphraseActivity::class.java)
            intent.action = null
            context.startActivity(intent)
        }
    }
}