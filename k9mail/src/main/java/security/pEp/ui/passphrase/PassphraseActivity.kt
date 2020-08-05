package security.pEp.ui.passphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.pEp.manualsync.WizardActivity
import kotlinx.android.synthetic.main.activity_passphrase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

const val REQUEST_TYPE_EXTRA: String = "requestTypeExtra"
const val PASSPHRASE_REQUEST_ACTION: String = "PASSPHRASE_REQUEST"
class PassphraseActivity : WizardActivity(), PassphraseInputView {
    @Inject
    lateinit var presenter: PassphrasePresenter

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passphrase)
        val type = intent?.extras?.getSerializable(REQUEST_TYPE_EXTRA)
        presenter.init(this, type as PassphraseRequirementType)
    }

    override fun init() {
        when (presenter.type) {
            PassphraseRequirementType.NEW_KEYS_PASSPHRASE ->
                setUpFloatingWindow(R.dimen.floating_height)
            else ->
                setUpFloatingWindow()
        }
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

    override fun enableActionConfirmation(enabled: Boolean) {
        afirmativeActionButton.isEnabled = enabled
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