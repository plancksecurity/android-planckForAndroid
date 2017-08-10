package com.fsck.k9.pEp;

import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;

public interface AccountStatsCallback {
    void accountStatusChanged(BaseAccount account, AccountStats stats);
}
