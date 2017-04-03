package com.fsck.k9.pEp.ui.keysync.languages;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import com.fsck.k9.R;
import com.fsck.k9.pEp.UIUtils;

public class PEpLanguageSelector {

    public static final String PEP_DEFAULT_LANGUAGE = "en";

    public static void showLanguageSelector(Context context, CharSequence[] pEpLanguages,
                                            String trustwordsLanguage,
                                            DialogInterface.OnClickListener onClickListener) {
        trustwordsLanguage = ensureTrustwordsLanguage(trustwordsLanguage);
        CharSequence[] displayLanguages = UIUtils.prettifyLanguages(pEpLanguages);
        Integer selectedLanguageIndex = getLanguageIndex(pEpLanguages, trustwordsLanguage);
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(R.string.settings_language_label))
                .setSingleChoiceItems(displayLanguages, selectedLanguageIndex, onClickListener)
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
