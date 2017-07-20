package com.fsck.k9.pEp.ui.infrastructure.exceptions;

public abstract class PEpSetupException {

    public abstract Boolean isCertificateAcceptanceNeeded();

    public abstract String getMessage();

    public abstract int getTitleResource();
}
