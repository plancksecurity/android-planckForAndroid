package com.fsck.k9.planck.ui.keys;

import com.fsck.k9.planck.ui.blacklist.KeyListItem;

/**
 * Created by arturo on 9/03/17.
 */

public interface OnKeyClickListener {

    public void onClick(KeyListItem item, Boolean checked);
}
