package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.R;

public class PEpPermissionView extends LinearLayout {
    private TextView title;
    private TextView subtitle;
    private CheckBox checkBox;

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
        checkBox = (CheckBox) findViewById(R.id.check_permission);
    }

    public void initialize(String permission, String description, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        title.setText(permission);
        subtitle.setText(description);
        checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    public void setChecked(Boolean checked) {
        checkBox.setChecked(checked);
    }

    public void enable(Boolean enable) {
        checkBox.setEnabled(enable);
    }
}
