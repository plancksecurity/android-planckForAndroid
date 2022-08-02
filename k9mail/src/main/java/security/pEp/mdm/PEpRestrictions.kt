package security.pEp.mdm

import android.content.RestrictionEntry
import android.content.RestrictionsManager
import android.os.Bundle
import javax.inject.Inject

class PEpRestrictions @Inject constructor(
    private val restrictionsManager: RestrictionsManager,
    private val packageName: String,
): RestrictionsProvider {
    override val applicationRestrictions: Bundle
        get() = restrictionsManager.applicationRestrictions
    override val manifestRestrictions: List<RestrictionEntry>
        get() = restrictionsManager.getManifestRestrictions(packageName)
}
