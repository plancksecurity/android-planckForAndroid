package com.fsck.k9.controller;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;

import org.pEp.jniadapter.Rating;

import timber.log.Timber;

class TrustedMessageController {

    boolean shouldDownloadMessageInTrustedServer(PEpProvider.DecryptResult result, MimeMessage decryptedMessage, Account account) {
        return account.ispEpPrivacyProtected()
                && !account.isUntrustedSever()
                && result.flags == null
                && !decryptedMessage.isSet(Flag.X_PEP_NEVER_UNSECURE);
    }

    <T extends Message> boolean shouldAppendMessageInTrustedServer(T message, Account account) {
        return !account.isUntrustedSever() && !message.isSet(Flag.X_PEP_NEVER_UNSECURE);
    }

    boolean getAlreadyDecrypted(Message message, Account account, Rating rating) {
        return account.ispEpPrivacyProtected()
                && !account.isUntrustedSever()
                && !message.isSet(Flag.X_PEP_NEVER_UNSECURE)
                && !rating.equals(Rating.pEpRatingUndefined)
                && rating.value > Rating.pEpRatingUnencrypted.value;
    }

    boolean shouldAppendMessageOnUntrustedServer(Account account, Rating rating) {
        return !(!account.isUntrustedSever() && rating.equals(Rating.pEpRatingUndefined));
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

        if (account.isUntrustedSever() ||
                localMessage.getFlags().contains(Flag.X_PEP_NEVER_UNSECURE)) { //Untrusted server
            encryptedMessage = encryptUntrustedMessage(context, pEpProvider, account, localMessage);
        } else { // Trusted
            return localMessage;
        }
        return encryptedMessage;
    }

    private Message encryptUntrustedMessage(Context context, PEpProvider pEpProvider, Account account, LocalMessage localMessage) throws MessagingException {
        Message encryptedMessage;
        try {
            if (PEpUtils.ispEpDisabled(account, null)) {
                encryptedMessage = localMessage;
            } else if (account.getDraftsFolderName().equals(localMessage.getFolder().getName())) {
                encryptedMessage = pEpProvider.encryptMessageToSelf(localMessage);
            } else {
                Preferences preferences = Preferences.getPreferences(context);
                String[] keys = preferences.getMasterKeysArray(account.getUuid());
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
