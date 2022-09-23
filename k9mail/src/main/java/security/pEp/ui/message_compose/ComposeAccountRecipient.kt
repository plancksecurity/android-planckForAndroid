package security.pEp.ui.message_compose

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.fsck.k9.R


class ComposeAccountRecipient(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var accountName: TextView = findViewById(R.id.accountName)

    fun bindView(name: String) {
        accountName.text = name
    }

    fun getText(): String {
        return accountName.text.toString()
    }

}