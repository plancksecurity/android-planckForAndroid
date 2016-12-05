package com.fsck.k9.message.extractors;

import com.fsck.k9.mail.Message;

public class EncryptionVerifier {

    public static Boolean isEncrypted(Message message) {
        TextPartFinder textPartFinder = new TextPartFinder();
        EncryptionDetector encryptionDetector = new EncryptionDetector(textPartFinder);
        return encryptionDetector.isEncrypted(message);
    }
}
