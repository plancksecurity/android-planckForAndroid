package com.fsck.k9.planck.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.databinding.FragmentAccountSetupGoogleGuide1Binding
import com.fsck.k9.databinding.WizardNextBinding
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator

class GoogleAuthGuideStep1Fragment : AccountSetupBasicsFragmentBase() {
    private var _binding: FragmentAccountSetupGoogleGuide1Binding? = null
    private val binding get() = _binding!!
    private var _wizardNextBinding: WizardNextBinding? = null
    private val wizardNextBinding get() = _wizardNextBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSetupGoogleGuide1Binding.inflate(inflater, container, false)
        _wizardNextBinding = WizardNextBinding.bind(binding.root)
        setupViews()
        setupToolbar()
        return binding.root
    }

    private fun setupViews() {
        wizardNextBinding.next.setOnClickListener { navigator.goToGoogleAuthGuideStep2(parentFragmentManager) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AccountSetupBasics).configurePasswordFlowScreen()
        navigator.setCurrentStep(AccountSetupNavigator.Step.GOOGLE_GUIDE_STEP_1, null)
    }
}