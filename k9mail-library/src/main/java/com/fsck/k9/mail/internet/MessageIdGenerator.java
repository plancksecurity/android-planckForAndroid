package com.fsck.k9.mail.internet;


import java.util.Locale;
import java.util.UUID;

import androidx.annotation.VisibleForTesting;


public class MessageIdGenerator {
    public static MessageIdGenerator getInstance() {
        return new MessageIdGenerator();
    }

    @VisibleForTesting
    MessageIdGenerator() {
    }

    public String generateMessageId() {
        String hostname = "pretty.Easy.privacy";

        String uuid = generateUuid();
        return "<" + uuid + "@" + hostname + ">";
    }

    @VisibleForTesting
    protected String generateUuid() {
        // We use upper case here to match Apple Mail Message-ID format (for privacy)
        return UUID.randomUUID().toString().toUpperCase(Locale.US);
    }
}
