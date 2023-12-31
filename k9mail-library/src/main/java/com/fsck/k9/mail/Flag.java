
package com.fsck.k9.mail;

/**
 * Flags that can be applied to Messages.
 */
public enum Flag {
    DELETED,
    SEEN,
    ANSWERED,
    FLAGGED,
    DRAFT,
    RECENT,
    FORWARDED,

    /*
     * The following flags are for internal library use only.
     */
    /**
     * Delete and remove from the LocalStore immediately.
     */
    X_DESTROYED,

    /**
     * Sending of an unsent message failed. It will be retried. Used to show status.
     */
    X_SEND_FAILED,

    /**
     * Sending of an unsent message is in progress.
     */
    X_SEND_IN_PROGRESS,

    /**
     * Indicates that a message is fully downloaded from the server and can be viewed normally.
     * This does not include attachments, which are never downloaded fully.
     */
    X_DOWNLOADED_FULL,

    /**
     * Indicates that a message is partially downloaded from the server and can be viewed but
     * more content is available on the server.
     * This does not include attachments, which are never downloaded fully.
     */
    X_DOWNLOADED_PARTIAL,

    /**
     * Indicates that the copy of a message to the Sent folder has started.
     */
    X_REMOTE_COPY_STARTED,

    /**
     * Messages with this flag have been migrated from database version 50 or earlier.
     * This earlier database format did not preserve the original mime structure of a
     * mail, which means messages migrated to the newer database structure may be
     * incomplete or broken.
     * TODO Messages with this flag should be redownloaded, if possible.
     */
    X_MIGRATED_FROM_V50,

    /**
     * This flag is used for drafts where the message should be sent as PGP/INLINE.
     */
    X_DRAFT_OPENPGP_INLINE,

    /**
     * This flag is used for force unencrypted.
     */
    X_PEP_DISABLED,

    /**
     * This flag is used for mark messages send through sendMessageCallback
     */
    X_PEP_SYNC_MESSAGE_TO_SEND,

    /**
     * This flag is used for mark messages that always will be sent encrypted
     */
    X_PEP_NEVER_UNSECURE,

    /**
     * This flag is used to know if message should be encrypted
     */
    X_PEP_SHOWN_ENCRYPTED,

    /**
     * This flag is used to know if message was encrypted by the engine
     */
    X_PEP_WASNT_ENCRYPTED,

    /**
     * This flag is used to know if message is S/Mime
     */
    X_SMIME_SIGNED,
}
