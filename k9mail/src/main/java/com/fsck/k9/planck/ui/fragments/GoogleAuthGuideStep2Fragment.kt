package com.fsck.k9.planck.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fsck.k9.databinding.FragmentAccountSetupGoogleGuide1Binding
import com.fsck.k9.databinding.FragmentAccountSetupGoogleGuide2Binding
import com.fsck.k9.databinding.WizardSetupBinding
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator

class GoogleAuthGuideStep2Fragment : AccountSetupBasicsFragmentBase() {
    private var _binding: FragmentAccountSetupGoogleGuide2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSetupGoogleGuide2Binding.inflate(inflater, container, false)
        setupViews()
        return binding.root
    }

    private fun setupViews() {
        binding.googleSignInButton.setOnClickListener { startGoogleFlow() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator.setCurrentStep(AccountSetupNavigator.Step.GOOGLE_GUIDE_STEP_1, null)
    }
}