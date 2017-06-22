package com.fsck.k9.ui.messageview;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import timber.log.Timber;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.fragment.ProgressDialogFragment;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.message.extractors.EncryptionVerifier;
import com.fsck.k9.pEp.PEpPermissionChecker;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProviderFactory;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.PermissionErrorListener;
import com.fsck.k9.pEp.ui.infrastructure.DrawerLocker;
import com.fsck.k9.pEp.ui.infrastructure.MessageAction;
import com.fsck.k9.pEp.ui.listeners.FragmentPermissionListener;
import com.fsck.k9.pEp.ui.listeners.OnMessageOptionsListener;
import com.fsck.k9.pEp.ui.privacy.status.PEpStatus;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.ui.messageview.CryptoInfoDialog.OnClickShowCryptoKeyListener;
import com.fsck.k9.ui.messageview.MessageCryptoPresenter.MessageCryptoMvpView;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import com.fsck.k9.view.MessageHeader;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.single.CompositePermissionListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class MessageViewFragment extends Fragment implements ConfirmationDialogFragmentListener,
        AttachmentViewCallback, OnClickShowCryptoKeyListener, OnSwipeGestureListener {

    private static final String ARG_REFERENCE = "reference";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int ACTIVITY_CHOOSE_DIRECTORY = 3;

    public static final int REQUEST_MASK_LOADER_HELPER = (1 << 8);
    public static final int REQUEST_MASK_CRYPTO_PRESENTER = (1 << 9);
    private static final int LOCAL_MESSAGE_LOADER_ID = 1;
    private static final int DECODE_MESSAGE_LOADER_ID = 2;
    private Rating pEpRating;
    private PePUIArtefactCache pePUIArtefactCache;
    private boolean isMessageFullDownloaded;
    private CompositePermissionListener storagePermissionListener;

    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putString(ARG_REFERENCE, reference.toIdentityString());
        fragment.setArguments(args);

        return fragment;
    }

    private MessageTopView mMessageView;

    private Account mAccount;
    private MessageReference mMessageReference;
    private LocalMessage mMessage;
    private MessagingController mController;
    private DownloadManager downloadManager;
    private Handler handler = new Handler();
    private MessageLoaderHelper messageLoaderHelper;
    private MessageCryptoPresenter messageCryptoPresenter;

    /**
     * Used to temporarily store the destination folder for refile operations if a confirmation
     * dialog is shown.
     */
    private String mDstFolder;

    private MessageViewFragmentListener mFragmentListener;

    /**
     * {@code true} after {@link #onCreate(Bundle)} has been executed. This is used by
     * {@code MessageList.configureMenu()} to make sure the fragment has been initialized before
     * it is used.
     */
    private boolean mInitialized = false;

    private Context mContext;

    private AttachmentViewInfo currentAttachmentViewInfo;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity;

        try {
            mFragmentListener = (MessageViewFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This fragments adds options to the action bar
        setHasOptionsMenu(true);

        setupSwipeDetector();

        Context context = getActivity().getApplicationContext();
        mController = MessagingController.getInstance(context);
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        messageCryptoPresenter = new MessageCryptoPresenter(savedInstanceState, messageCryptoMvpView);
        mInitialized = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((DrawerLocker) getActivity()).setDrawerEnabled(false);
        Context context = getActivity().getApplicationContext();
        messageLoaderHelper =
                new MessageLoaderHelper(context, getLoaderManager(), getFragmentManager(), messageLoaderCallbacks);

        Bundle arguments = getArguments();
        String messageReferenceString = arguments.getString(ARG_REFERENCE);
        MessageReference messageReference = MessageReference.parse(messageReferenceString);

        displayMessage(messageReference);
        mMessageView.setPrivacyProtected(mAccount.ispEpPrivacyProtected());
    }

    private void setupSwipeDetector() {
        ((MessageList) getActivity()).setupGestureDetector(this);
    }

    @Override
    public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        ((MessageList) getActivity()).showNextMessage();
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        ((MessageList) getActivity()).showPreviousMessage();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        messageCryptoPresenter.onSaveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Activity activity = getActivity();
        boolean isChangingConfigurations = activity != null && activity.isChangingConfigurations();
        if (isChangingConfigurations) {
            messageLoaderHelper.onDestroyChangingConfigurations();
            return;
        }
        if (messageLoaderHelper != null) {
            messageLoaderHelper.onDestroy();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.message, container, false);

        mMessageView = (MessageTopView) view.findViewById(R.id.message_view);
        mMessageView.setAttachmentCallback(this);
        mMessageView.setMessageCryptoPresenter(messageCryptoPresenter);

        mMessageView.setOnToggleFlagClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleFlagged();
            }
        });

        mMessageView.setOnDownloadButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageView.disableDownloadButton();
                messageLoaderHelper.downloadCompleteMessage();
            }
        });
        // onDownloadRemainder();;
        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        setMessageOptionsListener();

        pePUIArtefactCache = PePUIArtefactCache.getInstance(getApplicationContext());
        return view;
    }

    private void setMessageOptionsListener() {
        mMessageView.getMessageHeader().setOnMessageOptionsListener(new OnMessageOptionsListener() {
            @Override
            public void OnMessageOptionsListener(MessageAction action) {
                if (action.equals(MessageAction.REPLY)) {
                    onReply();
                } else if (action.equals(MessageAction.REPLY_ALL)) {
                    onReplyAll();
                } else if (action.equals(MessageAction.FORWARD)) {
                    onForward();
                } else if (action.equals(MessageAction.SHARE)) {
                    onSendAlternate();
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void displayMessage(MessageReference messageReference) {
        mMessageReference = messageReference;
        Timber.d("MessageView displaying message %s", mMessageReference);

        mAccount = Preferences.getPreferences(getApplicationContext()).getAccount(mMessageReference.getAccountUuid());
        messageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, null);

        mFragmentListener.updateMenu();
    }

    public void onPendingIntentResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode & REQUEST_MASK_LOADER_HELPER) == REQUEST_MASK_LOADER_HELPER) {
            requestCode ^= REQUEST_MASK_LOADER_HELPER;
            messageLoaderHelper.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if ((requestCode & REQUEST_MASK_CRYPTO_PRESENTER) == REQUEST_MASK_CRYPTO_PRESENTER) {
            requestCode ^= REQUEST_MASK_CRYPTO_PRESENTER;
            messageCryptoPresenter.onActivityResult(requestCode, resultCode, data);
        }

        if (resultCode == RESULT_OK && requestCode == PEpStatus.REQUEST_STATUS) {
            pEpRating = (Rating) data.getSerializableExtra(PEpStatus.CURRENT_RATING);
            K9Activity activity = (K9Activity) getActivity();
            if (mAccount.ispEpPrivacyProtected()) {
                PEpUtils.colorToolbar(activity.getToolbar(), PEpUtils.getRatingColor(pEpRating, getActivity()));
                mMessage.setpEpRating(pEpRating);
            } else {
                PEpUtils.colorToolbar(activity.getToolbar(), PEpUtils.getRatingColor(Rating.pEpRatingUndefined, getActivity()));
                mMessage.setpEpRating(Rating.pEpRatingUndefined);
            }
            activity.setStatusBarPepColor(pEpRating);
            mMessageView.setHeaders(mMessage, mAccount);
        }
    }

    private void hideKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View decorView = activity.getWindow().getDecorView();
        if (decorView != null) {
            imm.hideSoftInputFromWindow(decorView.getApplicationWindowToken(), 0);
        }
    }

    private void showUnableToDecodeError() {
        FeedbackTools.showShortFeedback(getView(), getString(R.string.message_view_toast_unable_to_display_message));
    }

    private void showMessage(MessageViewInfo messageViewInfo) {
        hideKeyboard();
        boolean handledByCryptoPresenter = messageCryptoPresenter.maybeHandleShowMessage(
                mMessageView, mAccount, messageViewInfo);
        if (!handledByCryptoPresenter) {
            mMessageView.showMessage(mAccount, messageViewInfo);
//            mMessageView.getMessageHeaderView().setCryptoStatusDisabled();
        }
    }

    private void displayHeaderForLoadingMessage(LocalMessage message) {
        mMessageView.setHeaders(message, mAccount);
        if (mAccount.isOpenPgpProviderConfigured()) {
            mMessageView.getMessageHeaderView().setCryptoStatusLoading();
        }
        displayMessageSubject(getSubjectForMessage(message));
        mFragmentListener.updateMenu();
    }

    /**
     * Called from UI thread when user select Delete
     */
    public void onDelete() {
        if (K9.confirmDelete() || (K9.confirmDeleteStarred() && mMessage.isSet(Flag.FLAGGED))) {
            showDialog(R.id.dialog_confirm_delete);
        } else {
            delete();
        }
    }

    public void onToggleAllHeadersView() {
        mMessageView.getMessageHeaderView().onShowAdditionalHeaders();
    }

    public boolean allHeadersVisible() {
        return mMessageView.getMessageHeaderView().additionalHeadersVisible();
    }

    private void delete() {
        if (mMessage != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mFragmentListener.disableDeleteAction();
            LocalMessage messageToDelete = mMessage;
            mFragmentListener.showNextMessageOrReturn();
            mController.deleteMessage(mMessageReference, null);
        }
    }

    public void onRefile(String dstFolder) {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }
        if (!mController.isMoveCapable(mMessageReference)) {
            FeedbackTools.showLongFeedback(getView(), getString(R.string.move_copy_cannot_copy_unsynced_message));
            return;
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }

        if (mAccount.getSpamFolderName().equals(dstFolder) && K9.confirmSpam()) {
            mDstFolder = dstFolder;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            refileMessage(dstFolder);
        }
    }

    private void refileMessage(String dstFolder) {
        String srcFolder = mMessageReference.getFolderName();
        MessageReference messageToMove = mMessageReference;
        mFragmentListener.showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder);
    }

    public void onReply() {
        if (mMessage != null) {
            mFragmentListener.onReply(mMessage.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), PEpUtils.extractRating(mMessage));
        }
    }

    public void onReplyAll() {
        if (mMessage != null) {
            mFragmentListener.onReplyAll(mMessage.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), PEpUtils.extractRating(mMessage));
        }
    }

    public void onForward() {
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), PEpUtils.extractRating(mMessage));
        }
    }

    public void onToggleFlagged() {
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    Collections.singletonList(mMessage), Flag.FLAGGED, newState);
            mMessageView.setHeaders(mMessage, mAccount);
        }
    }

    public void onMove() {
        if ((!mController.isMoveCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isMoveCapable(mMessageReference)) {
            FeedbackTools.showLongFeedback(getView(), getString(R.string.move_copy_cannot_copy_unsynced_message));
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);

    }

    public void onCopy() {
        if ((!mController.isCopyCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isCopyCapable(mMessageReference)) {
            FeedbackTools.showLongFeedback(getView(), getString(R.string.move_copy_cannot_copy_unsynced_message));
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    public void onArchive() {
        onRefile(mAccount.getArchiveFolderName());
    }

    public void onSpam() {
        onRefile(mAccount.getSpamFolderName());
    }

    public void onSelectText() {
        // FIXME
        // mMessageView.beginSelectingText();
    }

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.getFolderName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference.toIdentityString());
        startActivityForResult(intent, activity);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK && resultCode != RESULT_CANCELED) {
            messageCryptoPresenter.onActivityResult(requestCode, resultCode, data);
            return;
        }

        // Note: because fragments do not have a startIntentSenderForResult method, pending intent activities are
        // launched through the MessageList activity, and delivered back via onPendingIntentResult()

        switch (requestCode) {
            case ACTIVITY_CHOOSE_DIRECTORY: {
                if (data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            getAttachmentController(currentAttachmentViewInfo).saveAttachmentTo(filePath);
                        }
                    }
                }
                break;
            }
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY: {
                if (data == null) {
                    return;
                }

                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                String messageReferenceString = data.getStringExtra(ChooseFolder.EXTRA_MESSAGE);
                MessageReference ref = MessageReference.parse(messageReferenceString);
                if (mMessageReference.equals(ref)) {
                    mAccount.setLastSelectedFolderName(destFolderName);
                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE: {
                            mFragmentListener.showNextMessageOrReturn();
                            moveMessage(ref, destFolderName);
                            break;
                        }
                        case ACTIVITY_CHOOSE_FOLDER_COPY: {
                            copyMessage(ref, destFolderName);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    public void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(getActivity(), mAccount, mMessage);
        }
    }

    public void onToggleRead() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    Collections.singletonList(mMessage), Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            displayMessageSubject(subject);
            mFragmentListener.updateMenu();
        }
    }

    private void setProgress(boolean enable) {
        if (mFragmentListener != null) {
            mFragmentListener.setProgress(enable);
        }
    }

    private void displayMessageSubject(String subject) {
        if (mFragmentListener != null) {
            mFragmentListener.displayMessageSubject(subject);
        }
    }

    private String getSubjectForMessage(LocalMessage message) {
        String subject = message.getSubject();
        if (TextUtils.isEmpty(subject)) {
            return mContext.getString(R.string.general_no_subject);
        }

        return subject;
    }

    public void moveMessage(MessageReference reference, String destFolderName) {
        mController.moveMessage(mAccount, mMessageReference.getFolderName(), reference, destFolderName);
    }

    public void copyMessage(MessageReference reference, String destFolderName) {
        mController.copyMessage(mAccount, mMessageReference.getFolderName(), reference, destFolderName);
    }

    private void showDialog(int dialogId) {
        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);
                String message = getString(R.string.dialog_confirm_delete_message);
                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);
                String message = getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, 1);
                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_attachment_progress: {
                String message = getString(R.string.dialog_attachment_progress_title);
                fragment = ProgressDialogFragment.newInstance(null, message);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private void removeDialog(int dialogId) {
        FragmentManager fm = getFragmentManager();

        if (fm == null || isRemoving() || isDetached()) {
            return;
        }

        // Make sure the "show dialog" transaction has been processed when we call
        // findFragmentByTag() below. Otherwise the fragment won't be found and the dialog will
        // never be dismissed.
        fm.executePendingTransactions();

        DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(getDialogTag(dialogId));

        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    public void zoom(KeyEvent event) {
        // mMessageView.zoom(event);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                delete();
                break;
            }
            case R.id.dialog_confirm_spam: {
                refileMessage(mDstFolder);
                mDstFolder = null;
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        /* do nothing */
    }

    @Override
    public void dialogCancelled(int dialogId) {
        /* do nothing */
    }

    /**
     * Get the {@link MessageReference} of the currently displayed message.
     */
    public MessageReference getMessageReference() {
        return mMessageReference;
    }

    public boolean isMessageRead() {
        return (mMessage != null) ? mMessage.isSet(Flag.SEEN) : false;
    }

    public boolean isCopyCapable() {
        return mController.isCopyCapable(mAccount);
    }

    public boolean isMoveCapable() {
        return mController.isMoveCapable(mAccount);
    }

    public boolean canMessageBeArchived() {
        return (!mMessageReference.getFolderName().equals(mAccount.getArchiveFolderName())
                && mAccount.hasArchiveFolder());
    }

    public boolean canMessageBeMovedToSpam() {
        return (!mMessageReference.getFolderName().equals(mAccount.getSpamFolderName())
                && mAccount.hasSpamFolder());
    }

    public void updateTitle() {
        if (mMessage != null) {
            displayMessageSubject(mMessage.getSubject());
        }
    }

    public Context getApplicationContext() {
        return getActivity().getApplication();
    }

    public View getRootView() {
        return ((MessageList) getActivity()).getRootView();
    }

    public void disableAttachmentButtons(AttachmentViewInfo attachment) {
        // mMessageView.disableAttachmentButtons(attachment);
    }

    public void enableAttachmentButtons(AttachmentViewInfo attachment) {
        // mMessageView.enableAttachmentButtons(attachment);
    }

    public void runOnMainThread(Runnable runnable) {
        handler.post(runnable);
    }

    public void showAttachmentLoadingDialog() {
        // mMessageView.disableAttachmentButtons();
        showDialog(R.id.dialog_attachment_progress);
    }

    public void hideAttachmentLoadingDialogOnMainThread() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                removeDialog(R.id.dialog_attachment_progress);
                // mMessageView.enableAttachmentButtons();
            }
        });
    }

    public void refreshAttachmentThumbnail(AttachmentViewInfo attachment) {
        // mMessageView.refreshAttachmentThumbnail(attachment);
    }

    private MessageCryptoMvpView messageCryptoMvpView = new MessageCryptoMvpView() {
        @Override
        public void redisplayMessage() {
            messageLoaderHelper.asyncReloadMessage();
        }

        @Override
        public void startPendingIntentForCryptoPresenter(IntentSender si, Integer requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) throws SendIntentException {
            if (requestCode == null) {
                getActivity().startIntentSender(si, fillIntent, flagsMask, flagValues, extraFlags);
                return;
            }

            requestCode |= REQUEST_MASK_CRYPTO_PRESENTER;
            getActivity().startIntentSenderForResult(
                    si, requestCode, fillIntent, flagsMask, flagValues, extraFlags);
        }

        @Override
        public void showCryptoInfoDialog(MessageCryptoDisplayStatus displayStatus) {
            CryptoInfoDialog dialog = CryptoInfoDialog.newInstance(displayStatus);
            dialog.setTargetFragment(MessageViewFragment.this, 0);
            dialog.show(getFragmentManager(), "crypto_info_dialog");
        }

        @Override
        public void restartMessageCryptoProcessing() {
            mMessageView.setToLoadingState();
            messageLoaderHelper.asyncRestartMessageCryptoProcessing();
        }
    };

    @Override
    public void onClickShowCryptoKey() {
        messageCryptoPresenter.onClickShowCryptoKey();
    }

    public void onPepStatus() {
        ArrayList<Identity> addresses = new ArrayList<>();
        addresses.addAll(PEpUtils.createIdentities(Arrays.asList(mMessage.getFrom()), getApplicationContext()));
        addresses.addAll(PEpUtils.createIdentities(Arrays.asList(mMessage.getRecipients(Message.RecipientType.TO)), getApplicationContext()));
        addresses.addAll(PEpUtils.createIdentities(Arrays.asList(mMessage.getRecipients(Message.RecipientType.CC)), getApplicationContext()));

        String myAdress = mAccount.getEmail();
        pePUIArtefactCache.setRecipients(mAccount, addresses);
        for (String s : mMessage.getHeaderNames()) {
            for (String s1 : mMessage.getHeader(s)) {
                Timber.i("MessageHeader", "onClick " + s + " " + s1);
            }
        }

        PEpStatus.actionShowStatus(getActivity(), pEpRating, mMessage.getFrom()[0].getAddress(), getMessageReference(), true, myAdress);
    }

    public interface MessageViewFragmentListener {
        void onForward(MessageReference messageReference, Parcelable decryptionResultForReply,
                       Rating pEpRating);
        void disableDeleteAction();
        void onReplyAll(MessageReference messageReference, Parcelable decryptionResultForReply,
                        Rating pEpRating);
        void onReply(MessageReference messageReference, Parcelable decryptionResultForReply,
                     Rating pEpRating);
        void displayMessageSubject(String title);
        void setProgress(boolean b);
        void showNextMessageOrReturn();
        void messageHeaderViewAvailable(MessageHeader messageHeaderView);
        void updateMenu();
    }

    public boolean isInitialized() {
        return mInitialized ;
    }

    private MessageLoaderCallbacks messageLoaderCallbacks = new MessageLoaderCallbacks() {
        @Override
        public void onMessageDataLoadFinished(LocalMessage message) {
            mMessage = message;
            isMessageFullDownloaded = mMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                    !MessageExtractor.hasMissingParts(mMessage);

            displayHeaderForLoadingMessage(message);
            mMessageView.setToLoadingState();

            // recover pEpRating from db, if is null,
            // then we take the one in the header and store it
            pEpRating = message.getpEpRating();
            if (pEpRating == null) {
                pEpRating = PEpUtils.extractRating(message);
                message.setpEpRating(pEpRating);
            }

            ((MessageList) getActivity()).setMessageViewVisible(true);

            boolean hasToBeDecrypted = hasToBeDecrypted(message);
            if (hasToBeDecrypted) {
                showNeedsDecryptionFeedback(message);
            }

            if (mAccount.ispEpPrivacyProtected()) {
                PEpUtils.colorToolbar(pePUIArtefactCache, ((MessageList) getActivity()).getSupportActionBar(), pEpRating);
            } else {
                PEpUtils.colorToolbar(pePUIArtefactCache, ((MessageList) getActivity()).getSupportActionBar(), Rating.pEpRatingUndefined);
                pEpRating = Rating.pEpRatingUndefined;
            }
            ((MessageList) getActivity()).setStatusBarPepColor(pEpRating);
        }

        @Override
        public void onMessageDataLoadFailed() {
            FeedbackTools.showLongFeedback(getView(), getString(R.string.status_loading_error));
        }

        @Override
        public void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo) {
            showMessage(messageViewInfo);
        }

        @Override
        public void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo) {
            showMessage(messageViewInfo);
        }

        @Override
        public void setLoadingProgress(int current, int max) {
            mMessageView.setLoadingProgress(current, max);
        }

        @Override
        public void onDownloadErrorMessageNotFound() {
            mMessageView.enableDownloadButton();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FeedbackTools.showLongFeedback(getView(), getString(R.string.status_invalid_id_error));
                }
            });
        }

        @Override
        public void onDownloadErrorNetworkError() {
            mMessageView.enableDownloadButton();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FeedbackTools.showLongFeedback(getView(), getString(R.string.status_network_error));
                }
            });
        }

        @Override
        public void startIntentSenderForMessageLoaderHelper(IntentSender si, int requestCode, Intent fillIntent,
                int flagsMask, int flagValues, int extraFlags) {
            try {
                requestCode |= REQUEST_MASK_LOADER_HELPER;
                getActivity().startIntentSenderForResult(
                        si, requestCode, fillIntent, flagsMask, flagValues, extraFlags);
            } catch (SendIntentException e) {
                Timber.e(e, "Irrecoverable error calling PendingIntent!");
            }
        }
    };

    private void showKeyNotFoundFeedback() {
        String title = pePUIArtefactCache.getTitle(Rating.pEpRatingHaveNoKey);
        String message = pePUIArtefactCache.getSuggestion(Rating.pEpRatingHaveNoKey);
        Activity activity = getActivity();
        if (activity != null) {
            new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.okay_action), null)
                    .create().show();
        }
    }

    private void showNeedsDecryptionFeedback(final LocalMessage message) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.decrypt_message_explanation)
                .setPositiveButton(getString(R.string.okay_action), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        decryptMessage(message);
                    }
                })
                .setNegativeButton(getString(R.string.cancel_action), null)
                .create().show();
    }

    private boolean hasToBeDecrypted(LocalMessage message) {
        return EncryptionVerifier.isEncrypted(message) && isMessageFullDownloaded;
    }

    private boolean canDecrypt() {
        return pEpRating.value != Rating.pEpRatingCannotDecrypt.value;
    }

    private void decryptMessage(LocalMessage message) {
        PEpProvider pEpProvider = PEpProviderFactory.createAndSetupProvider(getActivity());
        pEpProvider.decryptMessage(mMessage, new PEpProvider.ResultCallback<PEpProvider.DecryptResult>() {
            @Override
            public void onLoaded(PEpProvider.DecryptResult decryptResult) {
                try {

                    MimeMessage decryptedMessage = decryptResult.msg;
                    if (message.getFolder().getName().equals(mAccount.getSentFolderName())
                            || message.getFolder().getName().equals(mAccount.getDraftsFolderName())) {
                        decryptedMessage.setHeader(MimeHeader.HEADER_PEP_RATING, PEpUtils.ratingToString(pEpProvider.getPrivacyState(message)));
                    }

                    decryptedMessage.setUid(message.getUid());      // sync UID so we know our mail...

                    // Store the updated message locally
                    LocalFolder folder = mMessage.getFolder();
                    LocalMessage localMessage = null;

                    localMessage = folder.storeSmallMessage(decryptedMessage, () -> {
                    });
                    mMessage = localMessage;
                    if (Rating.pEpRatingHaveNoKey.value == decryptResult.rating.value
                            || !canDecrypt()) {
                        showKeyNotFoundFeedback();
                    } else {
                        refreshMessage();
                    }
                } catch (MessagingException e) {
                    Timber.e("pEp", "decryptMessage: view", e);
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private void refreshMessage() {
        //If get support manager is null, it means that you don't have a fragment to refresh
        if (getFragmentManager() != null) {
            MessageViewFragment fragment = MessageViewFragment.newInstance(mMessageReference);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.message_view_container, fragment);
            ft.commit();
        }
    }


    @Override
    public void onViewAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first

        getAttachmentController(attachment).viewAttachment();
    }

    @Override
    public void onSaveAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first
        createPermissionListeners();
        if (PEpPermissionChecker.hasWriteExternalPermission(getActivity())) {
            getAttachmentController(attachment).saveAttachment();
        }
    }

    @Override
    public void onSaveAttachmentToUserProvidedDirectory(final AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first

        currentAttachmentViewInfo = attachment;
        FileBrowserHelper.getInstance().showFileBrowserActivity(MessageViewFragment.this, null,
                ACTIVITY_CHOOSE_DIRECTORY, new FileBrowserFailOverCallback() {
                    @Override
                    public void onPathEntered(String path) {
                        getAttachmentController(attachment).saveAttachmentTo(path);
                    }

                    @Override
                    public void onCancel() {
                        // Do nothing
                    }
                });
    }

    private AttachmentController getAttachmentController(AttachmentViewInfo attachment) {
        return new AttachmentController(mController, downloadManager, this, attachment);
    }

    private void createPermissionListeners() {
        FragmentPermissionListener feedbackViewPermissionListener = new FragmentPermissionListener(this);

        String explanation = getResources().getString(R.string.download_permission_first_explanation);
        storagePermissionListener = new CompositePermissionListener(feedbackViewPermissionListener,
                SnackbarOnDeniedPermissionListener.Builder.with(mMessageView, explanation)
                        .withOpenSettingsButton(R.string.button_settings)
                        .build());
        Dexter.withActivity(getActivity())
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(storagePermissionListener)
                .withErrorListener(new PermissionErrorListener())
                .onSameThread()
                .check();
    }

    public void showPermissionGranted(String permissionName) {
    }

    public void showPermissionDenied(String permissionName, boolean permanentlyDenied) {
        String permissionDenied = getResources().getString(R.string.download_snackbar_permission_permanently_denied);
        FeedbackTools.showLongFeedback(mMessageView,  permissionDenied);
    }

    public void showPermissionRationale(PermissionToken token) {
        String rationaleExplanation = getResources().getString(R.string.download_snackbar_permission_rationale);
        new AlertDialog.Builder(getActivity()).setTitle(R.string.download_permission_rationale_title)
                .setMessage(rationaleExplanation)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    token.cancelPermissionRequest();
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    token.continuePermissionRequest();
                })
                .setOnDismissListener(dialog -> token.cancelPermissionRequest())
                .show();
    }
}