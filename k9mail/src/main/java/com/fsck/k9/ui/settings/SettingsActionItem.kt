package com.fsck.k9.ui.settings

import android.view.View
import com.fsck.k9.R
import com.fsck.k9.databinding.TextListItemBinding
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.android.synthetic.main.text_list_item.*

internal class SettingsActionItem(val actionName: String, val action: SettingsAction) : BindableItem<TextListItemBinding>() {

    override fun getLayout(): Int = R.layout.text_list_item

    override fun bind(binding: TextListItemBinding, position: Int) {
        with(binding){
            text.text = actionName
        }
    }

    override fun initializeViewBinding(view: View): TextListItemBinding {
        return TextListItemBinding.bind(view)
    }
}