package com.fsck.k9.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import org.koin.android.architecture.ext.viewModel

class K9SettingsActivity : K9Activity() {
    private val viewModel: SettingsViewModel by viewModel()

    private lateinit var settingsAdapter: GroupAdapter<ViewHolder>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.activity_settings)

        initializeActionBar()
        initializeSettingsList()

        populateSettingsList()
    }

    private fun initializeActionBar() {
        setUpToolbar(true)
    }

    private fun initializeSettingsList() {
        settingsAdapter = GroupAdapter()
        settingsAdapter.setOnItemClickListener { item, _ ->
            handleItemClick(item)
        }

        with(findViewById<RecyclerView>(R.id.settings_list)) {
            adapter = settingsAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@K9SettingsActivity)
        }
    }

    private fun populateSettingsList() {
        viewModel.accounts.observeNotNull(this) { accounts ->
            populateSettingsList(accounts)
        }
    }

    private fun populateSettingsList(accounts: List<Account>) {
        settingsAdapter.clear()

        val generalSection = Section().apply {
            add(SettingsActionItem(getString(R.string.general_settings_title), SettingsAction.GENERAL_SETTINGS))
        }
        settingsAdapter.add(generalSection)

        val accountSection = Section().apply {
            for (account in accounts) {
                add(AccountItem(account))
            }
            add(SettingsActionItem(getString(R.string.add_account_action), SettingsAction.ADD_ACCOUNT))
        }
        settingsAdapter.add(accountSection)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleItemClick(item: Item<*>) {
        when (item) {
            is AccountItem -> launchAccountSettings(item.account)
            is SettingsActionItem -> item.action.execute(this)
        }
    }

    private fun launchAccountSettings(account: Account) {
        AccountSettingsActivity.start(this, account.uuid)
    }


    companion object {
        @JvmStatic fun launch(activity: Activity) {
            val intent = Intent(activity, K9SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun search(query: String?) {
        //TODO("not implemented").
    }

}
