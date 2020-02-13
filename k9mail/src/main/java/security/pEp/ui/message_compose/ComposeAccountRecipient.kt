package security.pEp.ui.message_compose

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.fsck.k9.helper.ContactPicture
import com.fsck.k9.mail.Address
import kotlinx.android.synthetic.main.compose_account_item.view.*


class ComposeAccountRecipient(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    fun bindView(name: String) {
        ContactPicture.getContactPictureLoader(context).loadContactPicture(Address(name), accountImage)
        accountName.text = name
    }

    fun getText(): String {
        return accountName.text.toString()
    }

}