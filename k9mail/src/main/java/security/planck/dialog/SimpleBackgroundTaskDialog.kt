package security.planck.dialog

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.fsck.k9.R
import com.fsck.k9.databinding.DialogSimpleBackgroundTaskBinding


abstract class SimpleBackgroundTaskDialog : DialogFragment(), BackgroundTaskDialogView {
    private var _binding: DialogSimpleBackgroundTaskBinding? = null
    private val binding get() = _binding!!

    protected abstract val title: String
    protected abstract val progressMessage: String
    protected abstract val confirmationMessage: String
    protected abstract val failureMessage: String
    protected abstract val successMessage: String
    protected abstract val actionText: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSimpleBackgroundTaskBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCancelable(false)
        binding.title.text = title
        (requireView() as ViewGroup).layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
        dialogInitialized()
    }

    override fun showState(
        state: BackgroundTaskDialogView.State
    ) {
        when (state) {
            BackgroundTaskDialogView.State.CONFIRMATION -> {
                showConfirmationState()
            }

            BackgroundTaskDialogView.State.ERROR -> {
                showErrorState()
            }

            BackgroundTaskDialogView.State.LOADING -> {
                showLoadingState()
            }

            BackgroundTaskDialogView.State.SUCCESS -> {
                showSuccessState()
            }
        }
    }

    open fun showSuccessState() {
        binding.loading.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.description.isVisible = true
        binding.description.text = successMessage
        binding.acceptButton.isVisible = false
        binding.cancelButton.isVisible = true
        binding.cancelButton.setOnClickListener {
            dialogFinished()
            dismissAllowingStateLoss()
        }
        binding.cancelButton.setText(R.string.close)
    }

    open fun showLoadingState() {
        binding.loading.isVisible = true
        binding.description.visibility = View.INVISIBLE
        binding.loadingMessage.isVisible = true
        binding.loadingMessage.text = progressMessage
        binding.acceptButton.isVisible = false
        binding.cancelButton.isVisible = false
    }

    open fun showErrorState() {
        binding.loading.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.description.isVisible = true
        binding.description.text = failureMessage
        binding.acceptButton.isVisible = false
        binding.cancelButton.isVisible = true
        binding.cancelButton.setOnClickListener {
            dialogFinished()
            dismissAllowingStateLoss()
        }
        binding.cancelButton.setText(R.string.close)
    }

    open fun showConfirmationState() {
        binding.acceptButton.isVisible = true
        binding.acceptButton.text = actionText
        binding.acceptButton.setOnClickListener {
            taskTriggered()
        }
        binding.cancelButton.isVisible = true
        binding.cancelButton.setText(R.string.cancel_action)
        binding.cancelButton.setOnClickListener {
            dialogCancelled()
            dismissAllowingStateLoss()
        }

        binding.loading.isVisible = false
        binding.loadingMessage.isVisible = false
        binding.description.isVisible = true
        binding.description.text = confirmationMessage
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    open fun dialogFinished() {}
    open fun dialogCancelled() {
        dialogFinished()
    }
    abstract fun taskTriggered()
    open fun dialogInitialized() {}
}