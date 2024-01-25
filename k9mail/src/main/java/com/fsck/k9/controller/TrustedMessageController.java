package com.fsck.k9.controller;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUtils;

import foundation.pEp.jniadapter.Rating;

import timber.log.Timber;

class TrustedMessageController {

    boolean shouldReuploadMessageInTrustedServer(PlanckProvider.DecryptResult result,
                                                 MimeMessage decryptedMessage,
                                                 Account account,
                                                 boolean alreadyDecrypted) {
        return false;
    }

    <T extends Message> boolean shouldAppendMessageInTrustedServer(T message, Account account) {
        return false;
    }

    boolean getAlreadyDecrypted(Message sourceMessage, PlanckProvider.DecryptResult decryptResult, Account account, Rating rating) {
        return false;
    }

    boolean shouldAppendMessageOnUntrustedServer(Account account, Rating rating) {
        return false;
    }

    Message getOwnMessageCopy(Context context, PlanckProvider planckProvider, Account account, LocalMessage localMessage) throws MessagingException {
        /*
        if, never insecure
            act 100% as untrusted -> This case should be applied if and if not is trusted server.
        else ->
            send encrypted copy
            store and send unencrypted copy (append to sent folder, same for draft and outbox)
        */
        Message encryptedMessage;

        if (localMessage.getFlags().contains(Flag.X_PEP_SYNC_MESSAGE_TO_SEND)) return localMessage;

        if (localMessage.getFlags().contains(Flag.X_PEP_NEVER_UNSECURE)) {
            encryptedMessage = encryptUntrustedMessage(context, planckProvider, account, localMessage);
        } else { // Trusted
            localMessage.setInternalDate(localMessage.getSentDate());
            return localMessage;
        }
        return encryptedMessage;
    }

    private Message encryptUntrustedMessage(Context context, PlanckProvider planckProvider, Account account, LocalMessage localMessage) throws MessagingException {
        Message encryptedMessage;
        try {
            //TODO: Move to pEp provider
            String[] keys = K9.getMasterKeys().toArray(new String[0]);
            if (PlanckUtils.ispEpDisabled(account, null)) {
                encryptedMessage = localMessage;
            } else if (account.getDraftsFolderName().equals(localMessage.getFolder().getName()) ||
                    account.getTrashFolderName().equals(localMessage.getFolder().getName()) ) {
                encryptedMessage = planckProvider.encryptMessageToSelf(localMessage, keys);
            } else {
                encryptedMessage = planckProvider.encryptMessage(localMessage, keys).get(0);
            }

        } catch (Exception ex) {
            Timber.e("pEp", "getOwnMessageCopy: ", ex);
            throw ex;
        }
        encryptedMessage.setUid(localMessage.getUid());
        encryptedMessage.setInternalDate(localMessage.getInternalDate());
        encryptedMessage.setFlags(localMessage.getFlags(), true);
        return encryptedMessage;
    }
}
