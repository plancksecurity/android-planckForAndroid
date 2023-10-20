package security.planck.ui.verifypartner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
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
//        if (savedInstanceState == null) {
//            arguments?.let {arguments ->
//                val sender = arguments.getString(ARG_SENDER) ?: error("sender missing")
//                val myself = arguments.getString(ARG_MYSELF) ?: error("myself missing")
//                val messageReference = MessageReference.parse(
//                    arguments.getString(ARG_MESSAGE_REFERENCE) ?: error("message reference missing")
//                ) ?: error("wrong message reference")
//                val isMessageIncoming = arguments.getBoolean(ARG_MESSAGE_DIRECTION)
//
//                viewModel.initialize(sender, myself, messageReference, isMessageIncoming)
//            }
//        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.rating
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

            VerifyPartnerState.ConfirmTrust ->
                showTrustConfirmation()

            VerifyPartnerState.ConfirmMistrust ->
                showMistrustConfirmation()

            VerifyPartnerState.TrustProgress ->
                showTrustProgress()

            VerifyPartnerState.MistrustProgress ->
                showMistrustProgress()

            VerifyPartnerState.TrustDone ->
                showTrustDone()

            VerifyPartnerState.MistrustDone ->
                showMistrustDone()

            VerifyPartnerState.ErrorLoadingMessage ->
                showErrorLoadingMessage()

            VerifyPartnerState.ErrorGettingTrustwords ->
                showErrorGettingTrustwords()

            VerifyPartnerState.ErrorTrusting ->
                showErrorTrusting()

            VerifyPartnerState.ErrorMistrusting ->
                showErrorMistrusting()

            VerifyPartnerState.Idle -> {}
        }
    }

    private fun showErrorMistrusting() {
        binding.progressGroup.isVisible = false
        binding.description.text =
            getString(R.string.reject_trust_dialog_failure, viewModel.partnerEmail)
        configureButtonsJustClose()
    }

    private fun showErrorTrusting() {
        binding.progressGroup.isVisible = false
        binding.description.text =
            getString(R.string.confirm_trust_dialog_failure, viewModel.partnerEmail)
        configureButtonsJustClose()
    }

    private fun showErrorGettingTrustwords() {
        binding.progressGroup.isVisible = false
        binding.description.setText(R.string.status_loading_error)
        configureButtonsJustClose()
    }

    private fun showErrorLoadingMessage() {
        binding.progressGroup.isVisible = false
        binding.description.setText(R.string.status_loading_error)
        configureButtonsJustClose()
    }

    private fun showMistrustDone() {
        binding.progressGroup.isVisible = false
        binding.description.text =
            getString(R.string.reject_trust_dialog_success, viewModel.partnerEmail)
        configureButtonsJustClose()
    }

    private fun showTrustDone() {
        binding.progressGroup.isVisible = false
        binding.description.text =
            getString(R.string.confirm_trust_dialog_success, viewModel.partnerEmail)
        configureButtonsJustClose()
    }

    private fun configureButtonsJustClose() {
        binding.dissmissActionButton.isVisible = false
        binding.negativeActionButton.isVisible = false
        binding.afirmativeActionButton.setText(R.string.close)
        binding.afirmativeActionButton.setOnClickListener { dismissAllowingStateLoss() }
    }

    private fun showMistrustProgress() {
        binding.progressGroup.isVisible = true
        binding.progressText.text =
            getString(R.string.reject_trust_dialog_progress, viewModel.partnerEmail)
    }

    private fun showTrustProgress() {
        binding.progressGroup.isVisible = true
        binding.progressText.text =
            getString(R.string.confirm_trust_dialog_progress, viewModel.partnerEmail)
    }

    private fun showMistrustConfirmation() {
        binding.toolbar.menu.clear()
        binding.fprGroup.isVisible = false
        binding.trustwordsGroup.isVisible = false
        binding.description.text = getString(
            R.string.reject_trust_dialog_confirmation,
            viewModel.partnerEmail
        ) // better use userName than email if possible??
        binding.afirmativeActionButton.setText(R.string.reject_trust_dialog_positive_action)
        binding.afirmativeActionButton.setOnClickListener {
            viewModel.positiveAction()
        }
        binding.negativeActionButton.setOnClickListener {
            viewModel.negativeAction()
        }
        binding.dissmissActionButton.setOnClickListener { dismissAllowingStateLoss() }
        binding.negativeActionButton.setText(R.string.verify_partner_dialog_go_back)
        binding.negativeActionButton.setTextColorAttr(R.attr.colorAccent)
        binding.dissmissActionButton.setTextColorAttr(R.attr.colorAccent)
    }

    private fun showTrustConfirmation() {
        binding.toolbar.menu.clear()
        binding.fprGroup.isVisible = false
        binding.trustwordsGroup.isVisible = false
        binding.description.text = getString(
            R.string.confirm_trust_dialog_confirmation,
            viewModel.partnerEmail
        ) // better use userName than email if possible??
        binding.afirmativeActionButton.setText(R.string.confirm_trust_dialog_positive_action)
        binding.afirmativeActionButton.setOnClickListener {
            viewModel.positiveAction()
        }
        binding.negativeActionButton.setText(R.string.verify_partner_dialog_go_back)
        binding.negativeActionButton.setTextColorAttr(R.attr.colorAccent)
        binding.dissmissActionButton.setTextColorAttr(R.attr.colorAccent)
        binding.negativeActionButton.setOnClickListener {
            viewModel.negativeAction()
        }
        binding.dissmissActionButton.setOnClickListener { dismissAllowingStateLoss() }
    }

    private fun renderHandshakeData(state: VerifyPartnerState.HandshakeReady) {
        binding.fprGroup.isVisible = true
        binding.trustwordsGroup.isVisible = state.trustwords.isNotBlank()
        binding.showLongTrustwords.isVisible =
            state.trustwords.isNotBlank() && viewModel.shortTrustWords
        binding.description.isVisible = true
        binding.description.setText(R.string.pep_ask_trustwords)
        binding.trustwords.text = state.trustwords
        binding.fprCurrentAccountTitle.text = viewModel.myselfEmail
        binding.fprPartnerAccountTitle.text = viewModel.partnerEmail
        binding.fprCurrentAccountValue.text = state.ownFpr
        binding.fprPartnerAccountValue.text = state.partnerFpr
        binding.progressGroup.isVisible = false
        binding.afirmativeActionButton.isVisible = true
        binding.negativeActionButton.isVisible = true
        binding.dissmissActionButton.isVisible = true
        binding.negativeActionButton.setTextColorColor(R.color.planck_red)
        binding.negativeActionButton.setText(R.string.key_import_reject)
        binding.afirmativeActionButton.setText(R.string.pep_confirm_trustwords)
        binding.dissmissActionButton.setTextColorAttr(R.attr.defaultColorOnBackground)
        setupMenu()
    }

    private fun showLoadingHandshakeData() {
        binding.toolbar.menu.clear()
        binding.progressText.setText(R.string.message_list_loading)
        binding.progressGroup.isVisible = true
    }

    private fun setupViews() {
        binding.afirmativeActionButton.setOnClickListener {
            viewModel.positiveAction()
        }
        binding.negativeActionButton.setOnClickListener {
            viewModel.negativeAction()
        }
        binding.dissmissActionButton.setOnClickListener { dismissAllowingStateLoss() }
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
            ThemeManager.getDrawableFromAttributeResource(requireContext(), R.attr.iconLanguage)
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

fun Fragment.showConfirmationDialog(
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

fun AppCompatActivity.showConfirmationDialog(
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

