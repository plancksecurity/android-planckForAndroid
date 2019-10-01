package com.fsck.k9.pEp.manualsync;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.pEp.MimeMessageBuilder;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.keysync.PEpAddDevice;

import foundation.pEp.jniadapter.DecryptFlags;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Pair;
import foundation.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.inject.Named;

import timber.log.Timber;

enum Role {
    IMPORTER,
    EXPORTER
}

enum KeyImportMessageType {
    BEACON_MESSAGE,
    HANDSHAKE_REQUEST_MESSAGE,
    PRIVATE_KEY_MESSAGE, NO_IMPORT
}

public class ImportKeyController {
    boolean enableDebugLogging = BuildConfig.DEBUG || K9.isDebug();

    private final K9 context;
    private String senderKey = "";
    private PEpProvider pEp;
    private ImportKeyWizardState state;
    private Account account;
    private Role role;
    private KeyImportMessagingActions messagingActions;
    private List <Message> alreadyProcessedMsgs;

    ImportKeyController(Application context, @Named("Background") PEpProvider pEp) {
        this.state = ImportKeyWizardState.INIT;
        this.context = ((K9) context);
        this.pEp = pEp;
        this.role = Role.EXPORTER;
        alreadyProcessedMsgs = new ArrayList<>();
    }

    public void cancel() {
        finish();
        messagingActions = null;
    }

    public ImportKeyWizardState next() {
        if (state.equals(ImportKeyWizardState.INIT) && role.equals(Role.EXPORTER)) {
            state = state.start();
        } else {
            state = state.next();
        }

        if (!state.equals(ImportKeyWizardState.INIT)) {
            context.enableFastPolling();
        }

        Timber.e("currentSTATE %s", state.name());

        return state;
    }

    public void setAccount(Account account) {
        this.account = account;
    }


    public void setStarter(boolean starter) {
        role = starter ? Role.IMPORTER : Role.EXPORTER;
    }

    public ImportKeyWizardState getState() {
        return state;
    }

    public void finish() {
        state = ImportKeyWizardState.INIT;
        context.disableFastPolling();
        senderKey = "";
    }

    @NonNull
    private <MSG extends Message> KeySourceType getKeySourceType(@NonNull MSG srcMsg,
                                                                 @NonNull MSG decryptedMsg,
                                                                 @NonNull PEpProvider.DecryptResult result) {

        if (account == null) throw new IllegalStateException("Account is not set");
        if (PEpUtils.isMessageOnOutgoingFolder(srcMsg, account)) {
            return KeySourceType.NONE;
        }

        if (ispEpKeyImportMsg(srcMsg, decryptedMsg)) {
            if (K9.isDebug())
            Timber.i( "MessageType: pEp key import message");
            return KeySourceType.PEP;
        }

        if (isExpectedPGPKeyImportMsg(srcMsg, result)) {
            Timber.i("MessageType: maybe PGP key import message");
            return KeySourceType.PGP;
        }
        return KeySourceType.NONE;
    }

    private <MSG extends Message> boolean ispEpKeyImportMsg(MSG srcMsg, MSG decryptedMsg) {
        return PEpUtils.hasKeyImportHeader(srcMsg, decryptedMsg);
    }

    private <MSG extends Message> boolean isExpectedPGPKeyImportMsg(MSG srcMsg, PEpProvider.DecryptResult result) {
        return isOngoingKeyImport()
                && srcMsg.getFrom()[0].getAddress().equals(account.getEmail())
                && isPGPImportEncryptedMessage(result);
    }

    private boolean isPGPImportEncryptedMessage(PEpProvider.DecryptResult result) {
        //TODO check myself && yellow OR myself && green && contains key
        boolean isHandshakeMessage = result.rating.value == Rating.pEpRatingReliable.value;
        boolean isKeyMessage = result.rating.value > Rating.pEpRatingReliable.value && containsPrivateOwnKey(result);
        return isHandshakeMessage || isKeyMessage;
    }


    private boolean isOngoingKeyImport() {
        return context.isShowingKeyimportDialog() && !state.equals(ImportKeyWizardState.INIT);
    }

    public void start(boolean ispEp, final ImportWizardPresenter.Callback callback) {
        new Thread(() -> {
            callback.onStart();
            finish();
            try {
                Message keyImportRequest = PEpUtils.generateKeyImportRequest(context, pEp, account,
                        ispEp, false);
                messagingActions.sendMessage(account, keyImportRequest);
                next();
                setStarter(true);
                callback.onFinish(true);
            } catch (NullPointerException | MessagingException e) {
                callback.onFinish(false);
                if (enableDebugLogging) {
                    new Handler(Looper.getMainLooper()).post(()
                            -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show());
                }

            }
        }).start();
    }

