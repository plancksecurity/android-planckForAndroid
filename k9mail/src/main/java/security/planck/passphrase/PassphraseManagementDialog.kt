package security.planck.passphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PassphraseManagementDialog : DialogFragment() {
    private val viewModel: PassphraseManagementViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                PassphraseManagementDialogContent(
                    onCancel = ::dismissAllowingStateLoss,
                    onConfirm = ::dismissAllowingStateLoss,
                    viewModel = viewModel,
                )
            }
        }
        return composeView
    }
}
