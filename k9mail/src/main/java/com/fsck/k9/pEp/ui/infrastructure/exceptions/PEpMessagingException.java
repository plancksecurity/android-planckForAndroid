package com.fsck.k9.pEp.ui.infrastructure.exceptions;

import com.fsck.k9.R;
import com.fsck.k9.mail.MessagingException;

public class PEpMessagingException extends PEpSetupException {
    private final String message;
    private final MessagingException originalException;

    public PEpMessagingException(MessagingException exception) {
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
        return R.string.account_setup_failed_dlg_server_message_fmt;
    }

    public MessagingException getOriginalException() {
        return originalException;
    }
}
