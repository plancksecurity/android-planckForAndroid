package com.fsck.k9.pEp;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class UIUtils {

    public static final String UTF_8_Q = "=?utf-8?Q?";
    public static final String UTF_8_B = "=?utf-8?B?";
    public static final String UTF_ENDING = "?=";

    public static final String PEP_SHARED_PREFERENCES = "pEp";
    public static final String EMAIL_SETUP = "email_setup";
    public static final String PASS_SETUP = "pass_setup";

    @NonNull
    public static CharSequence[] prettifyLanguages(CharSequence[] pEpLocales) {
        CharSequence[] pEpLanguages = new CharSequence[pEpLocales.length];
        for (Locale locale : Locale.getAvailableLocales()) {
            for (int i = 0; i < pEpLocales.length; i++) {
                if (locale.getLanguage().equals(pEpLocales[i])) {
                    String language = "";
                    switch (locale.getLanguage()) {
                        case "ca":
                            language = "Català";
                            break;
                        case "de":
                            language = "Deutsch";
                            break;
                        case "es":
                            language = "Español";
                            break;
                        case "fr":
                            language = "Français";
                            break;
                        case "tr":
                            language = "Türkçe";
                            break;
                        case "en":
                            language = "English";
                            break;
                    }
                    pEpLanguages[i] = language;
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

    public static void saveCredentialsInPreferences(Context context, String email, String password) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PEP_SHARED_PREFERENCES, MODE_PRIVATE).edit();
        editor.putString(EMAIL_SETUP, email);
        editor.putString(PASS_SETUP, password);
        editor.apply();
    }

    public static void removeCredentialsInPreferences(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PEP_SHARED_PREFERENCES, MODE_PRIVATE).edit();
        editor.remove(EMAIL_SETUP);
        editor.remove(PASS_SETUP);
        editor.apply();
    }

    public static String getEmailInPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PEP_SHARED_PREFERENCES, MODE_PRIVATE);
        return prefs.getString(EMAIL_SETUP, null);
    }
    public static String getPasswordInPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PEP_SHARED_PREFERENCES, MODE_PRIVATE);
        return prefs.getString(PASS_SETUP, null);
    }
}
