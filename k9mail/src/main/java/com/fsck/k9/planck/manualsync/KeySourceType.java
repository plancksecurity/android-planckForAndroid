package com.fsck.k9.planck.manualsync;

public enum KeySourceType {
    PGP, PLANCK, NONE;

    public boolean isImportType() {
        return !equals(NONE);
    }

    public boolean ispEp() {
        return equals(PLANCK);
    }

    public boolean isPGP() {
        return equals(PGP);
    }
}
