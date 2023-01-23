package security.pEp.ui.message_compose

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.fsck.k9.databinding.ComposeAccountItemBinding


class ComposeAccountRecipient(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var binding = ComposeAccountItemBinding.inflate(LayoutInflater.from(context), this, true)
    fun bindView(name: String) {
        binding.accountName.text = name
    }

    fun getText(): String {
        return binding.accountName.text.toString()
    }

}