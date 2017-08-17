package com.fsck.k9.pEp.ui.keysync.languages;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import com.fsck.k9.R;

public class PEpLanguageSelector {

    public static final String PEP_DEFAULT_LANGUAGE = "en";

    public static void showLanguageSelector(Context context, CharSequence[] pEpLocales,
                                            CharSequence[] pEpLanguages, String trustwordsLanguage,
                                            DialogInterface.OnClickListener onClickListener) {
        trustwordsLanguage = ensureTrustwordsLanguage(trustwordsLanguage);
        Integer selectedLanguageIndex = getLanguageIndex(pEpLocales, trustwordsLanguage);
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(R.string.settings_language_label))
                .setSingleChoiceItems(pEpLanguages, selectedLanguageIndex, onClickListener)
                .setPositiveButton(R.string.ok, null)
                .create().show();
    }

    @NonNull
    private static Integer getLanguageIndex(CharSequence[] pEpLanguages, String trustwordsLanguage) {
        Integer selectedLanguageIndex = 0;
        for (int i = 0; i < pEpLanguages.length; i++) {
            if (pEpLanguages[i].equals(trustwordsLanguage)) {
                selectedLanguageIndex = i;
                break;
            }
        }
        return selectedLanguageIndex;
    }

    @NonNull
    private static String ensureTrustwordsLanguage(String trustwordsLanguage) {
        if (trustwordsLanguage == null) {
            trustwordsLanguage = PEP_DEFAULT_LANGUAGE;
        }
        return trustwordsLanguage;
    }
}
