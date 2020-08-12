package com.fsck.k9.activity.accountlist

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.*
import com.fsck.k9.activity.FolderList
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.pEp.ui.listeners.IndexedFolderClickListener
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.SearchSpecification
import security.pEp.ui.resources.ResourcesProvider
import java.util.concurrent.ConcurrentHashMap

class AccountListAdapter(
    private val context: Context,
        private val accounts: List<BaseAccount>,
        private val indexedFolderClickListener: IndexedFolderClickListener,
        private val resourcesProvider: ResourcesProvider,
        private val fontSizes: FontSizes,
        private val accountStats: ConcurrentHashMap<String, AccountStats>) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
        , AccountClickListener {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.accounts_item, parent, false)
        return AccountViewHolder(
            view,
            indexedFolderClickListener,
            this,
            resourcesProvider,
            fontSizes
        )
    }

    override fun getItemCount(): Int {
        return accounts.size
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val account = accounts[position]
        when (holder) {
            is AccountViewHolder -> holder.bind(account, accountStats[account.uuid])

        }
    }

    override fun flaggedClicked(account: BaseAccount) {
        val search = SettingsActivity.createUnreadSearch(context, account)
        MessageList.actionDisplaySearch(context, search, true, false)
    }

    @SuppressLint("StringFormatInvalid")
    override fun unreadClicked(account: BaseAccount) {
        val searchTitle = context.getString(
            R.string.search_title,
            account.description,
            context.getString(R.string.flagged_modifier)
        )

        val search: LocalSearch
        if (account is SearchAccount) {
            search = account.relatedSearch.clone()
            search.name = searchTitle
        } else {
            search = LocalSearch(searchTitle)
            search.addAccountUuid(account.uuid)

            val realAccount = account as Account
            realAccount.excludeSpecialFolders(search)
            realAccount.limitToDisplayableFolders(search)
        }

        search.and(
            SearchSpecification.SearchField.FLAGGED,
            "1",
            SearchSpecification.Attribute.EQUALS
        )
    }

    override fun foldersClicked(account: BaseAccount) {
        FolderList.actionHandleAccount(context, account as Account)
    }

    override fun settingsClicked(account: BaseAccount) {
        // onEditAccount(account as Account)
    }

    fun getItem(position: Int): BaseAccount {
        return accounts[position]
    }

}