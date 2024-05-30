package security.planck.passphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.fsck.k9.ui.getEnum
import com.fsck.k9.ui.putEnum
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "security.planck.passphrase.PassphraseManagementDialog"
private const val ARG_MODE = "security.planck.passphrase.PassphraseManagementDialog.Mode"
private const val ARG_ACCOUNTS_WITH_ERROR = "security.planck.passphrase.PassphraseManagementDialog.AccountsWithError"

@AndroidEntryPoint
class PassphraseManagementDialog : DialogFragment() {
    private val viewModel: PassphraseManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val args = requireArguments()
            val mode = args.getEnum<PassphraseDialogMode>(ARG_MODE)
            val accountsWithError = args.getStringArrayList(ARG_ACCOUNTS_WITH_ERROR)?.toList()
            viewModel.start(mode, accountsWithError)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                PassphraseManagementDialogContent(
                    viewModel = viewModel,
                    onCancel = ::dismissAllowingStateLoss,
                    onConfirm = ::dismissAllowingStateLoss,
                    dismiss = ::dismissAllowingStateLoss,
                )
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
    }
}

private fun newInstance(
    mode: PassphraseDialogMode,
    accountsWithError: List<String>,
): PassphraseManagementDialog = PassphraseManagementDialog().apply {
    arguments = Bundle().apply {
        putEnum(ARG_MODE, mode)
        putStringArrayList(ARG_ACCOUNTS_WITH_ERROR, ArrayList(accountsWithError))
    }
}

private fun createAndShowPassphraseManagementDialog(
    fragmentManager: FragmentManager,
    mode: PassphraseDialogMode,
    accountsWithError: List<String>
) {
    val fragment = newInstance(
        mode,
        accountsWithError
    )
    fragmentManager
        .beginTransaction()
        .add(fragment, TAG)
        .commitAllowingStateLoss()
}

fun Fragment.showPassphraseManagementDialog(
    mode: PassphraseDialogMode,
    accountsWithError: List<String> = emptyList(),
) {
    createAndShowPassphraseManagementDialog(
        parentFragmentManager,
        mode,
        accountsWithError,
    )
}

@JvmOverloads
fun AppCompatActivity.showPassphraseManagementDialog(
    mode: PassphraseDialogMode,
    accountsWithError: List<String> = emptyList(),
) {
    createAndShowPassphraseManagementDialog(
        supportFragmentManager,
        mode,
        accountsWithError,
    )
}
