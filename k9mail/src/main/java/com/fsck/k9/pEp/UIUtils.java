package com.fsck.k9.pEp;

import android.support.annotation.NonNull;

import java.util.Locale;

public class UIUtils {

    @NonNull
    public static CharSequence[] prettifyLanguages(CharSequence[] pEpLocales) {
        CharSequence[] pEpLanguages = new CharSequence[pEpLocales.length];
        for (Locale locale : Locale.getAvailableLocales()) {
            for (int i = 0; i < pEpLocales.length; i++) {
                if (locale.getLanguage().equals(pEpLocales[i])) {
                    String uppercasedLanguage = uppercaseFirstCharacter(locale.getDisplayLanguage());
                    pEpLanguages[i] = uppercasedLanguage;
                }
            }
        }
        return pEpLanguages;
    }

    @NonNull
    private static String uppercaseFirstCharacter(String locale) {
        return locale.substring(0, 1).toUpperCase() + locale.substring(1);
    }
}
