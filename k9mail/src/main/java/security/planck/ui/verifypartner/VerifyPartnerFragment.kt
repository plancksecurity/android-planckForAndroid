package security.planck.ui.verifypartner

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.fsck.k9.R
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.databinding.FragmentVerifyPartnerBinding
import com.fsck.k9.planck.ui.tools.ThemeManager
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "security.planck.ui.verifypartner.VerifyPartnerFragment"
private const val ARG_SENDER = "security.planck.ui.verifypartner.VerifyPartnerFragment.sender"
private const val ARG_MYSELF = "security.planck.ui.verifypartner.VerifyPartnerFragment.myself"
private const val ARG_MESSAGE_REFERENCE =
    "security.planck.ui.verifypartner.VerifyPartnerFragment.messageReference"
private const val ARG_MESSAGE_DIRECTION =
    "security.planck.ui.verifypartner.VerifyPartnerFragment.messageDirection"
private const val ENGLISH_POSITION = 0
private const val GERMAN_POSITION = 1
private const val NO_RESOURCE = 0

@AndroidEntryPoint
class VerifyPartnerFragment : DialogFragment() {
    private val viewModel: VerifyPartnerViewModel by viewModels()
    private var _binding: FragmentVerifyPartnerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            arguments?.let { arguments ->
                val sender = arguments.getString(ARG_SENDER) ?: error("sender missing")
                val myself = arguments.getString(ARG_MYSELF) ?: error("myself missing")
                val messageReference = MessageReference.parse(
                    arguments.getString(ARG_MESSAGE_REFERENCE) ?: error("message reference missing")
                ) ?: error("wrong message reference")
                val isMessageIncoming = arguments.getBoolean(ARG_MESSAGE_DIRECTION)

                viewModel.initialize(sender, myself, messageReference, isMessageIncoming)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyPartnerBinding.inflate(inflater)

        setupViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireView() as ViewGroup).layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
        dialog?.setCancelable(false)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: VerifyPartnerState) {
        when (state) {
            VerifyPartnerState.LoadingHandshakeData ->
                showLoadingHandshakeData()

            is VerifyPartnerState.HandshakeReady ->
                renderHandshakeData(state)

            is VerifyPartnerState.ConfirmTrust ->
                showTrustConfirmation(state)

            is VerifyPartnerState.ConfirmMistrust ->
                showMistrustConfirmation(state)

            is VerifyPartnerState.TrustProgress ->
                showTrustProgress(state)

            is VerifyPartnerState.MistrustProgress ->
                showMistrustProgress(state)

            is VerifyPartnerState.TrustDone ->
                showTrustDone(state)

            is VerifyPartnerState.MistrustDone ->
                showMistrustDone(state)

            VerifyPartnerState.ErrorLoadingMessage ->
                showErrorLoadingMessage()

            VerifyPartnerState.ErrorGettingTrustwords ->
                showErrorGettingTrustwords()

            is VerifyPartnerState.ErrorTrusting ->
                showErrorTrusting(state)

            is VerifyPartnerState.ErrorMistrusting ->
                showErrorMistrusting(state)

            VerifyPartnerState.DeletedMessage -> {
                showMessageNoLongerAvailable()
            }

            is VerifyPartnerState.Finish -> {
                setFragmentResult(REQUEST_KEY, state.result.toBundle())
                dismissAllowingStateLoss()
            }

            VerifyPartnerState.Idle -> {}
        }
    }

    private fun showMessageNoLongerAvailable() {
        configureButtonsJustClose(getString(R.string.message_view_message_no_longer_available))
    }

    private fun showErrorMistrusting(state: VerifyPartnerState.ErrorMistrusting) {
        configureButtonsJustClose(getString(R.string.reject_trust_dialog_failure, state.partner))
    }

    private fun showErrorTrusting(state: VerifyPartnerState.ErrorTrusting) {
        configureButtonsJustClose(getString(R.string.confirm_trust_dialog_failure, state.partner))
    }

    private fun showErrorGettingTrustwords() {
        configureButtonsJustClose(getString(R.string.verify_partner_dialog_error_getting_trustwords))
    }

    private fun showErrorLoadingMessage() {
        configureButtonsJustClose(getString(R.string.status_loading_error))
    }

    private fun showMistrustDone(state: VerifyPartnerState.MistrustDone) {
        configureButtonsJustClose(getString(R.string.reject_trust_dialog_success, state.partner))
    }

    private fun showTrustDone(state: VerifyPartnerState.TrustDone) {
        configureButtonsJustClose(getString(R.string.confirm_trust_dialog_success, state.partner))
    }

    private fun configureButtonsJustClose(description: String) {
        showScreen(
            description = description,
            positiveButtonText = R.string.close,
            positiveButtonClick = viewModel::finish
        )
    }

    private fun showMistrustProgress(state: VerifyPartnerState.MistrustProgress) {
        showScreen(
            progressText = getString(R.string.reject_trust_dialog_progress, state.partner)
        )
    }

    private fun showTrustProgress(state: VerifyPartnerState.TrustProgress) {
        showScreen(
            progressText = getString(R.string.confirm_trust_dialog_progress, state.partner)
        )
    }

    private fun showMistrustConfirmation(state: VerifyPartnerState.ConfirmMistrust) {
        binding.toolbar.menu.clear()
        showScreen(
            description = getString(R.string.reject_trust_dialog_confirmation, state.partner),
            positiveButtonText = R.string.reject_trust_dialog_positive_action,
            negativeButtonText = R.string.verify_partner_dialog_go_back,
            dismissButtonVisible = true,
        )
        binding.negativeActionButton.setTextColorAttr(R.attr.colorAccent)
        binding.dissmissActionButton.setTextColorAttr(R.attr.colorAccent)
    }

    private fun showTrustConfirmation(state: VerifyPartnerState.ConfirmTrust) {
        binding.toolbar.menu.clear()
        showScreen(
            description = getString(R.string.confirm_trust_dialog_confirmation, state.partner),
            positiveButtonText = R.string.confirm_trust_dialog_positive_action,
            negativeButtonText = R.string.verify_partner_dialog_go_back,
            dismissButtonVisible = true,
        )
        binding.negativeActionButton.setTextColorAttr(R.attr.colorAccent)
        binding.dissmissActionButton.setTextColorAttr(R.attr.colorAccent)
    }

    private fun renderHandshakeData(state: VerifyPartnerState.HandshakeReady) {
        showScreen(
            trustwords = state.trustwords,
            ownFprTitle = state.myself,
            partnerFprTitle = state.partner,
            ownFpr = state.ownFpr,
            partnerFpr = state.partnerFpr,
            description = getString(R.string.pep_ask_trustwords),
            positiveButtonText = if (state.allowChangeTrust) R.string.pep_confirm_trustwords else NO_RESOURCE,
            negativeButtonText = if (state.allowChangeTrust) R.string.key_import_reject else NO_RESOURCE,
            dismissButtonVisible = true,
            dismissButtonText = if (state.allowChangeTrust)
                R.string.keysync_wizard_action_cancel
            else R.string.close
        )
        binding.negativeActionButton.setTextColorColor(R.color.planck_red)
        binding.dissmissActionButton.setTextColorAttr(R.attr.defaultColorOnBackground)
        setupMenu()
    }

    private fun showLoadingHandshakeData() {
        binding.toolbar.menu.clear()
        binding.progressText.setText(R.string.message_list_loading)
        binding.progressGroup.isVisible = true
    }

    private fun showScreen(
        description: String = "",
        ownFprTitle: String = "",
        partnerFprTitle: String = "",
        ownFpr: String = "",
        partnerFpr: String = "",
        trustwords: String = "",
        shortTrustwords: Boolean = true,
        progressText: String = "",
        negativeButtonVisible: Boolean = false,
        dismissButtonVisible: Boolean = false,
        @StringRes positiveButtonText: Int = NO_RESOURCE,
        @StringRes negativeButtonText: Int = NO_RESOURCE,
        @StringRes dismissButtonText: Int = R.string.keysync_wizard_action_cancel,
        positiveButtonClick: () -> Unit = { viewModel.positiveAction() },
    ) {
        binding.description.apply {
            isVisible = (description.isNotBlank()).also { if (it) text = description }
        }
        if (ownFpr.isNotBlank()) {
            showFprs(ownFprTitle, partnerFprTitle, ownFpr, partnerFpr)
        } else {
            binding.fprGroup.isVisible = false
        }
        if (trustwords.isNotBlank()) {
            showTrustwords(trustwords)
            binding.showLongTrustwords.isVisible = shortTrustwords
        } else {
            binding.trustwordsGroup.isVisible = false
        }

        binding.progressGroup.isVisible = progressText.isNotBlank()
        binding.progressText.text = progressText
        binding.negativeActionButton.isVisible = negativeButtonVisible
        binding.negativeActionButton.apply {
            isVisible =
                (negativeButtonText != NO_RESOURCE).also { if (it) setText(negativeButtonText) }
        }
        binding.dissmissActionButton.isVisible = dismissButtonVisible
        binding.dissmissActionButton.setText(dismissButtonText)
        binding.afirmativeActionButton.apply {
            isVisible =
                (positiveButtonText != NO_RESOURCE).also { if (it) setText(positiveButtonText) }
            setOnClickListener { positiveButtonClick() }
        }
    }

    private fun showTrustwords(trustwords: String) {
        binding.trustwordsGroup.isVisible = true
        binding.trustwords.text = trustwords
    }

    private fun showFprs(
        ownTitle: String,
        partnerTitle: String,
        ownFpr: String,
        partnerFpr: String
    ) {
        binding.fprGroup.isVisible = true
        binding.fprCurrentAccountTitle.text = ownTitle
        binding.fprPartnerAccountTitle.text = partnerTitle
        binding.fprCurrentAccountValue.text = ownFpr
        binding.fprPartnerAccountValue.text = partnerFpr
    }

    private fun setupViews() {
        binding.afirmativeActionButton.setOnClickListener {
            viewModel.positiveAction()
        }
        binding.negativeActionButton.setOnClickListener {
            viewModel.negativeAction()
        }
        binding.dissmissActionButton.setOnClickListener { viewModel.finish() }
        binding.showLongTrustwords.setOnClickListener {
            binding.showLongTrustwords.isVisible = false
            viewModel.switchTrustwordsLength()
        }
        binding.trustwords.setOnLongClickListener {
            viewModel.switchTrustwordsLength()
            true
        }
    }

    private fun setupMenu() {
        binding.toolbar.menu.clear()
        binding.toolbar.inflateMenu(R.menu.menu_add_device)
        binding.toolbar.overflowIcon =
            ThemeManager.getDrawableFromAttributeResource(requireContext(), R.attr.iconLanguageGray)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.english -> viewModel.changeTrustwordsLanguage(ENGLISH_POSITION)
                R.id.german -> viewModel.changeTrustwordsLanguage(GERMAN_POSITION)
            }
            true
        }
    }

    private fun TextView.setTextColorAttr(@AttrRes attrId: Int) {
        setTextColor(ThemeManager.getColorFromAttributeResource(requireContext(), attrId))
    }

    private fun TextView.setTextColorColor(@ColorRes colorId: Int) {
        setTextColor(ContextCompat.getColor(requireContext(), colorId))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Map<String, Any?>.toBundle(): Bundle =
        bundleOf(*map { it.key to it.value }.toTypedArray())

    companion object {
        const val REQUEST_KEY = TAG
        const val RESULT_KEY_RATING =
            "security.planck.ui.verifypartner.VerifyPartnerFragment.Rating"
    }
}

