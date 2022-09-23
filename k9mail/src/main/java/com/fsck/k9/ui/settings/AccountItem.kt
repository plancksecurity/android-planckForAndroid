package com.fsck.k9.ui.settings

import android.widget.TextView
import com.fsck.k9.Account
import com.fsck.k9.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

internal class AccountItem(val account: Account) : Item() {

    override fun getLayout(): Int = R.layout.account_list_item

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.name).text = account.description
        viewHolder.itemView.findViewById<TextView>(R.id.email).text = account.email
    }
}
