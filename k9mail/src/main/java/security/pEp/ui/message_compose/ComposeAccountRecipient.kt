package security.pEp.ui.message_compose

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.compose_account_item.view.*


class ComposeAccountRecipient(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    fun bindView(name: String) {
        accountName.text = name
    }

    fun getText(): String {
        return accountName.text.toString()
    }

}