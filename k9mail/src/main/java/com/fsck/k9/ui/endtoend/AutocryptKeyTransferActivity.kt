package com.fsck.k9.ui.endtoend


import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.databinding.CryptoKeyTransferBinding
import com.fsck.k9.finishWithErrorToast
import com.fsck.k9.view.StatusIndicator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AutocryptKeyTransferActivity : K9Activity(), AutocryptKeyTransferView {
    private lateinit var binding: CryptoKeyTransferBinding

    override fun search(query: String?) {
    }
    private val viewModel: AutocryptKeyTransferViewModel by viewModels()
    @Inject
    lateinit var presenter: AutocryptKeyTransferPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CryptoKeyTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter.initialize(this, viewModel, this)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        binding.transferSendButton.setOnClickListener { presenter.onClickTransferSend() }
        binding.transferButtonShowCode.setOnClickListener { presenter.onClickShowTransferCode() }

        presenter.initFromIntent(accountUuid)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            presenter.onClickHome();
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun setAddress(address: String) {
        binding.transferAddress1.text = address
        binding.transferAddress2.text = address
    }

    override fun sceneBegin() {
        binding.transferSendButton.visibility = View.VISIBLE
        binding.transferMsgInfo.visibility = View.VISIBLE
        binding.transferLayoutGenerating.visibility = View.GONE
        binding.transferLayoutSending.visibility = View.GONE
        binding.transferLayoutFinish.visibility = View.GONE
        binding.transferErrorSend.visibility = View.GONE
        binding.transferButtonShowCode.visibility = View.GONE
    }

    override fun sceneGeneratingAndSending() {
        setupSceneTransition()

        binding.transferSendButton.visibility = View.GONE
        binding.transferMsgInfo.visibility = View.GONE
        binding.transferLayoutGenerating.visibility = View.VISIBLE
        binding.transferLayoutSending.visibility = View.VISIBLE
        binding.transferLayoutFinish.visibility = View.GONE
        binding.transferErrorSend.visibility = View.GONE
        binding.transferButtonShowCode.visibility = View.GONE
    }

    override fun sceneSendError() {
        setupSceneTransition()

        binding.transferSendButton.visibility = View.GONE
        binding.transferMsgInfo.visibility = View.GONE
        binding.transferLayoutGenerating.visibility = View.VISIBLE
        binding.transferLayoutSending.visibility = View.VISIBLE
        binding.transferLayoutFinish.visibility = View.GONE
        binding.transferErrorSend.visibility = View.VISIBLE
        binding.transferButtonShowCode.visibility = View.GONE
    }

    override fun sceneFinished(transition: Boolean) {
        if (transition) {
            setupSceneTransition()
        }

        binding.transferSendButton.visibility = View.GONE
        binding.transferMsgInfo.visibility = View.GONE
        binding.transferLayoutGenerating.visibility = View.VISIBLE
        binding.transferLayoutSending.visibility = View.VISIBLE
        binding.transferLayoutFinish.visibility = View.VISIBLE
        binding.transferErrorSend.visibility = View.GONE
        binding.transferButtonShowCode.visibility = View.VISIBLE
    }

    override fun setLoadingStateGenerating() {
        binding.transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.PROGRESS)
        binding.transferProgressSending.setDisplayedChild(StatusIndicator.Status.IDLE)
    }

    override fun setLoadingStateSending() {
        binding.transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.OK)
        binding.transferProgressSending.setDisplayedChild(StatusIndicator.Status.PROGRESS)
    }

    override fun setLoadingStateSendingFailed() {
        binding.transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.OK)
        binding.transferProgressSending.setDisplayedChild(StatusIndicator.Status.ERROR)
    }

    override fun setLoadingStateFinished() {
        binding.transferProgressGenerating.setDisplayedChild(StatusIndicator.Status.OK)
        binding.transferProgressSending.setDisplayedChild(StatusIndicator.Status.OK)
    }

    override fun finishWithInvalidAccountError() {
        finishWithErrorToast(R.string.toast_account_not_found)
    }

    override fun finishWithProviderConnectError(providerName: String) {
        finishWithErrorToast(R.string.toast_openpgp_provider_error, providerName)
    }

    override fun launchUserInteractionPendingIntent(pendingIntent: PendingIntent) {
        try {
            startIntentSender(pendingIntent.intentSender, null, 0, 0, 0)
        } catch (e: SendIntentException) {
            Timber.e(e)
        }
    }

    private fun setupSceneTransition() {
        val transition = TransitionInflater.from(this).inflateTransition(R.transition.transfer_transitions)
        TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), transition)
    }

    override fun finishAsCancelled() {
        setResult(RESULT_CANCELED)
        finish()
    }

    suspend fun uxDelay() {
        // called before logic resumes upon screen transitions, to give some breathing room
        //delay(UX_DELAY_MS)
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"
        private const val UX_DELAY_MS = 1200L

        fun createIntent(context: Context, accountUuid: String): Intent {
            val intent = Intent(context, AutocryptKeyTransferActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, accountUuid)
            return intent
        }
    }
}