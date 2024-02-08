package com.fsck.k9.ui.settings

import android.view.View
import com.fsck.k9.R
import com.fsck.k9.databinding.TextListItemBinding
import com.xwray.groupie.viewbinding.BindableItem

internal class SettingsActionItem(val text: String, val action: SettingsAction) :
    BindableItem<TextListItemBinding>() {

    override fun getLayout(): Int = R.layout.text_list_item
    override fun initializeViewBinding(view: View): TextListItemBinding {
        return TextListItemBinding.bind(view)
    }

    override fun bind(viewBinding: TextListItemBinding, position: Int) {
        viewBinding.text.text = text
    }
}
