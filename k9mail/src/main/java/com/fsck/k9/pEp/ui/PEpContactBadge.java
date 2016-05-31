/*
Created by Helm  23/05/16.
*/


package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpUtils;
import org.pEp.jniadapter.Color;

public class PEpContactBadge extends QuickContactBadge {
    Color pEpColor;
    int color = 0x00000000;
    private Context context;
    Paint paint;
    Rect contactBoundsBadgeRect, pEpBadgeRect;
    Drawable currentStatus;



    public PEpContactBadge(Context context) {
        super(context);
        init(context);
    }

    public PEpContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public PEpContactBadge(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setpEpColor(Color pEpColor) {
        this.pEpColor = pEpColor;
        color = PEpUtils.getColorColor(pEpColor, context);
        paint.setColor(color);
        currentStatus = getCurrentStatus();
        invalidate();
    }

    private Drawable getCurrentStatus() {
        if (color == getResources().getColor(R.color.pep_red)){
            return ContextCompat.getDrawable(context, R.drawable.pep_status_red);
        }
        else if (color == getResources().getColor(R.color.pep_green)){
            return ContextCompat.getDrawable(context, R.drawable.pep_status_green);
        }
        else if (color == getResources().getColor(R.color.pep_yellow)){
            return ContextCompat.getDrawable(context, R.drawable.pep_status_yellow);
        }
        else {
            return ContextCompat.getDrawable(context, R.drawable.pep_status_gray);
        }
    }

    private void init(Context context) {
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setEnabled(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (super.getDrawable() != null) {
            super.getDrawable().setBounds(contactBoundsBadgeRect);
        }
        super.getDrawable().draw(canvas);
        if (paint.getAlpha() != 0 && currentStatus != null) {
            currentStatus.setBounds(pEpBadgeRect);
            currentStatus.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        float additionalPaddingPercent = .1f;
        int contactLeft = Math.round(0);
        int contactTop = Math.round(height * additionalPaddingPercent);
        int contactRight = Math.round(width * (1f - additionalPaddingPercent * 2));
        int contactBottom = Math.round(height * (1f - additionalPaddingPercent));
        int pEpBadgeLeft = Math.round(width * .60f);
        int pEpBadgeTop = Math.round(height * .60f);
        contactBoundsBadgeRect = new Rect(contactLeft, contactTop, contactRight, contactBottom);
        pEpBadgeRect = new Rect(pEpBadgeLeft, pEpBadgeTop, width, height);

    }




}
