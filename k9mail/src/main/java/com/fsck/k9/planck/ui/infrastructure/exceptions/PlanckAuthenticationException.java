package com.fsck.k9.planck.ui.infrastructure.exceptions;

import com.fsck.k9.R;
import com.fsck.k9.mail.AuthenticationFailedException;

public class PlanckAuthenticationException extends PlanckSetupException {
    private final String message;
    private final AuthenticationFailedException originalException;

    public PlanckAuthenticationException(AuthenticationFailedException exception) {
        this.message = exception.getMessage();
        this.originalException = exception;
    }

    @Override
    public Boolean isCertificateAcceptanceNeeded() {
        return false;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getTitleResource() {
        return R.string.account_setup_failed_dlg_auth_message_fmt;
    }

    public AuthenticationFailedException getOriginalException() {
        return originalException;
    }
}
