package com.fsck.k9.planck.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.fsck.k9.R
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.databinding.FragmentAccountSelectAuthBinding
import com.fsck.k9.planck.infrastructure.extensions.showTermsAndConditions
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator
import dagger.hilt.android.AndroidEntryPoint
import security.planck.provisioning.AccountMailSettingsProvision

@AndroidEntryPoint
class AccountSetupSelectAuthFragment : AccountSetupBasicsFragmentBase() {

    private var _binding: FragmentAccountSelectAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleButton: Button
    private lateinit var microsoftButton: Button
    private lateinit var passwordFlowButton: Button
    private lateinit var googleButtonCard: View
    private lateinit var microsoftButtonCard: View
    private lateinit var passwordFlowButtonCard: View
    private lateinit var termsAndConditionTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSelectAuthBinding.inflate(inflater, container, false)
        setupViews()
        if (k9.isRunningOnWorkProfile) {
            updateUiFromProvisioningSettings()
        }
        return binding.root
    }

    private fun setupViews() {
        googleButton = binding.googleSignInButton
        microsoftButton = binding.microsoftSignInButton
        passwordFlowButton = binding.otherMethodSignInButton
        googleButtonCard = binding.googleSignInButtonCard
        microsoftButtonCard = binding.microsoftSignInButtonCard
        passwordFlowButtonCard = binding.otherMethodSignInButtonCard
        termsAndConditionTextView = binding.termsAndConditions

        googleButton.setOnClickListener { navigator.goToGoogleAuthGuideStep1(parentFragmentManager) }
        microsoftButton.setOnClickListener { startMicrosoftFlow() }
        passwordFlowButton.setOnClickListener { startPasswordFlow() }
    }

    private fun updateUiFromProvisioningSettings() {
        accountProvisioningSettings?.provisionedMailSettings?.let { mailSettings ->
            binding.pleaseChooseSignInOption.isVisible = false
            val buttonsToHide =
                getButtonsToHide(mailSettings, accountProvisioningSettings?.oAuthType)
            hideViews(*buttonsToHide)
        }
    }

    private fun getButtonsToHide(
        mailSettings: AccountMailSettingsProvision,
        oAuthType: OAuthProviderType?
    ): Array<View> {
        return when (mailSettings.incoming.authType) {
            security.planck.mdm.AuthType.XOAUTH2 ->
                arrayOf(
                    passwordFlowButtonCard,
                    if (oAuthType == OAuthProviderType.GOOGLE) microsoftButtonCard
                    else googleButtonCard
                )

            else ->
                arrayOf(googleButtonCard, microsoftButtonCard)
        }
    }

    private fun hideViews(vararg views: View) {
        views.forEach { it.isVisible = false }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator.setCurrentStep(AccountSetupNavigator.Step.SELECT_AUTH, null)
        configureSelectAuthScreen()
    }

    private fun configureSelectAuthScreen() {
        setWelcomeBackground()
        hideNavigationBar()
        setupTermsAndConditionsView()
    }

    private fun setupTermsAndConditionsView() {
        termsAndConditionTextView.text = HtmlCompat.fromHtml(
            "<a href=\"#\">Terms and Conditions</a>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        termsAndConditionTextView.setOnClickListener {
            activity?.showTermsAndConditions()
        }
    }

    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView
        ).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun setWelcomeBackground() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        requireView().background =
            ContextCompat.getDrawable(requireContext(), R.drawable.background_startup)
        val window: Window = requireActivity().window
        val background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_startup)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        window.statusBarColor =
            ContextCompat.getColor(requireActivity(), android.R.color.transparent)
        window.navigationBarColor =
            ContextCompat.getColor(requireActivity(), android.R.color.transparent)
        window.setBackgroundDrawable(background)
        requireActivity().findViewById<View>(R.id.toolbar).isVisible = false
    }

    private fun startPasswordFlow() {
        navigator.goToAccountSetupBasicsFragment(parentFragmentManager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}