package security.pEp.ui.passphrase

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import com.fsck.k9.R
import com.fsck.k9.databinding.ActivityPassphraseBinding
import com.fsck.k9.pEp.manualsync.WizardActivity
import com.takisoft.preferencex.PreferenceFragmentCompat
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

class PassphraseActivity : WizardActivity(), PassphraseInputView {

    private lateinit var binding: ActivityPassphraseBinding
    @Inject
    lateinit var presenter: PassphrasePresenter

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassphraseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val type = intent?.extras?.getSerializable(REQUEST_TYPE_EXTRA)
        presenter.init(this, type as PassphraseRequirementType)
    }

    override fun init() {
        setUpFloatingWindow(R.dimen.floating_height)
        binding.afirmativeActionButton.isEnabled = false
    }

    override fun initAffirmativeListeners() {
        with(binding){
            passphrase.doAfterTextChanged { inputText ->
                presenter.validateInput(inputText.toString())
            }

            afirmativeActionButton.setOnClickListener {
                presenter.deliverPassphrase(passphrase.text.toString())
            }
        }
    }

    override fun enableSyncDismiss() {
        with(binding){
            dismissActionButton.setText(R.string.passhphrase_action_disable_sync)
            dismissActionButton.setOnClickListener {
                presenter.cancelSync()
            }
        }
    }

    override fun enableNonSyncDismiss() {
        with(binding){
            dismissActionButton.setText(R.string.cancel_action)
            dismissActionButton.setOnClickListener {
                presenter.cancel()
            }
        }
    }

    override fun finish(passphraseAdded: Boolean) {
        val returnIntent = Intent()
        returnIntent.putExtra(PASSPHRASE_RESULT_KEY, passphraseAdded)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun enableActionConfirmation(enabled: Boolean) {
        binding.afirmativeActionButton.isEnabled = enabled
    }

    override fun showRetryPasswordRequest() {
        binding.description.setText(R.string.passhphrase_body_wrong_passphrase)
    }

    override fun showPasswordRequest() {
        binding.description.setText(R.string.passhphrase_body_insert_passphrase)
    }

    override fun showSyncPasswordRequest() {
        binding.description.setText(R.string.passhphrase_body_sync_passphrase)
    }

    override fun showNewKeysPassphrase() {
        binding.description.setText(R.string.passhphrase_body_new_keys_passphrase)
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