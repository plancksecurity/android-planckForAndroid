package com.fsck.k9.pEp;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import com.fsck.k9.R;

import org.pEp.jniadapter.Color;

import java.util.HashMap;

/**
 * Created by dietz on 04.07.15.
 */
public class PePUIArtefactCache
{
    private static HashMap<Color,Integer> colorIndexMapping = null;
    private String[] title;
    private int[] color;
    private Drawable[] icon;

    public PePUIArtefactCache(Resources resources) {
        fillIndexMapping(resources);
        title = resources.getStringArray(R.array.pep_title);

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
        if(colorIndexMapping != null) return;               // only once...
        colorIndexMapping = new HashMap<Color,Integer>();
        String[] colornames = resources.getStringArray(R.array.pep_states);
        for(int idx=0; idx < colornames.length; idx ++) {
            colorIndexMapping.put(Color.valueOf(colornames[idx]), idx);
        }
    }

    public String getTitle(Color c) {
        return title[colorIndexMapping.get(c)];
    }

    public Drawable getIcon(Color c) {
        return icon[colorIndexMapping.get(c)];
    }

    public int getColor(Color c) {
        return color[colorIndexMapping.get(c)];
    }
}