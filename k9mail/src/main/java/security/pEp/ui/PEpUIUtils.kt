package security.pEp.ui

object PEpUIUtils {

    @JvmStatic
    fun firstLetterOf(word: String): String {
        return word.take(0).ifBlank { "?" }
    }

}