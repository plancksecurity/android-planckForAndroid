package security.planck.file

import android.content.Context
import com.fsck.k9.K9
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val HOME_FOLDER = "home"
private const val PEP_FOLDER = ".pEp"
private const val TRUSTWORDS_FOLDER = "trustwords"
private const val KEYSTORE_FOLDER = "KeyStore"
private const val KEYS_DB_FILE = "keys.db"

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
}
