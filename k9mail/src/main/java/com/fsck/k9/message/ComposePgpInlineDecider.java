package com.fsck.k9.message;


import java.util.List;

import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class ComposePgpInlineDecider {


    private static ComposePgpInlineDecider sDecider = new ComposePgpInlineDecider();


    public static ComposePgpInlineDecider getInstance() {
        return sDecider;
    }

    public boolean shouldReplyInline(Message localMessage) {
        // TODO more criteria for this? maybe check the User-Agent header?
        return messageHasPgpInlineParts(localMessage);
    }

    private boolean messageHasPgpInlineParts(Message localMessage) {
        List<Part> inlineParts = MessageDecryptVerifier.findPgpInlineParts(localMessage);
        return !inlineParts.isEmpty();
    }

}
