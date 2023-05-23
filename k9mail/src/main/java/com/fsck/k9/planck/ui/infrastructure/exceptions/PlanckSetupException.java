package com.fsck.k9.planck.ui.infrastructure.exceptions;

import com.fsck.k9.activity.setup.AccountSetupCheckSettings;

public abstract class PlanckSetupException {

    public AccountSetupCheckSettings.CheckDirection direction;

    public abstract Boolean isCertificateAcceptanceNeeded();

    public abstract String getMessage();

    public abstract int getTitleResource();
}
