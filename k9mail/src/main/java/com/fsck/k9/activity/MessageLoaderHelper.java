package com.fsck.k9.activity;


import static com.fsck.k9.planck.PlanckProvider.KEY_COULD_NOT_DECRYPT_MESSAGE;
import static com.fsck.k9.planck.PlanckProvider.KEY_MISSING_ERROR_MESSAGE;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.extensions.LocalMessageKt;
import com.fsck.k9.helper.RetainFragment;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.mailstore.MessageViewInfoExtractor;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import com.fsck.k9.ui.crypto.MessageCryptoCallback;
import com.fsck.k9.ui.crypto.MessageCryptoHelper;
import com.fsck.k9.ui.message.LocalMessageExtractorLoader;
import com.fsck.k9.ui.message.LocalMessageLoader;

import org.openintents.openpgp.OpenPgpDecryptionResult;

import timber.log.Timber;


/** This class is responsible for loading a message start to finish, and
 * retaining or reloading the loading state on configuration changes.
 *
 * In particular, it takes care of the following:
 *  - load raw message data from the database, using LocalMessageLoader
 *  - download partial message content if it is missing using MessagingController
 *  - apply crypto operations if applicable, using MessageCryptoHelper
 *  - extract MessageViewInfo from the message and crypto data using DecodeMessageLoader
 *  - download complete message content for partially downloaded messages if requested
 *
 * No state is retained in this object itself. Instead, state is stored in the
 * message loaders and the MessageCryptoHelper which is stored in a
 * RetainFragment. The public interface is intended for use by an Activity or
 * Fragment, which should construct a new instance of this class in onCreate,
 * then call asyncStartOrResumeLoadingMessage to start or resume loading the
 * message, receiving callbacks when it is loaded.
 *
 * When the Activity or Fragment is ultimately destroyed, it should call
 * onDestroy, which stops loading and deletes all state kept in loaders and
 * fragments by this object. If it is only destroyed for a configuration
 * change, it should call onDestroyChangingConfigurations, which cancels any
 * further callbacks from this object but retains the loading state to resume
 * from at the next call to asyncStartOrResumeLoadingMessage.
 *
 * If the message is already loaded, a call to asyncStartOrResumeLoadingMessage
 * will typically load by starting the decode message loader, retrieving the
 * already cached LocalMessage. This message will be passed to the retained
 * CryptoMessageHelper instance, returning the already cached
 * MessageCryptoAnnotations. These two objects will be checked against the
 * retained DecodeMessageLoader, returning the final result. At each
 * intermediate step, the input of the respective loaders will be checked for
 * consistency, reloading if there is a mismatch.
 *
 */
public class MessageLoaderHelper {
    private static final int LOCAL_MESSAGE_LOADER_ID = 1;
    private static final int DECODE_MESSAGE_LOADER_ID = 2;


    // injected state - all of this may be cleared to avoid data leakage!
    private Context context;
    private FragmentManager fragmentManager;
    private LoaderManager loaderManager;
    @Nullable // make this explicitly nullable, make sure to cancel/ignore any operation if this is null
    private MessageLoaderCallbacks callback;
    @Nullable // make this explicitly nullable, make sure to cancel/ignore any operation if this is null
    private MessageLoaderDecryptCallbacks decryptCallback;

    private PlanckProvider planckProvider;

    // transient state
    private MessageReference messageReference;
    private Account account;

    private LocalMessage localMessage;
    private MessageCryptoAnnotations messageCryptoAnnotations;
    private OpenPgpDecryptionResult cachedDecryptionResult;

    private MessageCryptoHelper messageCryptoHelper;
    private final MessageViewInfoExtractor messageViewInfoExtractor;
    private Handler handler = new Handler(Looper.getMainLooper());


    public MessageLoaderHelper(Context context,
                               LoaderManager loaderManager,
                               FragmentManager fragmentManager,
                               @NonNull MessageLoaderCallbacks callback,
                               DisplayHtml displayHtml) {
        this.context = context;
        this.loaderManager = loaderManager;
        this.fragmentManager = fragmentManager;
        this.callback = callback;
        this.planckProvider = ((K9) context.getApplicationContext()).planckProvider;
        this.messageViewInfoExtractor = new MessageViewInfoExtractor(context,
                AttachmentInfoExtractor.getInstance(), displayHtml);
    }

    public MessageLoaderHelper(Context context, LoaderManager loaderManager, FragmentManager fragmentManager,
                               @NonNull MessageLoaderCallbacks callback,
                               @NonNull MessageLoaderDecryptCallbacks decryptCallback,
                               DisplayHtml displayHtml) {
        this(context, loaderManager, fragmentManager, callback, displayHtml);
        this.decryptCallback = decryptCallback;
    }

