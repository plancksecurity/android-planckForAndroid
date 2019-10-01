package com.fsck.k9.ui.settings.general

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import androidx.preference.PreferenceScreen
import android.view.MenuItem
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.fragmentTransactionWithBackStack

class GeneralSettingsActivity : K9Activity(), OnPreferenceStartScreenCallback {
    override fun search(query: String?) {
//
   }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.general_settings)

        initializeActionBar()

        if (savedInstanceState == null) {
            fragmentTransaction {
                add(R.id.generalSettingsContainer, GeneralSettingsFragment.create())
            }
        }
    }

    private fun initializeActionBar() {
        setUpToolbar(true)
        val actionBar = supportActionBar ?: throw RuntimeException("getSupportActionBar() == null")
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPreferenceStartScreen(
            caller: PreferenceFragmentCompat, preferenceScreen: PreferenceScreen
    ): Boolean {
        fragmentTransactionWithBackStack {
            replace(R.id.generalSettingsContainer, GeneralSettingsFragment.create(preferenceScreen.key))
        }

        return true
    }


    companion object {
        fun start(context: Context) {
            val intent = Intent(context, GeneralSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