private fun newInstance(
    sender: String,
    myself: String,
    messageReference: MessageReference,
    isMessageIncoming: Boolean,
): VerifyPartnerFragment = VerifyPartnerFragment().apply {
    arguments = bundleOf(
        ARG_SENDER to sender,
        ARG_MYSELF to myself,
        ARG_MESSAGE_REFERENCE to messageReference.toIdentityString(),
        ARG_MESSAGE_DIRECTION to isMessageIncoming,
    )
}

private fun createAndShowVerifyPartnerDialog(
    fragmentManager: FragmentManager,
    sender: String,
    myself: String,
    messageReference: MessageReference,
    isMessageIncoming: Boolean,
) {
    val fragment = newInstance(
        sender, myself, messageReference, isMessageIncoming
    )
    fragmentManager
        .beginTransaction()
        .add(fragment, TAG)
        .commitAllowingStateLoss()
}

fun Fragment.showVerifyPartnerDialog(
    sender: String,
    myself: String,
    messageReference: MessageReference,
    isMessageIncoming: Boolean,
) {
    createAndShowVerifyPartnerDialog(
        parentFragmentManager,
        sender, myself, messageReference, isMessageIncoming
    )
}

fun AppCompatActivity.showVerifyPartnerDialog(
    sender: String,
    myself: String,
    messageReference: MessageReference,
    isMessageIncoming: Boolean,
) {
    createAndShowVerifyPartnerDialog(
        supportFragmentManager,
        sender, myself, messageReference, isMessageIncoming
    )
}

