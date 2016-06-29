package com.fsck.k9.pEp;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import com.fsck.k9.R;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

import java.util.ArrayList;
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
    private HashMap<Color,Integer> colorIndexMapping = new HashMap<>();
    private String[] titles;
    private String[] explanations;
    private String[] suggestions;
    private int[] color;
    private Drawable icon;
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
            colorIndexMapping.put(Color.valueOf(colornames[idx]), idx);
        }
    }

    public String getTitle(Color c) {
        return titles[colorIndexMapping.get(c)];
    }

    public String getExplanation(Color c) {
        return explanations[colorIndexMapping.get(c)];
    }
    public String getSuggestion(Color c) {
        return suggestions[colorIndexMapping.get(c)];
    }

    public Drawable getIcon(Color pEpColor) {
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_action_pep_indicator);
//        icon.setColorFilter(getColor(pEpColor), PorterDuff.Mode.MULTIPLY);
        return icon;
    }

    public int getColor(Color pepColor) {
        return PEpUtils.getColorColor(pepColor, context);
    }


    public ArrayList<Identity> getRecipients() {
        if (recipients == null) {
            return new ArrayList<>();
        }
        return recipients;
    }

    public void setRecipients(ArrayList<Identity> recipients) {
        this.recipients = recipients;
    }
}