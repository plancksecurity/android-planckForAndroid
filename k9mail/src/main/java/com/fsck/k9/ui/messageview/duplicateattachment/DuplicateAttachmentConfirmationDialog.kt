package com.fsck.k9.ui.messageview.duplicateattachment

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

private const val ID = 1
private const val DIALOG_TAG = "duplicateAttachmentConfirmationDialog"
private const val ARG_INITIAL_SCREEN_MODE = "overwriteOrRename"
private const val ARG_DEFAULT_FILE_NAME = "default_file_name"

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
                initialScreenMode = ScreenMode.valueOf(
                    it.getString(
                        ARG_INITIAL_SCREEN_MODE,
                        ScreenMode.RENAME.name
                    )
                ),
                defaultName = it.getString(
                    ARG_DEFAULT_FILE_NAME,
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
        presenter.displayInitialScreen(savedInstanceState)
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
            presenter.positiveButtonClicked()
        }
        negativelButton.setOnClickListener {
            presenter.negativeButtonClicked()
        }
        renameButton.setOnClickListener {
            presenter.renameButtonClicked()
        }
    }

    override fun finish() {
        dismissAllowingStateLoss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.saveInstanceState(outState)
    }

    override fun displayOverwriteScreen() {
        messageText.setText(R.string.dialog_confirm_duplicate_attachment_message)
        newNameInput.visibility = View.GONE
        positiveButton.setText(R.string.dialog_confirm_duplicate_attachment_overwrite_button)
        negativelButton.setText(R.string.cancel_action)
        renameButton.visibility = View.VISIBLE
    }

    override fun displayRenameScreen(
        backOrCancel: Boolean,
        defaultFileName: String
    ) {
        messageText.setText(R.string.dialog_confirm_duplicate_attachment_rename_message)
        newNameInput.setText(defaultFileName)
        newNameInput.visibility = View.VISIBLE
        positiveButton.setText(R.string.dialog_confirm_duplicate_attachment_save_button)
        negativelButton.setText(
            if(backOrCancel) R.string.dialog_confirm_duplicate_attachment_back_button
            else R.string.cancel_action
        )
        renameButton.visibility = View.GONE
    }

    private fun getListener(): DuplicationAttachmentConfirmationListener {
        try {
            return targetFragment as DuplicationAttachmentConfirmationListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                targetFragment!!.javaClass.toString() +
                        " must implement DuplicationAttachmentConfirmationListener"
            )
        }
    }

    companion object {
        private fun newInstance(
            initialScreenMode: ScreenMode,
            defaultFileName: String
        ) = DuplicateAttachmentConfirmationDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_DEFAULT_FILE_NAME, defaultFileName)
                putString(ARG_INITIAL_SCREEN_MODE, initialScreenMode.name)
            }
        }

        @JvmStatic
        fun Fragment.showDuplicateAttachmentConfirmationDialog(
            initialScreenMode: ScreenMode,
            defaultFileName: String
        ) {
            if(this !is DuplicationAttachmentConfirmationListener) {
                throw IllegalStateException("Fragment must implement DuplicationAttachmentConfirmationListener!")
            }
            val dialogFragment = newInstance(initialScreenMode, defaultFileName)

            dialogFragment.setTargetFragment(this, ID)
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