package com.fsck.k9.pEp.ui.infrastructure.exceptions;

import com.fsck.k9.R;
import com.fsck.k9.mail.CertificateValidationException;

public class PEpCertificateException extends PEpSetupException {

    private final String message;
    private final CertificateValidationException originalException;

    public PEpCertificateException(CertificateValidationException exception) {
        this.message = exception.getMessage();
        this.originalException = exception;
    }

    @Override
    public Boolean isCertificateAcceptanceNeeded() {
        return true;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getTitleResource() {
        if (hasCertChain()) {
            return R.string.account_setup_failed_dlg_certificate_message_fmt;
        } else {
            return R.string.account_setup_failed_dlg_server_message_fmt;
        }
    }

    public Boolean hasCertChain() {
        return originalException.getCertChain() != null;
    }

    public CertificateValidationException getOriginalException() {
        return originalException;
    }
}
