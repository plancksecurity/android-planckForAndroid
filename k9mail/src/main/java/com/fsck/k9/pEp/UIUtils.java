package com.fsck.k9.pEp;

import android.support.annotation.NonNull;

import java.util.Locale;

public class UIUtils {

    public static final String UTF_8_Q = "=?utf-8?Q?";
    public static final String UTF_8_B = "=?utf-8?B?";
    public static final String UTF_ENDING = "?=";

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

    public static String prettifyAddressName(String addressName) {
        if (addressName != null && addressName.contains(UTF_8_Q)) {
            addressName = addressName.replace(UTF_8_Q, "").replace(UTF_ENDING, "");
        }
        if (addressName != null && addressName.contains(UTF_8_B)) {
            addressName = addressName.replace("UTF_8_B", "").replace(UTF_ENDING, "");
        }
        return addressName;
    }
}
