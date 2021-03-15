package security.pEp.mdm

import android.content.*
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences

class ConfigurationManager(
        private val context: Context,
        private val preferences: Preferences) {

    companion object {
        const val RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION = "pep_disable_privacy_protection"
        const val RESTRICTION_PEP_EXTRA_KEYS = "pep_extra_keys"
    }

    private var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null
    private lateinit var accounts: MutableList<Account>

    fun loadConfigurations() {
        accounts = preferences.accounts
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictions = manager.applicationRestrictions
        val entries = manager.getManifestRestrictions(context.applicationContext?.packageName)
        mapRestrictions(entries, restrictions)
        saveAccounts()
        sendRemoteConfig()
    }

    private fun mapRestrictions(entries: List<RestrictionEntry>, restrictions: Bundle) {
        entries.forEach { entry ->
            when (entry.key) {
                RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION -> savePrivacyProtection(restrictions, entry)
                RESTRICTION_PEP_EXTRA_KEYS -> saveExtrasKeys(restrictions, entry)
            }
        }
    }

    private fun saveExtrasKeys(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<List<ExtraKey>>()?.toManageableSetting()
            config?.let {newExtraKeys ->
                val currentKeys = K9.getMasterKeys()
                newExtraKeys.value.forEach { extraKey ->
                    currentKeys.add(extraKey.fpr)
                }
                K9.setMasterKeys(currentKeys)
            }
        }
    }

    private fun savePrivacyProtection(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfigEntry(entry.key, value).getValue<Boolean>()?.toManageableSetting()
            config?.let {
                accounts.forEach { account ->
                    account.setpEpPrivacyProtection(config)
                }
            }

        }
    }

    private fun saveAccounts() {
        accounts.forEach { account ->
            account.save(preferences)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun sendRemoteConfig() {
        listener?.updatedRestrictions()
    }

    fun unregisterReceiver() {
        if (restrictionsReceiver != null) {
            context.applicationContext.unregisterReceiver(restrictionsReceiver)
            restrictionsReceiver = null
        }
    }

    fun registerReceiver() {
        if (restrictionsReceiver == null) {
            restrictionsReceiver = RestrictionsReceiver(this)
        }
        context.applicationContext.registerReceiver(
                restrictionsReceiver, IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        )
        loadConfigurations()
    }

    fun setListener(listener: RestrictionsListener) {
        this.listener = listener
    }
}