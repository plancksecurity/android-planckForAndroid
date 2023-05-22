package com.fsck.k9.controller;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.planck.PEpProvider;
import com.fsck.k9.planck.PEpUtils;

import foundation.pEp.jniadapter.Rating;

import timber.log.Timber;

class TrustedMessageController {

    boolean shouldReuploadMessageInTrustedServer(PEpProvider.DecryptResult result,
                                                 MimeMessage decryptedMessage,
                                                 Account account,
                                                 boolean alreadyDecrypted) {
        return account.ispEpPrivacyProtected()
                && !account.isUntrustedSever()
                && result.flags == -1
                && !decryptedMessage.isSet(Flag.X_PEP_NEVER_UNSECURE)
                && !alreadyDecrypted
                && result.rating.value >= Rating.pEpRatingUnreliable.value;
    }

    <T extends Message> boolean shouldAppendMessageInTrustedServer(T message, Account account) {
        return !account.isUntrustedSever() && !message.isSet(Flag.X_PEP_NEVER_UNSECURE);
    }

    boolean getAlreadyDecrypted(Message sourceMessage, PEpProvider.DecryptResult decryptResult, Account account, Rating rating) {
        return account.ispEpPrivacyProtected()
                && !account.isUntrustedSever()
                && !sourceMessage.isSet(Flag.X_PEP_NEVER_UNSECURE)
                && !rating.equals(Rating.pEpRatingUndefined)
                && !decryptResult.isFormerlyEncryptedReUploadedMessage;
    }

    boolean shouldAppendMessageOnUntrustedServer(Account account, Rating rating) {
        return !account.isUntrustedSever() && rating.equals(Rating.pEpRatingUndefined);
    }

    Message getOwnMessageCopy(Context context, PEpProvider pEpProvider, Account account, LocalMessage localMessage) throws MessagingException {
        /*
        if, never insecure
            act 100% as untrusted -> This case should be applied if and if not is trusted server.
        else ->
            send encrypted copy
            store and send unencrypted copy (append to sent folder, same for draft and outbox)
        */
        Message encryptedMessage;

        if (localMessage.getFlags().contains(Flag.X_PEP_SYNC_MESSAGE_TO_SEND)) return localMessage;

        if (account.isUntrustedSever() ||
                localMessage.getFlags().contains(Flag.X_PEP_NEVER_UNSECURE)) { //Untrusted server
            encryptedMessage = encryptUntrustedMessage(context, pEpProvider, account, localMessage);
        } else { // Trusted
            localMessage.setInternalDate(localMessage.getSentDate());
            return localMessage;
        }
        return encryptedMessage;
    }

    private Message encryptUntrustedMessage(Context context, PEpProvider pEpProvider, Account account, LocalMessage localMessage) throws MessagingException {
        Message encryptedMessage;
        try {
            //TODO: Move to pEp provider
            String[] keys = K9.getMasterKeys().toArray(new String[0]);
            if (PEpUtils.ispEpDisabled(account, null)) {
                encryptedMessage = localMessage;
            } else if (account.getDraftsFolderName().equals(localMessage.getFolder().getName()) ||
                    account.getTrashFolderName().equals(localMessage.getFolder().getName()) ) {
                encryptedMessage = pEpProvider.encryptMessageToSelf(localMessage, keys);
            } else {
                encryptedMessage = pEpProvider.encryptMessage(localMessage, keys).get(0);
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
