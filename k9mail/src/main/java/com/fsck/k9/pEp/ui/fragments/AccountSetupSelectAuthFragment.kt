package com.fsck.k9.pEp.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.activity.setup.OAuthFlowActivity
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.databinding.FragmentAccountSelectAuthBinding
import com.fsck.k9.mail.AuthType
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator

class AccountSetupSelectAuthFragment : AccountSetupBasicsFragmentBase() {

    private var _binding: FragmentAccountSelectAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleButton: Button
    private lateinit var microsoftButton: Button
    private lateinit var passwordFlowButton: Button
    private lateinit var termsAndConditionTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSelectAuthBinding.inflate(inflater, container, false)
        setupViews()
        return binding.root
    }

    private fun setupViews() {
        googleButton = binding.googleSignInButton
        microsoftButton = binding.microsoftSignInButton
        passwordFlowButton = binding.otherMethodSignInButton
        termsAndConditionTextView = binding.termsAndConditions

        googleButton.setOnClickListener { startGoogleFlow() }
        microsoftButton.setOnClickListener { startMicrosoftFlow() }
        passwordFlowButton.setOnClickListener { startPasswordFlow() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator.setCurrentStep(AccountSetupNavigator.Step.SELECT_AUTH, null)
        configureSelectAuthScreen()
    }

    private fun configureSelectAuthScreen() {
        setWelcomeBackground()
        hideNavigationBar()
    }

    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView
        ).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
        }
        termsAndConditionTextView.text = HtmlCompat.fromHtml(
            "<a href=\"#\">Terms and Conditions</a>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun setWelcomeBackground() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        requireView().background =
            ContextCompat.getDrawable(requireContext(), R.drawable.background_startup)
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.startup_gradient_start)
        requireActivity().findViewById<View>(R.id.toolbar).isVisible = false
    }

    private fun startGoogleFlow() {
        startOAuthFlow(OAuthProviderType.GOOGLE)
    }

    private fun startMicrosoftFlow() {
        startOAuthFlow(OAuthProviderType.MICROSOFT)
    }

    private fun startOAuthFlow(oAuthProviderType: OAuthProviderType) {
        val email = if (k9.isRunningOnWorkProfile) provisioningSettings.email else null
        val account = initAccount(email).also { it.mandatoryOAuthProviderType = oAuthProviderType }
        val intent = OAuthFlowActivity.buildLaunchIntent(requireContext(), account.uuid)
        requireActivity().startActivityForResult(intent, REQUEST_CODE_OAUTH)
    }

    private fun startPasswordFlow() {
        navigator.goToAccountSetupBasicsFragment(parentFragmentManager)
    }

    override fun onManualSetup(fromUser: Boolean) {
        (requireActivity() as AccountSetupBasics).setManualSetupRequired(true)
        val account = retrieveAccount() ?: error("Account is null!!")
        if (account.storeUri == null || account.transportUri == null) {
            setDefaultSettingsForManualSetup(account)
        }
        goForward()
    }

    private fun setDefaultSettingsForManualSetup(account: Account) {
        val email = account.email ?: let {
            account.email = DEFAULT_EMAIL
            account.email
        }
        val connectionSettings =
            defaultConnectionSettings(email, null, null, AuthType.PLAIN)
        account.setMailSettings(requireContext(), connectionSettings, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!isAdded) {
            return
        }
        if (requestCode == REQUEST_CODE_OAUTH) {
            handleSignInResult(resultCode)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleSignInResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_CANCELED) {
            deleteAccount()
            return
        }
        checkNotNull(account) { "Account instance missing" }
        checkSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    companion object {
        private const val REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1
        private const val DEFAULT_EMAIL = "mail@example.com"
    }
}