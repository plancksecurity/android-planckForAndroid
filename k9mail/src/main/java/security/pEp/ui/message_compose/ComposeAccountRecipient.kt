package security.pEp.ui.message_compose

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fsck.k9.R
import com.fsck.k9.databinding.ComposeAccountItemBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ComposeAccountRecipient(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private lateinit var accountName : TextView

    override fun onFinishInflate() {
        super.onFinishInflate()
        accountName = findViewById<View>(R.id.accountName) as TextView
    }

    fun bindView(name: String) {
        accountName.text = name
    }

    fun getText(): String {
        return accountName.text.toString()
    }

}