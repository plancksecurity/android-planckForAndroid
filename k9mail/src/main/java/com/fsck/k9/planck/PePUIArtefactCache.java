package com.fsck.k9.planck;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Cache for texts and icons.
 *
 * (I am not completely sure that this is a perfect solution, because of permanently acquired
 * ressources, but this would be very easily fixable.)
 *
 * For the semantics, the pep_states array contains a list of enum Color elements, that are
 * used UI-wise. The other arrays contain the respective items (texts, colors, icons) in the
 * same order as the colors in pep_states.
 *
 * During initialization, pep_states is used to fill a hash map, that maps each color to an
 * index into the other arrays. The hash map is later used to find the index for a specific color.
 */
public class PePUIArtefactCache
{
    private final Context context;
    private HashMap<Rating,Integer> colorIndexMapping = new HashMap<>();
    private String[] titles;
    private String[] explanations;
    private String[] suggestions;
    private static PePUIArtefactCache instance = null;
    private ArrayList<Identity> recipients;
    private Resources resources;
    private Account composingAccount;
    private Account lastUsedAccount;
    private String emailSetup;
    private String passSetup;

    public synchronized static PePUIArtefactCache getInstance(Context context) {
        if (instance == null) {
            instance = new PePUIArtefactCache(context);
        }
        else if(!instance.getCurrentLanguage().equals(K9.getK9CurrentLanguage())) {
            Resources resources = context.getResources();
            Configuration config = resources.getConfiguration();
            config.locale = new Locale(K9.getK9CurrentLanguage());
            context.getResources().updateConfiguration(config, resources.getDisplayMetrics());
            instance = new PePUIArtefactCache(context);
        }
        return instance;
    }

    private PePUIArtefactCache(Context context) {
        this.context = context;
        this.resources = context.getResources();

        fillIndexMapping(resources);

        titles = resources.getStringArray(R.array.pep_title);
        explanations = resources.getStringArray(R.array.pep_explanation);
        suggestions = resources.getStringArray(R.array.pep_suggestion);

    }

    private String getCurrentLanguage() {
        return resources.getConfiguration().locale.getLanguage();
    }

    private void fillIndexMapping(Resources resources) {
        String[] colornames = resources.getStringArray(R.array.pep_states);
        for(int idx=0; idx < colornames.length; idx ++) {
            colorIndexMapping.put( PEpUtils.stringToRating(colornames[idx]), idx);
        }
    }

    public String getTitle(Rating rating) {
        return titles[colorIndexMapping.get(rating)];
    }

    public String getExplanation(Rating rating) {
        return explanations[colorIndexMapping.get(rating)];
    }
    public String getSuggestion(Rating rating) {
        return suggestions[colorIndexMapping.get(rating)];
    }

    public ArrayList<Identity> getRecipients() {
        if (recipients == null) {
            return new ArrayList<>();
        }
        return recipients;
    }

    public void setRecipients(Account account, ArrayList<Identity> recipients) {
        this.recipients = PEpUtils.filterRecipients(account, recipients);
    }

    public void setComposingAccount(Account account) {
        this.composingAccount = account;
    }

    public Account getComposingAccount() {
        return composingAccount;
    }

    public Account getLastUsedAccount() {
        return lastUsedAccount;
    }

    public void setLastUsedAccount(Account lastUsedAccount) {
        this.lastUsedAccount = lastUsedAccount;
    }

    public void saveCredentialsInPreferences(String email, String password) {
        emailSetup = email;
        passSetup = password;
    }

    public void removeCredentialsInPreferences() {
        emailSetup = null;
        passSetup = null;
    }

    public String getEmailInPreferences() {
        return emailSetup;
    }
    public String getPasswordInPreferences() {
        return passSetup;
    }
}