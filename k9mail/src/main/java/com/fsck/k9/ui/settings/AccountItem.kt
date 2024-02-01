package com.fsck.k9.ui.settings

import android.view.View
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.databinding.AccountListItemBinding
import com.xwray.groupie.viewbinding.BindableItem

internal class AccountItem(val account: Account) : BindableItem<AccountListItemBinding>() {

    override fun getLayout(): Int = R.layout.account_list_item
    override fun initializeViewBinding(view: View): AccountListItemBinding {
        return AccountListItemBinding.bind(view)
    }

    override fun bind(viewBinding: AccountListItemBinding, position: Int) {
        viewBinding.name.text = account.description
        viewBinding.email.text = account.email
    }
}
