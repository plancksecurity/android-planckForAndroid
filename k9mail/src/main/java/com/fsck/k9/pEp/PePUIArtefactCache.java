package com.fsck.k9.pEp;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import com.fsck.k9.R;

import org.pEp.jniadapter.Color;

import java.util.HashMap;

/**
 * Cache for texts and icons.
 *
 * (I am not completely sure that this is a perfect solution, because of permanently acquired
 * ressources, but this would be very easily fixable.
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
    private HashMap<Color,Integer> colorIndexMapping = new HashMap<Color,Integer>();
    private String[] title;
    private String[] description;
    private int[] color;
    private Drawable[] icon;
    private static PePUIArtefactCache instance = null;

    public synchronized static PePUIArtefactCache getInstance(Resources resources) {
        if (instance == null) {
            instance = new PePUIArtefactCache(resources);
        }
        return instance;
    }

    private PePUIArtefactCache(Resources resources) {
        fillIndexMapping(resources);

        title = resources.getStringArray(R.array.pep_title);
        description = resources.getStringArray(R.array.pep_description);

        TypedArray colors = resources.obtainTypedArray(R.array.pep_color);
        color = new int[colors.length()];
        for(int idx=0; idx < colors.length(); idx++)
            color[idx] = colors.getColor(idx, 0);
        colors.recycle();

        TypedArray icons = resources.obtainTypedArray(R.array.pep_icon);
        icon = new Drawable[icons.length()];
        for(int idx=0; idx < icons.length(); idx++)
            icon[idx] = icons.getDrawable(idx);
        icons.recycle();
    };

    private void fillIndexMapping(Resources resources) {
        String[] colornames = resources.getStringArray(R.array.pep_states);
        for(int idx=0; idx < colornames.length; idx ++) {
            colorIndexMapping.put(Color.valueOf(colornames[idx]), idx);
        }
    }

    public String getTitle(Color c) {
        return title[colorIndexMapping.get(c)];
    }

    public String getDescription(Color c) {
        return description[colorIndexMapping.get(c)];
    }

    public Drawable getIcon(Color c) {
        return icon[colorIndexMapping.get(c)];
    }

    public int getColor(Color c) {
        return color[colorIndexMapping.get(c)];
    }
}