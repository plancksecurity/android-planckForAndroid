package com.fsck.k9.ui.settings.account.remove

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.fsck.k9.R
import com.fsck.k9.databinding.ActivityRemoveAccountBinding
import javax.inject.Inject

class RemoveAccountViewImpl @Inject constructor(): RemoveAccountView {
    private lateinit var binding: ActivityRemoveAccountBinding
    private lateinit var context: Context
    private lateinit var acceptButton: Button
    private lateinit var cancelButton: Button
    private lateinit var dialogMessage: TextView
    private lateinit var progressLayout: View
    private lateinit var progressText: TextView
    private lateinit var title: TextView

    override fun initialize(
        binding: ActivityRemoveAccountBinding,
        onAcceptButtonClicked: () -> Unit,
        onCancelButtonClicked: () -> Unit
    ) {
        this.binding = binding
        this.context = binding.root.context

        locateViews(binding)

        title.text = context.getString(R.string.account_delete_dlg_title)
        binding.acceptButton.setOnClickListener {
            onAcceptButtonClicked()
        }
        binding.cancelButton.setOnClickListener {
            onCancelButtonClicked()
        }
    }

    private fun locateViews(binding: ActivityRemoveAccountBinding) {
        acceptButton = binding.acceptButton
        cancelButton = binding.cancelButton
        dialogMessage = binding.dialogMessage
        progressLayout = binding.progressLayout.root
        progressText = binding.progressLayout.progressText
        title = binding.title
    }

    override fun showLoading(step: RemoveAccountStep) {
        acceptButton.visibility = View.INVISIBLE
        cancelButton.visibility = View.INVISIBLE
        dialogMessage.visibility = View.INVISIBLE
        progressLayout.visibility = View.VISIBLE
        progressText.text = chooseLoadingText(step)
    }

    override fun hideLoading() {
        acceptButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        dialogMessage.visibility = View.VISIBLE
        progressLayout.visibility = View.GONE
    }

    private fun chooseAccountDeleteDialogMsg(step: RemoveAccountStep, accountDescription: String): String {
        return when(step) {
            RemoveAccountStep.MESSAGES_IN_OUTBOX -> context.getString(R.string.account_delete_dlg_messages_in_outbox_instructions_fmt, accountDescription)
            RemoveAccountStep.SEND_FAILED -> context.getString(R.string.account_delete_dlg_after_send_failed_instructions_fmt)
            else -> context.getString(R.string.account_delete_dlg_instructions_fmt, accountDescription)
        }
    }

    private fun chooseAccountDeleteButtonText(step: RemoveAccountStep): String {
        return when(step) {
            RemoveAccountStep.MESSAGES_IN_OUTBOX -> context.getString(R.string.send_all_messages_and_remove_account_action)
            else -> context.getString(R.string.okay_action)
        }
    }

    private fun chooseLoadingText(step: RemoveAccountStep): String {
        return when(step) {
            RemoveAccountStep.INITIAL -> context.getString(R.string.account_delete_dlg_retrieving_data)
            RemoveAccountStep.CHECKING_FOR_MESSAGES -> context.getString(R.string.account_delete_dlg_checking_pending_messages)
            RemoveAccountStep.SENDING_MESSAGES -> context.getString(R.string.sending_messages_in_progress)
            else -> ""
        }
    }

    override fun showDialogAtStep(step: RemoveAccountStep, accountDescription: String) {
        dialogMessage.text = chooseAccountDeleteDialogMsg(step, accountDescription)
        acceptButton.text = chooseAccountDeleteButtonText(step)
    }
}