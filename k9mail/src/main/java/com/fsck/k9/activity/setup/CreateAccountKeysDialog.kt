package com.fsck.k9.activity.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.fsck.k9.activity.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import security.planck.ui.passphrase.newaccount.CreateAccountKeysDialogContent

private const val ARG_ACCOUNT_UUID =
    "com.fsck.k9.activity.setup.CreateAccountKeysDialog.accountUuid"
private const val ARG_MANUAL_SETUP =
    "com.fsck.k9.activity.setup.CreateAccountKeysDialog.manualSetup"

@AndroidEntryPoint
class CreateAccountKeysDialog : DialogFragment() {
    private val createAccountKeysViewModel: CreateAccountKeysViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            createAccountKeysViewModel.initialize(
                arguments?.getString(ARG_ACCOUNT_UUID) ?: error("account missing"),
                arguments?.getBoolean(ARG_MANUAL_SETUP) ?: false
            )
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
                    CreateAccountKeysDialogContent(
                        viewModel = createAccountKeysViewModel,
                        successDismiss = {
                            SettingsActivity.listAccountsOnStartup(requireContext().applicationContext)
                            dismissAllowingStateLoss()
                        },
                        errorDismiss = {
                            dismissAllowingStateLoss()
                        },
                        cancel = {
                            dismissAllowingStateLoss()
                        },
                    )
                }
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
    }

    companion object {
        const val DIALOG_TAG = "com.fsck.k9.activity.setup.CreateAccountKeysDialog"
    }
}

private fun newInstance(
    accountUuid: String,
    manualSetup: Boolean,
): CreateAccountKeysDialog = CreateAccountKeysDialog().apply {
    arguments = Bundle().apply {
        putString(ARG_ACCOUNT_UUID, accountUuid)
        putBoolean(ARG_MANUAL_SETUP, manualSetup)
    }
}

private fun createAndShowreateAccountKeysDialog(
    fragmentManager: FragmentManager,
    accountUuid: String,
    manualSetup: Boolean,
) {
    val fragment = newInstance(
        accountUuid,
        manualSetup,
    )
    fragmentManager
        .beginTransaction()
        .add(fragment, CreateAccountKeysDialog.DIALOG_TAG)
        .commitAllowingStateLoss()
}

fun AppCompatActivity.showCreateAccountKeysDialog(
    accountUuid: String,
    manualSetup: Boolean,
) {
    createAndShowreateAccountKeysDialog(
        supportFragmentManager,
        accountUuid,
        manualSetup,
    )
}
