package com.fsck.k9.ui.settings

import android.view.View
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.databinding.AccountListItemBinding
import com.xwray.groupie.viewbinding.BindableItem


internal class AccountItem(val account: Account) : BindableItem<AccountListItemBinding>() {

    override fun bind(itemBinding: AccountListItemBinding, position: Int) {
        with(itemBinding){
            name.text = account.description
            email.text = account.email
        }
    }

    override fun getLayout(): Int {
        return R.layout.account_list_item
    }

    override fun initializeViewBinding(view: View): AccountListItemBinding {
        return AccountListItemBinding.bind(view)
    }
}