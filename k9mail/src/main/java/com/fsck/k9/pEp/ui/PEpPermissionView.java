package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fsck.k9.R;

public class PEpPermissionView extends RelativeLayout {
    public PEpPermissionView(Context context) {
        super(context);
    }

    public PEpPermissionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PEpPermissionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PEpPermissionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void initialize(String permission, String description, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        TextView title = (TextView) findViewById(R.id.permission_title);
        TextView subtitle = (TextView) findViewById(R.id.permission_subtitle);
        CheckBox checkBox = (CheckBox) findViewById(R.id.check_permission);
        title.setText(permission);
        subtitle.setText(description);
        checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    public void setChecked(Boolean checked) {
        CheckBox checkBox = (CheckBox) findViewById(R.id.check_permission);
        checkBox.setChecked(checked);
    }
}
