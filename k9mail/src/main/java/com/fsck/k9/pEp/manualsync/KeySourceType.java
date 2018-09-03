package com.fsck.k9.pEp.manualsync;

import android.util.Log;

public enum KeySourceType {
    PGP, PEP, NONE;

    public boolean isImportType() {
        return !equals(NONE);
    }

    public boolean ispEp() {
        return equals(PEP);
    }

    public boolean isPGP() {
        return equals(PGP);
    }
}
