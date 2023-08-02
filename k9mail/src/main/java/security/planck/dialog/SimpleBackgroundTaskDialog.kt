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
                binding.acceptButton.isVisible = true
                binding.acceptButton.text = actionText
                binding.acceptButton.setOnClickListener {
                    taskTriggered()
                }
                binding.cancelButton.isVisible = true
                binding.cancelButton.setText(R.string.cancel_action)
                binding.cancelButton.setOnClickListener {
                    dialogFinished()
                    dismissAllowingStateLoss()
                }

                binding.loading.isVisible = false
                binding.loadingMessage.isVisible = false
                binding.description.isVisible = true
                binding.description.text = confirmationMessage
            }

            BackgroundTaskDialogView.State.ERROR -> {
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

            BackgroundTaskDialogView.State.LOADING -> {
                binding.loading.isVisible = true
                binding.description.visibility = View.INVISIBLE
                binding.loadingMessage.isVisible = true
                binding.loadingMessage.text = progressMessage
                binding.acceptButton.isVisible = false
                binding.cancelButton.isVisible = false
            }

            BackgroundTaskDialogView.State.SUCCESS -> {
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun dialogFinished()
    abstract fun taskTriggered()
    abstract fun dialogInitialized()
}