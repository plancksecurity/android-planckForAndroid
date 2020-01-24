package com.fsck.k9.ui.crypto;


import androidx.annotation.VisibleForTesting;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.CryptoResultAnnotation;

import java.util.HashMap;


public class MessageCryptoAnnotations {
    private HashMap<Part, CryptoResultAnnotation> annotations = new HashMap<>();

    @VisibleForTesting
    MessageCryptoAnnotations() {
        // Package-private constructor
    }

    public MessageCryptoAnnotations(Part part, CryptoResultAnnotation annotation){
        annotations.put(part, annotation);
    }

    @VisibleForTesting
    void put(Part part, CryptoResultAnnotation annotation) {
        annotations.put(part, annotation);
    }

    public CryptoResultAnnotation get(Part part) {
        return annotations.get(part);
    }

    public boolean has(Part part) {
        return annotations.containsKey(part);
    }

    public boolean isEmpty() {
        return annotations.isEmpty();
    }

    public Part findKeyForAnnotationWithReplacementPart(Part part) {
        for (HashMap.Entry<Part, CryptoResultAnnotation> entry : annotations.entrySet()) {
            if (part == entry.getValue().getReplacementData()) {
                return entry.getKey();
            }
        }
        return null;
    }
}
