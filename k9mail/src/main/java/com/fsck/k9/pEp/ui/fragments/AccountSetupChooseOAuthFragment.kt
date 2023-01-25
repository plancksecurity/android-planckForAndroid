package com.fsck.k9.pEp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.auth.OAuthProviderType

class AccountSetupChooseOAuthFragment: PEpFragment() {
    private lateinit var googleButton: Button
    private lateinit var microsoftButton: Button
    private lateinit var otherMethodButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupPEpFragmentToolbar()
        val rootView = inflater.inflate(R.layout.fragment_account_choose_oauth, container, false)
        googleButton = rootView.findViewById(R.id.google_sign_in_button)
        microsoftButton = rootView.findViewById(R.id.microsoft_sign_in_button)
        otherMethodButton = rootView.findViewById(R.id.other_method_sign_in_button)
        setupToolbar()
        setupClickListeners()
        return rootView
    }

    private fun setupClickListeners() {
        googleButton.setOnClickListener {
            goToAccountSetupBasicsFragment(OAuthProviderType.GOOGLE)
        }

        microsoftButton.setOnClickListener {
            goToAccountSetupBasicsFragment(OAuthProviderType.MICROSOFT)
        }

        otherMethodButton.setOnClickListener {
            goToAccountSetupBasicsFragment(null)
        }
    }

    private fun goToAccountSetupBasicsFragment(oAuthProviderType: OAuthProviderType?) {
        val fragment = AccountSetupBasicsFragment.newInstance(oAuthProviderType)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
            .replace(
                R.id.account_setup_container,
                fragment,
                "accountSetupBasicsFragment"
            )
            .addToBackStack(null)
            .commit()
    }

    private fun setupToolbar() {
        (requireActivity() as AccountSetupBasics).initializeToolbar(
            !requireActivity().isTaskRoot,
            R.string.account_setup_basics_title
        )
    }

    override fun injectFragment() {

    }
}