package security.pEp.remoteConfiguration

import android.content.*
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.fsck.k9.Preferences
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory

class ConfigurationManager(private val context: Context, private val dataStoreFactory: AccountSettingsDataStoreFactory) {

    companion object {
        const val RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION = "pep_disable_privacy_protection"
        const val RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION_MANAGED = "pep_disable_privacy_protection_managed"

    }

    var listener: RestrictionsListener? = null
    private var restrictionsReceiver: RestrictionsReceiver? = null

    fun loadConfigurations() {
        val manager =
                context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictions = manager.applicationRestrictions
        val entries =
                manager.getManifestRestrictions(context.applicationContext?.packageName)
        val remoteConfig = mapRestrictions(entries, restrictions)
        saveRemoteConfig(remoteConfig)
        sendRemoteConfig()
    }

    private fun mapRestrictions(entries: List<RestrictionEntry>, restrictions: Bundle): RemoteConfig {
        val remoteConfigBuilder = RemoteConfigBuilder()
        entries.forEach { entry ->
            when (entry.key) {
                RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION ->
                    remoteConfigBuilder.pepEnablePrivacyProtection(
                            restrictions.getBoolean(RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION, true)
                    )
            }
        }
        return remoteConfigBuilder.build()
    }

    private fun saveRemoteConfig(remoteConfig: RemoteConfig) {
        val preferences = Preferences.getPreferences(context)
        val accounts = preferences.accounts

        accounts.forEach { account ->
            account.setpEpPrivacyProtection(remoteConfig.pepEnablePrivacyProtection)
            account.save(preferences)
            val dataStore = dataStoreFactory.create(account)
            dataStore.putBoolean(RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION, remoteConfig.pepEnablePrivacyProtection)
            preferences.isPepEnablePrivacyProtectionManaged = remoteConfig.isPepEnablePrivacyProtectionManaged
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