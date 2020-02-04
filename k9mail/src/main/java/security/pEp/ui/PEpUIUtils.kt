package security.pEp.ui

object PEpUIUtils {

    @JvmStatic
    fun accountNameSummary(word: String): String {
        return word.take(0).ifBlank { "?" }
    }

}