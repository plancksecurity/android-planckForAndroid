package security.pEp.ui.duplicateattachment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule
import com.fsck.k9.pEp.infrastructure.modules.PEpModule
import javax.inject.Inject

private const val TARGET_FRAGMENT_REQUEST_CODE = 2001
private const val DIALOG_TAG = "duplicateAttachmentConfirmationDialog"
private const val ARG_INITIAL_SCREEN_MODE = "overwriteOrRename"
private const val ARG_DEFAULT_FILE_NAME = "defaultFileName"
private const val ARG_SAVE_PATH = "savePath"

class DuplicateAttachmentConfirmationDialog : DialogFragment(),
    DuplicateAttachmentConfirmationView {
    private lateinit var messageText: TextView
    private lateinit var newNameInput: EditText
    private lateinit var positiveButton: Button
    private lateinit var renameButton: Button
    private lateinit var negativelButton: Button

    @Inject
    lateinit var presenter: DuplicateAttachmentConfirmationPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeInjector()
        arguments?.let {
            presenter.initialize(
                view = this,
                listener = getListener(),
                initialScreenMode = ScreenMode.valueOf(
                    it.getString(
                        ARG_INITIAL_SCREEN_MODE,
                        ScreenMode.RENAME.name
                    )
                ),
                defaultName = it.getString(
                    ARG_DEFAULT_FILE_NAME,
                    ""
                ),
                savePath = it.getString(
                    ARG_SAVE_PATH,
                    ""
                )
            )
        }
    }

    private fun initializeInjector() {
        val pEpComponent = DaggerPEpComponent.builder()
            .applicationComponent((requireContext().applicationContext as K9).component)
            .pEpModule(
                PEpModule(
                    requireActivity(),
                    LoaderManager.getInstance(this),
                    parentFragmentManager
                )
            )
            .activityModule(ActivityModule(activity))
            .build()
        pEpComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            R.layout.fragment_duplicate_attachment_confirmation_dialog,
            container,
            false
        )
        locateViews(rootView)
        setupClickListeners()
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.displayInitialStage(savedInstanceState)
    }

    private fun locateViews(rootView: View) {
        messageText = rootView.findViewById(R.id.messageText)
        newNameInput = rootView.findViewById(R.id.newNameInput)
        positiveButton = rootView.findViewById(R.id.acceptButton)
        renameButton = rootView.findViewById(R.id.renameButton)
        negativelButton = rootView.findViewById(R.id.cancelButton)
    }

    private fun setupClickListeners() {
        positiveButton.setOnClickListener {
            presenter.positiveActionClicked(newNameInput.text.toString())
        }
        negativelButton.setOnClickListener {
            presenter.negativeActionClicked()
        }
        renameButton.setOnClickListener {
            presenter.renameActionClicked()
        }
    }

    override fun finish() {
        dismissAllowingStateLoss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.saveInstanceState(outState)
    }

    override fun displayOverwriteStage() {
        messageText.setText(R.string.dialog_confirm_duplicate_attachment_message)
        newNameInput.visibility = View.GONE
        positiveButton.setText(R.string.dialog_confirm_duplicate_attachment_overwrite_button)
        negativelButton.setText(R.string.cancel_action)
        renameButton.visibility = View.VISIBLE
    }

    override fun displayRenameStage(
        canGoBack: Boolean,
        defaultFileName: String
    ) {
        messageText.setText(R.string.dialog_confirm_duplicate_attachment_rename_message)
        newNameInput.setText(defaultFileName)
        newNameInput.visibility = View.VISIBLE
        positiveButton.setText(R.string.dialog_confirm_duplicate_attachment_save_button)
        negativelButton.setText(
            if (canGoBack) R.string.dialog_confirm_duplicate_attachment_back_button
            else R.string.cancel_action
        )
        renameButton.visibility = View.GONE
    }

    private fun getListener(): DuplicationAttachmentConfirmationListener  =
        targetFragment as DuplicationAttachmentConfirmationListener

    companion object {
        private fun newInstance(
            initialScreenMode: ScreenMode,
            defaultFileName: String,
            savePath: String
        ) = DuplicateAttachmentConfirmationDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_DEFAULT_FILE_NAME, defaultFileName)
                putSerializable(ARG_SAVE_PATH, savePath)
                putString(ARG_INITIAL_SCREEN_MODE, initialScreenMode.name)
            }
        }

        @JvmStatic
        fun Fragment.showDuplicateAttachmentConfirmationDialog(
            initialScreenMode: ScreenMode,
            defaultFileName: String,
            savePath: String
        ) {
            if (this !is DuplicationAttachmentConfirmationListener) {
                throw IllegalStateException("Fragment must implement DuplicationAttachmentConfirmationListener!")
            }
            val dialogFragment = newInstance(initialScreenMode, defaultFileName, savePath)

            dialogFragment.setTargetFragment(this, TARGET_FRAGMENT_REQUEST_CODE)
            dialogFragment.show(parentFragmentManager, DIALOG_TAG)
        }
    }
}

enum class ScreenMode {
    OVERWRITE, RENAME
}

interface DuplicationAttachmentConfirmationListener {
    fun attachmentNameConfirmed(newName: String)
    fun overwriteAttachmentName()
}