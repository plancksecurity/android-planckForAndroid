package security.pEp.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.fsck.k9.R
import java.lang.IllegalStateException

private const val ID = 1
private const val DIALOG_TAG = "duplicateAttachmentConfirmationDialog"
private const val ARG_INITIAL_SCREEN_MODE = "overwriteOrRename"
private const val ARG_DEFAULT_FILE_NAME = "default_file_name"
private const val STATE_CURRENT_SCREEN_MODE = "overwriteScreen"

class DuplicateAttachmentConfirmationDialog : DialogFragment() {
    private lateinit var initialScreenMode: ScreenMode
    private lateinit var currentScreenMode: ScreenMode
    private var defaultFileName: String = ""
    private lateinit var messageText: TextView
    private lateinit var newNameInput: EditText
    private lateinit var positiveButton: Button
    private lateinit var renameButton: Button
    private lateinit var negativelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialScreenMode = ScreenMode.valueOf(it.getString(ARG_INITIAL_SCREEN_MODE, ScreenMode.RENAME.name))
            defaultFileName = it.getString(ARG_DEFAULT_FILE_NAME, "")
        }
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
        displayScreen(
            ScreenMode.valueOf(savedInstanceState?.getString(STATE_CURRENT_SCREEN_MODE) ?: initialScreenMode.name)
        )
       
        return rootView
    }
    
    private fun locateViews(rootView: View) {
        messageText = rootView.findViewById(R.id.messageText)
        newNameInput = rootView.findViewById(R.id.newNameInput)
        positiveButton = rootView.findViewById(R.id.acceptButton)
        renameButton = rootView.findViewById(R.id.renameButton)
        negativelButton = rootView.findViewById(R.id.cancelButton)
    }

    private fun displayScreen(screenMode: ScreenMode) {
        currentScreenMode = screenMode
        when(screenMode) {
            ScreenMode.OVERWRITE -> {
                messageText.setText(
                    R.string.dialog_confirm_duplicate_attachment_message
                )
                newNameInput.visibility = View.GONE
                positiveButton.setText(R.string.dialog_confirm_duplicate_attachment_overwrite_button)
                positiveButton.setOnClickListener {
                    dismissAllowingStateLoss()
                    getListener().overwriteAttachmentName()
                }
                negativelButton.setText(R.string.cancel_action)
                negativelButton.setOnClickListener {
                    dismissAllowingStateLoss()
                }
                renameButton.visibility = View.VISIBLE
                renameButton.setOnClickListener {
                    displayScreen(ScreenMode.RENAME)
                }
            }
            ScreenMode.RENAME -> {
                messageText.setText(
                    R.string.dialog_confirm_duplicate_attachment_rename_message
                )
                newNameInput.setText(defaultFileName)
                newNameInput.visibility = View.VISIBLE
                positiveButton.setText(R.string.dialog_confirm_duplicate_attachment_save_button)
                positiveButton.setOnClickListener {
                    dismissAllowingStateLoss()
                    getListener().attachmentNameConfirmed(newNameInput.text.toString())
                }
                negativelButton.setText(
                    if(initialScreenMode == ScreenMode.OVERWRITE) R.string.dialog_confirm_duplicate_attachment_back_button
                    else R.string.cancel_action
                )
                negativelButton.setOnClickListener {
                    if(initialScreenMode == ScreenMode.OVERWRITE) displayScreen(ScreenMode.OVERWRITE)
                    else dismissAllowingStateLoss()
                }
                renameButton.visibility = View.GONE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CURRENT_SCREEN_MODE, currentScreenMode.name)
    }

    interface DuplicationAttachmentConfirmationListener {
        fun attachmentNameConfirmed(newName: String)
        fun overwriteAttachmentName()
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