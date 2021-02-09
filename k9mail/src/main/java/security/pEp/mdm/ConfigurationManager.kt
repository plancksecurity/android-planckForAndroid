package security.pEp.mdm

import android.content.*
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory

class ConfigurationManager(
        private val context: Context,
        private val preferences: Preferences) {

    companion object {
        const val RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION = "pep_disable_privacy_protection"
    }

    var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null
    val accounts: MutableList<Account> = preferences.accounts

    fun loadConfigurations() {
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictions = manager.applicationRestrictions
        val entries = manager.getManifestRestrictions(context.applicationContext?.packageName)
        mapRestrictions(entries, restrictions)
        sendRemoteConfig()
    }

    private fun mapRestrictions(entries: List<RestrictionEntry>, restrictions: Bundle) {
        entries.forEach { entry ->
            when (entry.key) {
                RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION -> savePrivacyProtection(restrictions, entry)
            }
        }
    }

    private fun savePrivacyProtection(restrictions: Bundle, entry: RestrictionEntry) {
        val value = restrictions.getString(entry.key)
        value?.let {
            val config = AppConfig(entry.key, value).getValue<Boolean>().toManageableSetting()
            accounts.forEach { account ->
                account.setpEpPrivacyProtection(config)
                account.save(preferences)
            }
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
}