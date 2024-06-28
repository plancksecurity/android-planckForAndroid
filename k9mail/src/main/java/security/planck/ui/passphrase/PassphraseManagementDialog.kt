package security.planck.ui.passphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.fsck.k9.ui.getEnum
import com.fsck.k9.ui.putEnum
import dagger.hilt.android.AndroidEntryPoint
import security.planck.ui.passphrase.unlock.compose.PassphraseUnlockDialogContent
import security.planck.ui.passphrase.manage.PassphraseManagementViewModel
import security.planck.ui.passphrase.manage.compose.PassphraseManagementDialogContent
import security.planck.ui.passphrase.unlock.PassphraseUnlockViewModel
import kotlin.system.exitProcess

private const val TAG = "security.planck.passphrase.PassphraseManagementDialog"
private const val ARG_MODE = "security.planck.passphrase.PassphraseManagementDialog.Mode"

@AndroidEntryPoint
class PassphraseManagementDialog : DialogFragment() {
    private val managementViewModel: PassphraseManagementViewModel by viewModels()
    private val unlockViewModel: PassphraseUnlockViewModel by viewModels()
    private val mode: PassphraseDialogMode
        get() = requireArguments().getEnum<PassphraseDialogMode>(
            ARG_MODE
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            when (mode) {
                PassphraseDialogMode.MANAGE -> managementViewModel.start()
                PassphraseDialogMode.UNLOCK -> unlockViewModel.start()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    when (mode) {
                        PassphraseDialogMode.UNLOCK -> PassphraseUnlockDialogContent(
                            viewModel = unlockViewModel,
                            dismiss = ::dismissAllowingStateLoss,
                            finishApp = ::finishApp,
                        )
                        PassphraseDialogMode.MANAGE -> PassphraseManagementDialogContent(
                            viewModel = managementViewModel,
                            dismiss = ::dismissAllowingStateLoss,
                        )
                    }
                }
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
    }

    private fun finishApp() {
        activity?.finishAndRemoveTask()
        exitProcess(0)
    }
}

private fun newInstance(
    mode: PassphraseDialogMode,
): PassphraseManagementDialog = PassphraseManagementDialog().apply {
    arguments = Bundle().apply {
        putEnum(ARG_MODE, mode)
    }
}

private fun createAndShowPassphraseManagementDialog(
    fragmentManager: FragmentManager,
    mode: PassphraseDialogMode,
) {
    val fragment = newInstance(
        mode,
    )
    fragmentManager
        .beginTransaction()
        .add(fragment, TAG)
        .commitAllowingStateLoss()
}

fun Fragment.showPassphraseManagementDialog(
    mode: PassphraseDialogMode,
) {
    createAndShowPassphraseManagementDialog(
        parentFragmentManager,
        mode,
    )
}

fun AppCompatActivity.showPassphraseManagementDialog(
    mode: PassphraseDialogMode,
) {
    createAndShowPassphraseManagementDialog(
        supportFragmentManager,
        mode,
    )
}
