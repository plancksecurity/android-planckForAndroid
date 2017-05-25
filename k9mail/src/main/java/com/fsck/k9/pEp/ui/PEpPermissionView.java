package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.R;

public class PEpPermissionView extends LinearLayout {
    private TextView title;
    private TextView subtitle;

    public PEpPermissionView(Context context) {
        super(context);
        init();
    }

    public PEpPermissionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PEpPermissionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        inflate(getContext(), R.layout.permission_view, this);
        title = (TextView) findViewById(R.id.permission_title);
        subtitle = (TextView) findViewById(R.id.permission_subtitle);
    }

    public void initialize(String permission, String description) {
        title.setText(permission);
        subtitle.setText(description);
    }
}
