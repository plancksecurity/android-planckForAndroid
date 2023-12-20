package security.planck.ui.removeaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.fsck.k9.R
import com.fsck.k9.databinding.DialogRemoveAccountBinding
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_ACCOUNT_UUID =
    "security.planck.ui.removeaccount.RemoveAccountDialog.accountUuid"
private const val TAG = "security.planck.ui.removeaccount.RemoveAccountDialog"
private const val NO_RESOURCE = 0

@AndroidEntryPoint
class RemoveAccountDialog : DialogFragment() {
    private val viewModel: RemoveAccountViewModel by viewModels()

    private var _binding: DialogRemoveAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        arguments?.let { arguments ->
            val uuid = arguments.getString(ARG_ACCOUNT_UUID) ?: error("account uuid missing")
            viewModel.initialize(uuid)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRemoveAccountBinding.inflate(inflater)
        setupViews()
        return binding.root
    }

    private fun setupViews() {
        binding.afirmativeActionButton.setOnClickListener {
            viewModel.positiveAction()
        }
        binding.negativeActionButton.setOnClickListener {
            viewModel.negativeAction()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: RemoveAccountState) {
        when (state) {
            is RemoveAccountState.RemoveAccountConfirmation ->
                showScreen(
                    dialogMessage = getString(
                        R.string.account_delete_dlg_instructions_fmt,
                        state.accountDescription
                    ),
                    positiveButtonText = R.string.okay_action,
                    negativeButtonVisible = true
                )

            is RemoveAccountState.AccountNotAvailable ->
                showScreen(
                    dialogMessage = getString(
                        R.string.account_delete_dlg_account_not_available,
                        state.accountDescription
                    ),
                    positiveButtonText = R.string.close
                )

            is RemoveAccountState.Done ->
                showScreen(
                    dialogMessage = getString(
                        R.string.account_delete_dlg_success,
                        state.accountDescription
                    ),
                    positiveButtonText = R.string.close
                )

            RemoveAccountState.Idle ->
                showScreen(
                    progressMessage = getString(R.string.account_delete_dlg_retrieving_data)
                )

            RemoveAccountState.RemovingAccount ->
                showScreen(
                    progressMessage = getString(R.string.account_delete_dlg_removing_account)
                )

            is RemoveAccountState.Finish -> {
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACCOUNT_REMOVED to state.removed))
                dismissAllowingStateLoss()
            }
        }
    }

    private fun showScreen(
        dialogMessage: String = "",
        progressMessage: String = "",
        @StringRes positiveButtonText: Int = NO_RESOURCE,
        negativeButtonVisible: Boolean = false
    ) {
        binding.dialogMessage.text = dialogMessage
        binding.progressGroup.isVisible = progressMessage.isNotBlank()
        binding.progressText.text = progressMessage
        binding.negativeActionButton.isVisible = negativeButtonVisible
        binding.afirmativeActionButton.apply {
            isVisible =
                (positiveButtonText != NO_RESOURCE).also { if (it) setText(positiveButtonText) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = TAG
        const val RESULT_ACCOUNT_REMOVED =
            "security.planck.ui.removeaccount.RemoveAccountDialog.accountRemoved"
    }
}

private fun newInstance(
    uuid: String,
): RemoveAccountDialog = RemoveAccountDialog().apply {
    arguments = bundleOf(
        ARG_ACCOUNT_UUID to uuid
    )
}

fun AppCompatActivity.showRemoveAccountDialog(
    uuid: String,
) {
    val dialog = newInstance(uuid)
    supportFragmentManager
        .beginTransaction()
        .add(dialog, TAG)
        .commitAllowingStateLoss()
}