    public void setMessagingActions(KeyImportMessagingActions messagingActions) {
        this.messagingActions = messagingActions;
    }

    public boolean isImporter() {
        return role.equals(Role.IMPORTER);
    }

    public void setState(ImportKeyWizardState state) {
        this.state = state;
    }

    interface KeyImporter {
        void processPrivateKeyMessage() throws MessagingException;

        Intent getHandshakeIntent(Identity myself, Identity sender, PEpProvider.DecryptResult result);
    }

    public <MSG extends Message> void processKeyImportMessage(MSG srcMsg,
                                                              PEpProvider.DecryptResult result,
                                                              String folder,
                                                              LocalFolder localFolder) throws MessagingException {


        MimeMessage decryptedMsg = result.msg;
        Rating rating = result.rating;
        senderKey = extractSenderKey(srcMsg, decryptedMsg, rating);
        Identity myself = pEp.myself(PEpUtils.createIdentity(srcMsg.getFrom()[0], context));
        Identity sender = createSenderIdentity(account, senderKey);


        KeyImportMessageType messageType = null;
        KeySourceType type = getKeySourceType(srcMsg, decryptedMsg, result);

        if ((type == KeySourceType.PEP && isOwnMessage(decryptedMsg, rating)) || type == KeySourceType.NONE) {
            return;
        }

        if (isAlreadyProcessed(srcMsg)) {
            //messagingActions.deleteMessage(account, srcMsg, folder, localFolder);
            return;
        }


        class PGPKeyImporter implements KeyImporter {

            @Override
            public void processPrivateKeyMessage() {
                messagingActions.showImportKeyDialogIfNeeded(srcMsg, result, account);
                ImportWizardFrompEp.notifyPrivatePGPKeyProcessed(context);
            }

            @Override
            public Intent getHandshakeIntent(Identity myself, Identity sender, PEpProvider.DecryptResult result) {
                return PEpAddDevice.getActionRequestHandshake(context, myself, sender,
                        PEpUtils.getKeyListWithoutDuplicates(result.msg.getHeader(MimeHeader.HEADER_PEP_KEY_LIST)),
                        context.getString(R.string.key_import_wizard_handshake_explanation_pgp));
            }
        }

        class PEpKeyImporter implements KeyImporter {

            @Override
            public void processPrivateKeyMessage() throws MessagingException {
//Received private key -
                ((K9) context.getApplicationContext()).disableFastPolling();
                if (role.equals(Role.IMPORTER)) { // is key to import.

                    if (enableDebugLogging) {
                        new Handler(Looper.getMainLooper()).post(()
                                -> Toast.makeText(context, "Trying to call setOWN: ::" + sender.fpr + "::", Toast.LENGTH_SHORT).show());
                    }
                    sendOwnKey(new ImportWizardPresenter.Callback() {
                        @Override
                        public void onStart() {
                            Timber.i("Init send own key from importer POV");
                        }

                        @Override
                        public void onFinish(boolean successful) {
                            Timber.e("Set own id: %s equals %s",
                                    sender.fpr, senderKey.equals(sender.fpr));
                            pEp.setOwnIdentity(sender, sender.fpr);
                            ImportWizardFrompEp.notifyPrivateKeyImported(context);
                            alreadyProcessedMsgs.clear();
                        }
                    });

                }
                //Else the key only has to be imported, already done by the engine.
            }

            @Override
            public Intent getHandshakeIntent(Identity myself, Identity sender, PEpProvider.DecryptResult result) {
                //pEp.resetTrust(sender);
                return PEpAddDevice.getActionRequestHandshake(context,
                        pEp.trustwords(myself, sender, "en", true),
                        myself, sender,
                        context.getString(R.string.key_import_wizard_handshake_explanation), true);
            }
        }

        messagingActions.deleteMessage(account, srcMsg, folder, localFolder);
        alreadyProcessedMsgs.add(srcMsg);

        KeyImporter keyImporter = null;
        switch (type) {
            case PGP:
                keyImporter = new PGPKeyImporter();
                break;
            case PEP:
                keyImporter = new PEpKeyImporter();
                break;
        }

        messageType = getMessageType(srcMsg, result);
        if (!messageType.equals(KeyImportMessageType.NO_IMPORT)) {
            next();
        }

        assert keyImporter != null;

        switch (messageType) {
            case BEACON_MESSAGE:
                //This state will only be reached if you are the exporter, as the importer
                //will ignore the beacon due to being the sender
                Timber.i("Detected Key import request");

                MimeMessage handshakeMessage = buildHandshakeRequestMessage();
                Timber.i("Handshake request generated");
                messagingActions.sendMessage(account, handshakeMessage);
                showInitialExportDialog(myself, sender);
                pEp.resetTrust(sender);
                break;
            case HANDSHAKE_REQUEST_MESSAGE:
                //Only importer will get the handshake request as the exporter generated it
                Intent handshakeIntent = keyImporter.getHandshakeIntent(myself, sender, result);

                if (type.isImportType() && context.isShowingKeyimportDialog()) { //if waiting for it
                    ImportWizardFrompEp.actionStartImportpEpKey(context, account.getUuid(), true,
                            type, handshakeIntent);
                } else {
                    Timber.e("NO CASE - INVALID STATE - Deleting message: " + state.toString());
                    finish();
                }

                Timber.i("ManualImport %s", "Detected yellow message aka handshake request");

                break;
            case PRIVATE_KEY_MESSAGE:
                Timber.i("ManualImport %s", "Key received");
                if (enableDebugLogging) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "DETECTED PRIV KEY: " + result.flags, Toast.LENGTH_SHORT).show());
                }

                keyImporter.processPrivateKeyMessage();
                finish();
                break;
            case NO_IMPORT:
                break;
        }
    }

    private <MSG extends Message> boolean isAlreadyProcessed(MSG srcMsg) {
        for (Message alreadyProcessedMsg : alreadyProcessedMsgs) {
            if (alreadyProcessedMsg.getUid().equals(srcMsg.getUid())) {
                return true;
            }
        }
        return false;
    }

    private void showInitialExportDialog(Identity myself, Identity sender) {
        Intent handshakeIntent = PEpAddDevice.getActionRequestHandshake(context,
                pEp.trustwords(myself, sender, "en", true),
                myself, sender, context.getString(R.string.key_import_wizard_handshake_explanation),
                true);
        ImportWizardFrompEp.actionStartImportpEpKey(context, account.getUuid(), false,
                KeySourceType.PEP, handshakeIntent);
    }

    @NonNull
    private MimeMessage buildHandshakeRequestMessage() throws MessagingException {
        MimeMessage handshakeUnencryptedMessage = createMimeMessage(account);
        MimeMessage handshakeMessage = pEp.encryptMessage(handshakeUnencryptedMessage,
                new String[]{senderKey}).get(PEpProvider.ENCRYPTED_MESSAGE_POSITION);
        handshakeMessage.addHeader(MimeHeader.HEADER_PEP_AUTOCONSUME_LEGACY, "yes");
        return handshakeMessage;
    }


    private <MSG extends Message> KeyImportMessageType getMessageType(MSG srcMsg, PEpProvider.DecryptResult result) {
        Rating rating = result.rating;
        if (rating.value <= Rating.pEpRatingUnencrypted.value && state.equals(ImportKeyWizardState.INIT)) {
            return KeyImportMessageType.BEACON_MESSAGE;
        } else if (containsPrivateOwnKey(result) && state.equals(ImportKeyWizardState.PRIVATE_KEY_WAITING)) {
            return KeyImportMessageType.PRIVATE_KEY_MESSAGE;
        } else if (isHandshakeRequest(srcMsg, rating) && state.isReadyToRequestHandshake()) {
            return KeyImportMessageType.HANDSHAKE_REQUEST_MESSAGE;
        } else {
            return KeyImportMessageType.NO_IMPORT;
        }
    }


    private <MSG extends Message> String extractSenderKey(MSG srcMsg, MimeMessage decryptedMsg, Rating rating) {
        String senderKey;
        senderKey = PEpUtils.extractKeyFromHeader(srcMsg, decryptedMsg, rating, MimeHeader.HEADER_PEP_KEY_IMPORT);
        if (senderKey.isEmpty()) {
            senderKey = PEpUtils.extractKeyFromHeader(srcMsg, decryptedMsg, rating, MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY);
        }
        return senderKey;
    }

    private <MSG extends Message> boolean isOwnMessage(MimeMessage decryptedMsg, Rating rating) {

        if (senderKey == null || senderKey.isEmpty()) {
            throw new IllegalStateException("senderKey not found: You should extractSenderKey first");
        }
        String currentKey = pEp.myself(PEpUtils.createIdentity(new Address(account.getEmail()), context)).fpr;
        return currentKey.equals(senderKey);
    }

    public void sendOwnKey(@NonNull ImportWizardPresenter.Callback callback) {
        callback.onStart();
        final String fpr = senderKey;

        new Thread(() -> {
            try {
                Timber.i("prepareOwnKey: %s", state);
                Message handshakeMessage = createPrivateKeyMessage(fpr);
                Timber.i( "sendingOwnKEy: ");
                messagingActions.sendMessage(account, handshakeMessage);
                callback.onFinish(true);
            } catch (NullPointerException | MessagingException me) {
                callback.onFinish(false);
                if (enableDebugLogging) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, me.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

            }
        }).start();
    }

    private Message createPrivateKeyMessage(String senderKey) throws MessagingException {
        MimeMessage handshakeUnencryptedMessage = createMimeMessage(account);
        handshakeUnencryptedMessage.setSubject("I'm a key");
        Message ownKeyMessage = pEp.generatePrivateKeyMessage(handshakeUnencryptedMessage, senderKey);
        if (ownKeyMessage != null) {
            ownKeyMessage.addHeader(MimeHeader.HEADER_PEP_AUTOCONSUME_LEGACY, "yes");
        }
        return ownKeyMessage;
    }

    @NonNull
    private MimeMessage createMimeMessage(Account account) throws MessagingException {
        foundation.pEp.jniadapter.Message resultM;
        resultM = new foundation.pEp.jniadapter.Message();
        Address address = new Address(account.getEmail());
        foundation.pEp.jniadapter.Identity identity = PEpUtils.createIdentity(address, context);
        identity = pEp.myself(identity);
        resultM.setFrom(identity);
        resultM.setTo(new Vector<>(Collections.singletonList(identity)));
        ArrayList<Pair<String, String>> fields = new ArrayList<>();
        fields.add(new foundation.pEp.jniadapter.Pair<>(MimeHeader.HEADER_PEP_KEY_IMPORT_LEGACY, identity.fpr));
        fields.add(new foundation.pEp.jniadapter.Pair<>(MimeHeader.HEADER_PEP_AUTOCONSUME_LEGACY, "yes"));
        resultM.setOptFields(fields);

        resultM.setSent(new Date(System.currentTimeMillis()));

        MimeMessageBuilder builder = new MimeMessageBuilder(resultM).newInstance();

        builder.setSubject("Please ignore, this message is part of import key protocol")
                .setSentDate(new Date())
                .setHideTimeZone(K9.hideTimeZone())
                .setIdentity(account.getIdentity(0))
                .setTo(Collections.singletonList(address))
                .setMessageFormat(SimpleMessageFormat.TEXT)


                .setText("").setAttachments(Collections.emptyList());

        return builder.parseMessage(resultM);
    }

    public <MSG extends Message> boolean isKeyImportMessage(MSG message, PEpProvider.DecryptResult decryptResult) {
        return getKeySourceType(message, decryptResult.msg, decryptResult).isImportType();
    }

    private <MSG extends Message> boolean isHandshakeRequest(MSG message, Rating rating) {
        return state != ImportKeyWizardState.INIT && (PEpUtils.extractRating(message).value >= Rating.pEpRatingReliable.value
                || rating.value >= Rating.pEpRatingReliable.value);
    }

    private boolean containsPrivateOwnKey(PEpProvider.DecryptResult result) {
        return result.flags != -1
                && (result.flags & DecryptFlags.pEpDecryptFlagOwnPrivateKey.value) == 1;
    }

    @NonNull
    private foundation.pEp.jniadapter.Identity createSenderIdentity(Account account, String senderKey) {
        foundation.pEp.jniadapter.Identity sender = PEpUtils.createIdentity(new Address(account.getEmail()), context);
        sender.fpr = senderKey;
        sender.user_id = PEpProvider.PEP_OWN_USER_ID;
        return sender;
    }

    public interface KeyImportMessagingActions {
        void deleteMessage(Account account, Message srcMsg, String folder, LocalFolder localFolder) throws MessagingException;

        void sendMessage(Account account, Message message) throws MessagingException;

        <T extends Message> void showImportKeyDialogIfNeeded(final T message, final PEpProvider.DecryptResult result, Account account);
    }
}
