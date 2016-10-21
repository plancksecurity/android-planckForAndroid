package com.fsck.k9.pEp;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.fsck.k9.R;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

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
    private int[] color;
    private static PePUIArtefactCache instance = null;
    private ArrayList<Identity> recipients;
    private Resources resources;

    public synchronized static PePUIArtefactCache getInstance(Context context) {
        if (instance == null) {
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

        TypedArray colors = resources.obtainTypedArray(R.array.pep_color);
        color = new int[colors.length()];
        for(int idx=0; idx < colors.length(); idx++)
            color[idx] = colors.getColor(idx, 0);
        colors.recycle();

    }

    private void fillIndexMapping(Resources resources) {
        String[] colornames = resources.getStringArray(R.array.pep_states);
        for(int idx=0; idx < colornames.length; idx ++) {
            colorIndexMapping.put(Rating.valueOf(colornames[idx]), idx);
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

    public Drawable getIcon() {
        return ContextCompat.getDrawable(context, R.drawable.ic_action_pep_indicator_white);
    }

    public int getColor(Rating pEpRating) {
        return PEpUtils.getRatingColor(pEpRating, context);
    }


    public ArrayList<Identity> getRecipients() {
        if (recipients == null) {
            return new ArrayList<>();
        }
        return recipients;
    }

    public void setRecipients(ArrayList<Identity> recipients) {
        this.recipients = filteredRecipients(recipients);
    }

    private ArrayList<Identity> filteredRecipients(ArrayList<Identity> recipients) {
        ArrayList<Identity> identities = new ArrayList<>();
        Collections.sort(recipients, new Comparator<Identity>() {
            @Override
            public int compare(Identity left, Identity right) {
                return left.address.compareTo(right.address);
            }
        });
        for (int i = 0; i < recipients.size(); i++) {
            Identity identity = recipients.get(i);
            if (i == 0) {
                identities.add(identity);
            } else {
                Identity previousIdentity = recipients.get(i - 1);
                if (!previousIdentity.address.equals(identity.address)) {
                    identities.add(identity);
                }
            }
        }
        return identities;
    }
}