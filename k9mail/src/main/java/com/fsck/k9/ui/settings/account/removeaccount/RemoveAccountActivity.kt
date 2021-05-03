package com.fsck.k9.ui.settings.account.removeaccount

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.databinding.ActivityRemoveAccountBinding
import com.fsck.k9.pEp.manualsync.WizardActivity
import javax.inject.Inject

class RemoveAccountActivity : WizardActivity(), RemoveAccountView {
    private lateinit var binding: ActivityRemoveAccountBinding

    @Inject
    lateinit var presenter: RemoveAccountPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()

        intent.extras?.let {
            val model = getViewModel()
            val accountUuid = it.getString(EXTRA_ACCOUNT_UUID, "")
            presenter.initialize(this, model, model, accountUuid, savedInstanceState == null)
        } ?: finish()
    }

    private fun getViewModel() =
        ViewModelProvider(this).get(RemoveAccountViewModel::class.java)

    override fun inject() {
        getpEpComponent().inject(this)
    }

    private fun setupViews() {
        binding = ActivityRemoveAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.title.text = getString(R.string.account_delete_dlg_title)
        binding.acceptButton.setOnClickListener {
            presenter.onAcceptButtonClicked()
        }
        binding.cancelButton.setOnClickListener {
            presenter.onCancelButtonClicked()
        }
    }

    override fun accountDeleted() {
        val intent = Intent()
        intent.putExtra(EXTRA_ACCOUNT_DELETED, true)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun showLoading() {
        binding.acceptButton.visibility = View.INVISIBLE
        binding.cancelButton.visibility = View.INVISIBLE
        binding.dialogMessage.visibility = View.INVISIBLE
        binding.progressLayout.root.visibility = View.VISIBLE
        binding.progressLayout.progressText.text = getString(R.string.sending_messages_in_progress)
    }

    override fun hideLoading() {
        binding.acceptButton.visibility = View.VISIBLE
        binding.cancelButton.visibility = View.VISIBLE
        binding.dialogMessage.visibility = View.VISIBLE
        binding.progressLayout.root.visibility = View.GONE
    }

    private fun chooseAccountDeleteDialogMsg(step: RemoveAccountStep, accountDescription: String): String {
        return when(step) {
            RemoveAccountStep.MESSAGES_IN_OUTBOX -> getString(R.string.account_delete_dlg_messages_in_outbox_instructions_fmt, accountDescription)
            RemoveAccountStep.SEND_FAILED -> getString(R.string.account_delete_dlg_after_send_failed_instructions_fmt)
            else -> getString(R.string.account_delete_dlg_instructions_fmt, accountDescription)
        }
    }

    private fun chooseAccountDeleteButtonText(step: RemoveAccountStep): String {
        return when(step) {
            RemoveAccountStep.MESSAGES_IN_OUTBOX -> getString(R.string.send_all_messages_and_remove_account_action)
            else -> getString(R.string.okay_action)
        }
    }

    override fun showDialogAtStep(step: RemoveAccountStep, accountDescription: String) {
        binding.dialogMessage.text = chooseAccountDeleteDialogMsg(step, accountDescription)
        binding.acceptButton.text = chooseAccountDeleteButtonText(step)
    }

    companion object {
        const val EXTRA_ACCOUNT_DELETED = "extra_account_deleted"
        const val ACTIVITY_REQUEST_REMOVE_ACCOUNT = 10013

        private const val EXTRA_ACCOUNT_UUID = "account_uuid"

        fun start(activity: FragmentActivity, account: Account) {
            val intent = Intent(activity, RemoveAccountActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT_UUID, account.uuid)
            activity.startActivityForResult(
                intent, ACTIVITY_REQUEST_REMOVE_ACCOUNT
            )
        }
    }
}