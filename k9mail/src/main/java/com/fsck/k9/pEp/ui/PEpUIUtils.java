package com.fsck.k9.pEp.ui;

public class PEpUIUtils {

    public static String firstLetterOf(String word) {
        if (word != null && !word.isEmpty()) {
            return word.substring(0,1);
        }
        return "?";
    }
}
