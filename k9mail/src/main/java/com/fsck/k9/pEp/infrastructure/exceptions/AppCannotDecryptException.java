package com.fsck.k9.pEp.infrastructure.exceptions;

public class AppCannotDecryptException extends RuntimeException {

    public AppCannotDecryptException() {
        super();
    }

    public AppCannotDecryptException(String detailMessage) {
        super(detailMessage);
    }

    public AppCannotDecryptException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public AppCannotDecryptException(Throwable throwable) {
        super(throwable);
    }
}
