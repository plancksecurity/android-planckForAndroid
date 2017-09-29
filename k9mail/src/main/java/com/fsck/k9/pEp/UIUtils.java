package com.fsck.k9.pEp;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class UIUtils {

    private static final String PEP_SHARED_PREFERENCES = "pEp";
    private static final String EMAIL_SETUP = "email_setup";
    private static final String PASS_SETUP = "pass_setup";

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
