package security.planck.mdm

import android.content.RestrictionEntry
import android.os.Bundle

interface RestrictionsProvider {
    val applicationRestrictions: Bundle
    val manifestRestrictions: List<RestrictionEntry>
}