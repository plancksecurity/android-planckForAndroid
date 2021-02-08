package security.pEp.mdm

import android.content.*
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.Preferences
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.ConfiguredSetting

class ConfigurationManager(private val context: Context, private val dataStoreFactory: AccountSettingsDataStoreFactory) {

    companion object {
        const val RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION = "pep_disable_privacy_protection"
    }

    var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null

    fun loadConfigurations() {
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictions = manager.applicationRestrictions
        val entries = manager.getManifestRestrictions(context.applicationContext?.packageName)
        val restrictionsList = mapRestrictions(entries, restrictions)
        saveRemoteConfig(restrictionsList)
        sendRemoteConfig()
    }

    private fun mapRestrictions(
            entries: List<RestrictionEntry>,
            restrictions: Bundle,
    ): MutableList<AppConfig> {
        val restrictionsList = mutableListOf<AppConfig>()
        entries.forEach { entry ->
            when (entry.key) {
                RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION -> {
                    restrictionsList.add(
                            AppConfig(RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION,
                                    restrictions.getString(RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION)
                            ))
                }

            }
        }
        return restrictionsList
    }

    private fun saveRemoteConfig(restrictionsList: MutableList<AppConfig>) {
        restrictionsList.forEach { entry ->
            if (entry.json != null) {
                when (entry.key) {
                    RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION -> {
                        savePEpPrivacyProtection(entry.getValue())
                    }
                }
            }
        }
    }

    private fun savePEpPrivacyProtection(config: ConfiguredSetting<Boolean>?) {
        config?.let {
            val preferences = Preferences.getPreferences(context)
            val accounts = preferences.accounts

            accounts.forEach { account ->
                account.setpEpPrivacyProtection(config)
                account.save(preferences)
                val dataStore = dataStoreFactory.create(account)
                dataStore.putBoolean(RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION, config.value)
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