    // public interface

    @UiThread
    public void asyncStartOrResumeLoadingMessage(MessageReference messageReference, Parcelable cachedDecryptionResult) {
        this.messageReference = messageReference;
        this.account = Preferences.getPreferences(context).getAccount(messageReference.getAccountUuid());

        if (cachedDecryptionResult != null) {
            if (cachedDecryptionResult instanceof OpenPgpDecryptionResult) {
                this.cachedDecryptionResult = (OpenPgpDecryptionResult) cachedDecryptionResult;
            } else {
                Timber.e("Got decryption result of unknown type - ignoring");
            }
        }
        startOrResumeLocalMessageLoader();
    }

    @UiThread
    public void asyncReloadMessage() {
        startOrResumeLocalMessageLoader();
    }

    @UiThread
    public void asyncRestartMessageCryptoProcessing() {
        cancelAndClearCryptoOperation();
        cancelAndClearDecodeLoader();

        if (account.isOpenPgpProviderConfigured()) {
            String openPgpProvider = account.getOpenPgpProvider();
            startOrResumeCryptoOperation(openPgpProvider);
        } else {
            startOrResumeDecodeMessage();
        }
    }

    /** Cancels all loading processes, prevents future callbacks, and destroys all loading state. */
    @UiThread
    public void onDestroy() {
        if (messageCryptoHelper != null) {
            messageCryptoHelper.cancelIfRunning();
        }

        callback = null;
        context = null;
        fragmentManager = null;
        loaderManager = null;
    }

    /** Prevents future callbacks, but retains loading state to pick up from in a call to
     * asyncStartOrResumeLoadingMessage in a new instance of this class. */
    @UiThread
    public void onDestroyChangingConfigurations() {
        cancelAndClearDecodeLoader();

        if (messageCryptoHelper != null) {
            messageCryptoHelper.detachCallback();
        }

        callback = null;
        context = null;
        fragmentManager = null;
        loaderManager = null;
    }

