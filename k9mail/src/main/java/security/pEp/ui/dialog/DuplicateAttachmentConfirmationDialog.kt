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
private const val ARG_MESSAGE = "message"
private const val ARG_DEFAULT_FILE_NAME = "default_file_name"

class DuplicateAttachmentConfirmationDialog : DialogFragment() {
    private var message: String = ""
    private var defaultFileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString(ARG_MESSAGE, "")
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
        rootView.findViewById<TextView>(R.id.messageText).text = message
        val nameInput = rootView.findViewById<EditText>(R.id.newNameInput)
        nameInput.setText(defaultFileName)
        rootView.findViewById<Button>(R.id.acceptButton).setOnClickListener {
            dismissAllowingStateLoss()
            getListener().attachmentNameConfirmed(nameInput.text.toString())
        }
        rootView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dismissAllowingStateLoss()
        }
        return rootView
    }

    interface DuplicationAttachmentConfirmationListener {
        fun attachmentNameConfirmed(newName: String)
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
            message: String,
            defaultFileName: String
        ) = DuplicateAttachmentConfirmationDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_MESSAGE, message)
                putString(ARG_DEFAULT_FILE_NAME, defaultFileName)
            }
        }

        @JvmStatic
        fun Fragment.showDuplicateAttachmentConfirmationDialog(
            message: String,
            defaultFileName: String
        ) {
            if(this !is DuplicationAttachmentConfirmationListener) {
                throw IllegalStateException("Fragment must implement DuplicationAttachmentConfirmationListener!")
            }
            val dialogFragment = newInstance(message, defaultFileName)

            dialogFragment.setTargetFragment(this, ID)
            dialogFragment.show(parentFragmentManager, DIALOG_TAG)
        }
    }
}