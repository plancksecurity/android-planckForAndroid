package security.planck.file

import android.content.Context
import com.fsck.k9.K9
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanckSystemFileLocator @Inject constructor(
    private val k9: K9,
) {
    private val homeFolder: File
        get() = k9.getDir(HOME_FOLDER, Context.MODE_PRIVATE)

    val trustwordsFolder: File
        get() = k9.getDir(TRUSTWORDS_FOLDER, Context.MODE_PRIVATE)

    val keyStoreFolder: File
        get() = k9.getDir(KEYSTORE_FOLDER, Context.MODE_PRIVATE)

    val pEpFolder: File
        get() = File(homeFolder, PEP_FOLDER)

    val keysDbFile: File
        get() = File(pEpFolder, KEYS_DB_FILE)

    companion object {
        const val HOME_FOLDER = "home"
        const val PEP_FOLDER = ".pEp"
        const val TRUSTWORDS_FOLDER = "trustwords"
        const val KEYSTORE_FOLDER = "KeyStore"
        const val KEYS_DB_FILE = "keys.db"
        const val MANAGEMENT_DB_FILE = "management.db"
        const val SYSTEM_DB_FILE = "system.db"
    }
}
