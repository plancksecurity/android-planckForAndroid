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
private const val ARG_OVERWRITE_OR_RENAME = "overwriteOrRename"
private const val ARG_DEFAULT_FILE_NAME = "default_file_name"
private const val STATE_OVERWRITE_SCREEN = "overwriteScreen"

class DuplicateAttachmentConfirmationDialog : DialogFragment() {
    private var overwriteOrRename: Boolean = false
    private var isOverwriteScreen: Boolean = false
    private var defaultFileName: String = ""
    private lateinit var messageText: TextView
    private lateinit var newNameInput: EditText
    private lateinit var positiveButton: Button
    private lateinit var renameButton: Button
    private lateinit var negativelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            overwriteOrRename = it.getBoolean(ARG_OVERWRITE_OR_RENAME, false)
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
        val showOverwriteScreen =
            savedInstanceState?.getBoolean(STATE_OVERWRITE_SCREEN, false) ?: overwriteOrRename
        displayScreen(showOverwriteScreen)
       
        return rootView
    }
    
    private fun locateViews(rootView: View) {
        messageText = rootView.findViewById(R.id.messageText)
        newNameInput = rootView.findViewById(R.id.newNameInput)
        positiveButton = rootView.findViewById(R.id.acceptButton)
        renameButton = rootView.findViewById(R.id.renameButton)
        negativelButton = rootView.findViewById(R.id.cancelButton)
    }

    private fun displayScreen(showOverwriteScreen: Boolean) {
        this.isOverwriteScreen = showOverwriteScreen
        if(showOverwriteScreen) {
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
                displayScreen(false)
            }
        } else {
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
                if(overwriteOrRename) R.string.dialog_confirm_duplicate_attachment_back_button
                else R.string.cancel_action
            )
            negativelButton.setOnClickListener {
                if(overwriteOrRename) displayScreen(true)
                else dismissAllowingStateLoss()
            }
            renameButton.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_OVERWRITE_SCREEN, isOverwriteScreen)
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
            overwrite: Boolean,
            defaultFileName: String
        ) = DuplicateAttachmentConfirmationDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_DEFAULT_FILE_NAME, defaultFileName)
                putBoolean(ARG_OVERWRITE_OR_RENAME, overwrite)
            }
        }

        @JvmStatic
        fun Fragment.showDuplicateAttachmentConfirmationDialog(
            overwrite: Boolean,
            defaultFileName: String
        ) {
            if(this !is DuplicationAttachmentConfirmationListener) {
                throw IllegalStateException("Fragment must implement DuplicationAttachmentConfirmationListener!")
            }
            val dialogFragment = newInstance(overwrite, defaultFileName)

            dialogFragment.setTargetFragment(this, ID)
            dialogFragment.show(parentFragmentManager, DIALOG_TAG)
        }
    }
}