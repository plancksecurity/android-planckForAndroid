package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;

import com.fsck.k9.R;

import foundation.pEp.jniadapter.Rating;
import security.pEp.ui.PEpUIUtils;

public class PEpContactBadge extends QuickContactBadge {
    Rating pEpRating;
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

    public void setPepRating(Rating pEpRating, boolean ispEpEnabled) {
        if (ispEpEnabled && pEpRating != null) {
            this.pEpRating = pEpRating;
        } else {
            this.pEpRating = Rating.pEpRatingUndefined;
        }
        color = PEpUIUtils.getRatingColor(context, this.pEpRating);
        paint.setColor(color);
        currentStatus = PEpUIUtils.getDrawableForRatingBordered(context, this.pEpRating);
        invalidate();
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
            super.getDrawable().draw(canvas);
        }
        if (paint.getAlpha() != 0 && currentStatus != null
                && color != getResources().getColor(R.color.pep_no_color)) {
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

        int pepBadgeSize = Math.round(width * .35f);
        int pepBadgePadding = Math.round(width * .10f);
        int pEpBadgeLeft = width - pepBadgeSize - pepBadgePadding;
        int pEpBadgeRight = width - pepBadgePadding;
        int pEpBadgeTop = height - pepBadgeSize;

        contactBoundsBadgeRect = new Rect(contactLeft, contactTop, contactRight, contactBottom);
        pEpBadgeRect = new Rect(pEpBadgeLeft, pEpBadgeTop, pEpBadgeRight, height);
    }


}