    @UiThread
    public void downloadCompleteMessage() {
        startDownloadingMessageBody(true);
    }

    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        messageCryptoHelper.onActivityResult(requestCode, resultCode, data);
    }


    // load from database

    private void startOrResumeLocalMessageLoader() {
        LocalMessageLoader loader =
                (LocalMessageLoader) loaderManager.<LocalMessage>getLoader(LOCAL_MESSAGE_LOADER_ID);
        boolean isLoaderStale = (loader != null) && loader.isCreatedFor(messageReference);

        if (isLoaderStale) {
            Timber.d("Creating new local message loader");
            cancelAndClearCryptoOperation();
            cancelAndClearDecodeLoader();
            loaderManager.restartLoader(LOCAL_MESSAGE_LOADER_ID, null, localMessageLoaderCallback);
        } else {
            Timber.d("Reusing local message loader");
            loaderManager.initLoader(LOCAL_MESSAGE_LOADER_ID, null, localMessageLoaderCallback);
        }
    }

    @UiThread
    private void onLoadMessageFromDatabaseFinished() {
        if (callback == null) {
            throw new IllegalStateException("unexpected call when callback is already detached");
        }

        if (LocalMessageKt.hasToBeDecrypted(localMessage)) {
            decryptMessage(localMessage);
        }

        callback.onMessageDataLoadFinished(localMessage);

        boolean messageIncomplete = LocalMessageKt.isMessageIncomplete(localMessage);
        if (messageIncomplete) {
            startDownloadingMessageBody(false);
            return;
        }

        /*if (onlyLoadMetadata) {
            MessageViewInfo messageViewInfo = MessageViewInfo.createForMetadataOnly(localMessage, !downloadedCompletely);
            onDecodeMessageFinished(messageViewInfo);
            return;
        }

        String openPgpProvider = account.getOpenPgpProvider();
        if (openPgpProvider != null) {
            startOrResumeCryptoOperation(openPgpProvider);
            return;
        }*/

        startOrResumeDecodeMessage();
    }

    private void onLoadMessageFromDatabaseFailed() {
        if (callback == null) {
            throw new IllegalStateException("unexpected call when callback is already detached");
        }
        callback.onMessageDataLoadFailed();
    }

    public void cancelAndClearLocalMessageLoader() {
        loaderManager.destroyLoader(LOCAL_MESSAGE_LOADER_ID);
    }

    private LoaderCallbacks<LocalMessage> localMessageLoaderCallback = new LoaderCallbacks<LocalMessage>() {
        @Override
        public Loader<LocalMessage> onCreateLoader(int id, Bundle args) {
            if (id != LOCAL_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message loader id");
            }

            return new LocalMessageLoader(context, MessagingController.getInstance(context), account, messageReference);
        }

        @Override
        public void onLoadFinished(Loader<LocalMessage> loader, LocalMessage message) {
            if (loader.getId() != LOCAL_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message loader id");
            }

            localMessage = message;
            if (message == null) {
                onLoadMessageFromDatabaseFailed();
            } else {
                onLoadMessageFromDatabaseFinished();
            }
        }

        @Override
        public void onLoaderReset(Loader<LocalMessage> loader) {
            if (loader.getId() != LOCAL_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message loader id");
            }
            // Do nothing
        }
    };

    // process with crypto helper

    private void startOrResumeCryptoOperation(String openPgpProvider) {
        RetainFragment<MessageCryptoHelper> retainCryptoHelperFragment = getMessageCryptoHelperRetainFragment(true);
        if (retainCryptoHelperFragment.hasData()) {
            messageCryptoHelper = retainCryptoHelperFragment.getData();
        }
        if (messageCryptoHelper == null || !messageCryptoHelper.isConfiguredForOpenPgpProvider(openPgpProvider)) {
            messageCryptoHelper = new MessageCryptoHelper(
                    context,  openPgpProvider);
            retainCryptoHelperFragment.setData(messageCryptoHelper);
        }
        messageCryptoHelper.asyncStartOrResumeProcessingMessage(
                localMessage, messageCryptoCallback, cachedDecryptionResult, !account.getOpenPgpHideSignOnly());
    }

    private void cancelAndClearCryptoOperation() {
        RetainFragment<MessageCryptoHelper> retainCryptoHelperFragment = getMessageCryptoHelperRetainFragment(false);
        if (retainCryptoHelperFragment != null) {
            if (retainCryptoHelperFragment.hasData()) {
                messageCryptoHelper = retainCryptoHelperFragment.getData();
                messageCryptoHelper.cancelIfRunning();
                messageCryptoHelper = null;
            }
            retainCryptoHelperFragment.clearAndRemove(fragmentManager);
        }
    }

    private RetainFragment<MessageCryptoHelper> getMessageCryptoHelperRetainFragment(boolean createIfNotExists) {
        if (createIfNotExists) {
            return RetainFragment.findOrCreate(fragmentManager, "crypto_helper_" + messageReference.hashCode());
        } else {
            return RetainFragment.findOrNull(fragmentManager, "crypto_helper_" + messageReference.hashCode());
        }
    }

    private MessageCryptoCallback messageCryptoCallback = new MessageCryptoCallback() {
        @Override
        public void onCryptoHelperProgress(int current, int max) {
            if (callback == null) {
                throw new IllegalStateException("unexpected call when callback is already detached");
            }

            callback.setLoadingProgress(current, max);
        }

        @Override
        public void onCryptoOperationsFinished(MessageCryptoAnnotations annotations) {
            if (callback == null) {
                throw new IllegalStateException("unexpected call when callback is already detached");
            }

            messageCryptoAnnotations = annotations;
            startOrResumeDecodeMessage();
        }

        @Override
        public void startPendingIntentForCryptoHelper(IntentSender si, int requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) {
            if (callback == null) {
                throw new IllegalStateException("unexpected call when callback is already detached");
            }

            callback.startIntentSenderForMessageLoaderHelper(si, requestCode, fillIntent,
                    flagsMask, flagValues, extraFlags);
        }
    };

    // decode message

    private void startOrResumeDecodeMessage() {
        LocalMessageExtractorLoader loader =
                (LocalMessageExtractorLoader) loaderManager.<MessageViewInfo>getLoader(DECODE_MESSAGE_LOADER_ID);
        boolean isLoaderStale = (loader == null) || !loader.isCreatedFor(localMessage, messageCryptoAnnotations);

        if (isLoaderStale) {
            Timber.d("Creating new decode message loader");
            loaderManager.restartLoader(DECODE_MESSAGE_LOADER_ID, null, decodeMessageLoaderCallback);
        } else {
            Timber.d("Reusing decode message loader");
            loaderManager.initLoader(DECODE_MESSAGE_LOADER_ID, null, decodeMessageLoaderCallback);
        }
    }

    private void onDecodeMessageFinished(MessageViewInfo messageViewInfo) {
        if (callback == null) {
            throw new IllegalStateException("unexpected call when callback is already detached");
        }

        if (messageViewInfo == null) {
            messageViewInfo = createErrorStateMessageViewInfo();
            callback.onMessageViewInfoLoadFailed(messageViewInfo);
            return;
        }

        callback.onMessageViewInfoLoadFinished(messageViewInfo);
    }

    @NonNull
    private MessageViewInfo createErrorStateMessageViewInfo() {
        boolean isMessageIncomplete = !localMessage.isSet(Flag.X_DOWNLOADED_FULL);
        return MessageViewInfo.createWithErrorState(localMessage, isMessageIncomplete);
    }

    private void cancelAndClearDecodeLoader() {
        loaderManager.destroyLoader(DECODE_MESSAGE_LOADER_ID);
    }

    private LoaderCallbacks<MessageViewInfo> decodeMessageLoaderCallback = new LoaderCallbacks<MessageViewInfo>() {
        @Override
        public Loader<MessageViewInfo> onCreateLoader(int id, Bundle args) {
            if (id != DECODE_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message decoder id");
            }
            return new LocalMessageExtractorLoader(context, localMessage, messageCryptoAnnotations, messageViewInfoExtractor);
        }

        @Override
        public void onLoadFinished(Loader<MessageViewInfo> loader, MessageViewInfo messageViewInfo) {
            if (loader.getId() != DECODE_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message decoder id");
            }
            onDecodeMessageFinished(messageViewInfo);
        }

        @Override
        public void onLoaderReset(Loader<MessageViewInfo> loader) {
            if (loader.getId() != DECODE_MESSAGE_LOADER_ID) {
                throw new IllegalStateException("loader id must be message decoder id");
            }
            // Do nothing
        }
    };


    // download missing body

    private void startDownloadingMessageBody(boolean downloadComplete) {
        if (downloadComplete) {
            MessagingController.getInstance(context).loadMessageRemote(
                    account, messageReference.getFolderName(), messageReference.getUid(), downloadMessageListener);
        } else {
            MessagingController.getInstance(context).loadMessageRemotePartial(
                    account, messageReference.getFolderName(), messageReference.getUid(), downloadMessageListener);
        }
    }

    private void onMessageDownloadFinished() {
        if (callback == null) {
            return;
        }

        cancelAndClearLocalMessageLoader();
        cancelAndClearDecodeLoader();
        cancelAndClearCryptoOperation();

        startOrResumeLocalMessageLoader();
    }

    private void onDownloadMessageFailed(final Throwable t) {
        if (callback == null) {
            return;
        }

        if (t instanceof IllegalArgumentException) {
            callback.onDownloadErrorMessageNotFound();
        } else {
            callback.onDownloadErrorNetworkError();
        }
    }

    MessagingListener downloadMessageListener = new SimpleMessagingListener() {
        @Override
        public void loadMessageRemoteFinished(final Account account, final String folder, final String uid) {
            handler.post(() -> {
                if (!messageReference.equals(account.getUuid(), folder, uid)) {
                    return;
                }
                onMessageDownloadFinished();
            });
        }

        @Override
        public void loadMessageRemoteFailed(Account account, String folder, String uid, final Throwable t) {
            handler.post(() -> onDownloadMessageFailed(t));
        }
    };

    // decrypt message

    private void decryptMessage(LocalMessage message) {
        planckProvider.decryptMessage(message, account, new PlanckProvider.ResultCallback<PlanckProvider.DecryptResult>() {
            @Override
            public void onLoaded(PlanckProvider.DecryptResult decryptResult) {
                try {
                    MimeMessage decryptedMessage = decryptResult.msg;
                    // sync UID so we know our mail...
                    decryptedMessage.setUid(message.getUid());
                    // Store the updated message locally
                    LocalFolder folder = message.getFolder();
                    folder.storeSmallMessage(decryptedMessage, () -> {
                        if (decryptCallback != null)
                            decryptCallback.onMessageDecrypted();
                    });
                } catch (MessagingException e) {
                    Timber.e("pEp %s", "decryptMessage: view", e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (decryptCallback != null && throwable.getMessage().equals(KEY_MISSING_ERROR_MESSAGE)) {
                    decryptCallback.onMessageDataDecryptFailed(KEY_MISSING_ERROR_MESSAGE);
                } else if (decryptCallback != null) {
                    decryptCallback.onMessageDataDecryptFailed(KEY_COULD_NOT_DECRYPT_MESSAGE);
                }
            }
        });
    }


    // main callback interface

    public interface MessageLoaderCallbacks {
        void onMessageDataLoadFinished(LocalMessage message);
        void onMessageDataLoadFailed();

        void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo);
        void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo);

        void setLoadingProgress(int current, int max);

        void startIntentSenderForMessageLoaderHelper(IntentSender si, int requestCode, Intent fillIntent, int flagsMask,
                int flagValues, int extraFlags);

        void onDownloadErrorMessageNotFound();
        void onDownloadErrorNetworkError();
    }

    // decrypt callback interface

    public interface MessageLoaderDecryptCallbacks {
        void onMessageDecrypted();
        void onMessageDataDecryptFailed(String errorMessage);
    }

}
