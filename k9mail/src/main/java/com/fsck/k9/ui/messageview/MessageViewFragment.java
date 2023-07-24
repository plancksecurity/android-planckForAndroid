package com.fsck.k9.ui.messageview;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static foundation.pEp.jniadapter.Rating.pEpRatingUndefined;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderCallbacks;
import com.fsck.k9.activity.MessageLoaderHelper.MessageLoaderDecryptCallbacks;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.AttachmentDownloadDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.infrastructure.MessageView;
import com.fsck.k9.planck.infrastructure.extensions.ContextKt;
import com.fsck.k9.planck.ui.infrastructure.DrawerLocker;
import com.fsck.k9.planck.ui.listeners.OnMessageOptionsListener;
import com.fsck.k9.planck.ui.privacy.status.PlanckStatus;
import com.fsck.k9.planck.ui.tools.FeedbackTools;
import com.fsck.k9.planck.ui.tools.KeyboardUtils;
import com.fsck.k9.ui.messageview.CryptoInfoDialog.OnClickShowCryptoKeyListener;
import com.fsck.k9.ui.messageview.MessageCryptoPresenter.MessageCryptoMvpView;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import com.fsck.k9.view.MessageHeader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;
import security.planck.permissions.PermissionChecker;
import security.planck.permissions.PermissionRequester;
import security.planck.print.Print;
import security.planck.print.PrintMessage;
import security.planck.ui.message_compose.PlanckFabMenu;
import security.planck.ui.toolbar.PlanckSecurityStatusLayout;
import security.planck.ui.toolbar.ToolBarCustomizer;
import timber.log.Timber;

@AndroidEntryPoint
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
    private PlanckUIArtefactCache planckUIArtefactCache;
    private PlanckSecurityStatusLayout planckSecurityStatusLayout;

    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putString(ARG_REFERENCE, reference.toIdentityString());
        fragment.setArguments(args);

        return fragment;
    }

    private MessageTopView mMessageView;
    private PlanckFabMenu pEpFabMenu;
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

    private final OnMessageOptionsListener messageOptionsListener = action -> {
        switch (action) {
            case REPLY:
                onReply();
                break;
            case REPLY_ALL:
                onReplyAll();
                break;
            case FORWARD:
                onForward();
                break;
            case SHARE:
                onSendAlternate();
                break;
            case PRINT:
                onPrintMessage();
                break;
        }
    };

    public void hideInitialStatus() {
        if (planckSecurityStatusLayout != null) {
            planckSecurityStatusLayout.hideRating();
        }
    }

    @Inject
    PermissionRequester permissionRequester;
    @Inject
    PermissionChecker permissionChecker;
    @Inject
    ToolBarCustomizer toolBarCustomizer;
    @Inject
    @MessageView
    DisplayHtml displayHtml;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context.getApplicationContext();

        try {
            mFragmentListener = (MessageViewFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("This fragment must be attached to a MessageViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This fragments adds options to the action bar
        setHasOptionsMenu(true);


        Context context = getActivity().getApplicationContext();
        mController = MessagingController.getInstance(context);
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        messageCryptoPresenter = new MessageCryptoPresenter(savedInstanceState, messageCryptoMvpView);
        ((MessageList) getActivity()).hideSearchView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.message, container, false);

        Toolbar toolbar = ((MessageList) getActivity()).getToolbar();
        if (toolbar != null) {
            planckSecurityStatusLayout = toolbar.findViewById(R.id.actionbar_message_view);
        }

        mMessageView = view.findViewById(R.id.message_view);
        pEpFabMenu = view.findViewById(R.id.fab_menu);
        mMessageView.setAttachmentCallback(this);
        mMessageView.setMessageCryptoPresenter(messageCryptoPresenter);

        mMessageView.setOnDownloadButtonClickListener(v -> {
            mMessageView.disableDownloadButton();
            mMessageView.setToLoadingState();
            messageLoaderHelper.downloadCompleteMessage();
        });
        // onDownloadRemainder();;
        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        setMessageOptionsListener();

        planckUIArtefactCache = PlanckUIArtefactCache.getInstance(getApplicationContext());
        return view;
    }

    @Override
    public void onDestroyView() {
        if (planckSecurityStatusLayout != null) {
            planckSecurityStatusLayout.hideRating();
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MessageList) getActivity()).setMessageViewVisible(true);
        setupSwipeDetector();
        ((DrawerLocker) getActivity()).setDrawerEnabled(false);
        Context context = getActivity().getApplicationContext();
        messageLoaderHelper = new MessageLoaderHelper(context, LoaderManager.getInstance(this),
                getFragmentManager(), messageLoaderCallbacks, messageLoaderDecryptCallbacks,
                displayHtml);
        displayMessage();
        if (K9.isUsingTrustwords()) {
            planckSecurityStatusLayout.setOnClickListener(view -> onPEpPrivacyStatus(false));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentListener = null;
        mContext = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Activity activity = getActivity();
        planckSecurityStatusLayout.setOnClickListener(null);

        boolean isChangingConfigurations = activity != null && activity.isChangingConfigurations();
        if (isChangingConfigurations) {
            if (messageLoaderHelper != null) {
                messageLoaderHelper.onDestroyChangingConfigurations();
            }
            return;
        }
        if (messageLoaderHelper != null) {
            messageLoaderHelper.onDestroy();
        }
        getActivity().setResult(RESULT_CANCELED);
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

    private void setMessageOptionsListener() {
        mMessageView.getMessageHeader().setOnMessageOptionsListener(messageOptionsListener);
        pEpFabMenu.setClickListeners(messageOptionsListener);
    }

    public void displayMessage() {
        Bundle arguments = getArguments();
        String messageReferenceString = arguments.getString(ARG_REFERENCE);
        mMessageReference = MessageReference.parse(messageReferenceString);
        Timber.d("MessageView displaying message %s", mMessageReference);

        mAccount = Preferences.getPreferences(getApplicationContext()).getAccount(mMessageReference.getAccountUuid());
        messageLoaderHelper.asyncStartOrResumeLoadingMessage(mMessageReference, null);
        mInitialized = true;
        mFragmentListener.updateMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MessageList) ContextKt.getRootContext(requireActivity())).removeGestureDetector();
        messageLoaderHelper.cancelAndClearLocalMessageLoader();
    }

    @Override
    public void onStop() {
        planckSecurityStatusLayout.setVisibility(View.GONE);
        super.onStop();
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

        if (resultCode == RESULT_OK && requestCode == PlanckStatus.REQUEST_STATUS) {
            if (requestCode == PlanckStatus.REQUEST_STATUS) {
                Rating rating = (Rating) data.getSerializableExtra(PlanckStatus.CURRENT_RATING);
                refreshRating(rating);
            } else {
                ((K9) getApplicationContext()).getPlanckProvider()
                        .incomingMessageRating(mMessage, new PlanckProvider.SimpleResultCallback<Rating>() {
                            @Override
                            public void onLoaded(Rating rating) {
                                refreshRating(rating);
                            }
                        });
            }
        }
    }

    private void refreshRating(Rating rating) {
        pEpRating = rating;
        setToolbar();
        mMessage.setPlanckRating(mAccount.isPlanckPrivacyProtected() ? pEpRating : pEpRatingUndefined);
        mMessageView.setHeaders(mMessage, mAccount);
    }

    private void setToolbar() {
        if (isAdded()) {
            if (K9.isUsingTrustwords()) {
                planckSecurityStatusLayout.setOnClickListener(view -> onPEpPrivacyStatus(false));
            }
            planckSecurityStatusLayout.setRating(mAccount.isPlanckPrivacyProtected() ? pEpRating : pEpRatingUndefined);
            toolBarCustomizer.setMessageToolbarColor();
            toolBarCustomizer.setMessageStatusBarColor();
        }
    }

    private void showUnableToDecodeError() {
        FeedbackTools.showShortFeedback(getView(), getString(R.string.message_view_toast_unable_to_display_message));
    }

    private void showMessage(MessageViewInfo messageViewInfo, boolean shouldStopProgressDialog) {
        KeyboardUtils.hideKeyboard(getActivity());
        boolean handledByCryptoPresenter = messageCryptoPresenter.maybeHandleShowMessage(
                mMessageView, mAccount, messageViewInfo);
        if (!handledByCryptoPresenter) {
            if (messageViewInfo.message.isSet(Flag.DELETED)) {
                requireActivity().onBackPressed();
                FeedbackTools.showLongFeedback(requireView(), getString(R.string.message_view_message_no_longer_available));
            } else {
                mMessageView.showMessage(mAccount, messageViewInfo, shouldStopProgressDialog);
            }
            /*if (mAccount.isOpenPgpProviderConfigured()) {
                mMessageView.getMessageHeaderView().setCryptoStatusDisabled();
            } else {
                mMessageView.getMessageHeaderView().hideCryptoStatus();
            }*/
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
        mMessageView.getMessageHeaderView().onShowAdditionalHeaders(false);
    }

    public boolean allHeadersVisible() {
        return mMessageView.getMessageHeaderView().additionalHeadersVisible();
    }

    private void delete() {
        if (mMessage != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mFragmentListener.disableDeleteAction();
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
            mFragmentListener.onReply(mMessage.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), PlanckUtils.extractRating(mMessage));
        }
    }

    public void onReplyAll() {
        if (mMessage != null) {
            mFragmentListener.onReplyAll(mMessage.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), PlanckUtils.extractRating(mMessage));
        }
    }

    public void onForward() {
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), PlanckUtils.extractRating(mMessage));
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

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.getFolderName());
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference.toIdentityString());
        requireActivity().startActivityForResult(intent, activity);
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


    public void onPrintMessage() {
        Print printMessage = new PrintMessage(
                requireContext(),
                permissionChecker,
                mMessageView.getCurrentAttachmentResolver(),
                mMessageView.getCurrentAttachments(),
                mMessage,
                mMessageView.toHtml());
        printMessage.print();
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
                long size = currentAttachmentViewInfo.size;
                fragment = AttachmentDownloadDialogFragment.newInstance(size, message);
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
            fragment.dismissAllowingStateLoss();
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

    public boolean isMessageFlagged() {
        return (mMessage != null) && mMessage.isSet(Flag.FLAGGED);
    }

    public boolean isMessageRead() {
        return (mMessage != null) && mMessage.isSet(Flag.SEEN);
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
        handler.post(() -> {
            removeDialog(R.id.dialog_attachment_progress);
            // mMessageView.enableAttachmentButtons();
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

    private void refreshRecipients(Context context) {
        ArrayList<Identity> addresses = new ArrayList<>();
        addresses.addAll(PlanckUtils.createIdentities(Arrays.asList(mMessage.getFrom()), context));
        addresses.addAll(PlanckUtils.createIdentities(Arrays.asList(mMessage.getRecipients(Message.RecipientType.TO)), context));
        addresses.addAll(PlanckUtils.createIdentities(Arrays.asList(mMessage.getRecipients(Message.RecipientType.CC)), context));
        planckUIArtefactCache.setRecipients(mAccount, addresses);
    }

    public void onPEpPrivacyStatus(boolean force) {
        refreshRecipients(getContext());
        if (force || PlanckUtils.isPepStatusClickable(planckUIArtefactCache.getRecipients(), pEpRating)) {
            String myAddress = mAccount.getEmail();
            PlanckStatus.actionShowStatus(getActivity(), mMessage.getFrom()[0].getAddress(), getMessageReference(), true, myAddress);
        }
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

        void refreshMessageViewFragment();
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    private MessageLoaderDecryptCallbacks messageLoaderDecryptCallbacks = new MessageLoaderDecryptCallbacks() {

        @Override
        public void onMessageDecrypted() {
            refreshMessage();
        }

        @Override
        public void onMessageDataDecryptFailed(String errorMessage) {
            if (errorMessage.equals(PlanckProvider.KEY_MISSING_ERROR_MESSAGE)) {
                showKeyNotFoundFeedback();
            } else {
                showGenericErrorFeedback();
            }
        }
    };

    private MessageLoaderCallbacks messageLoaderCallbacks = new MessageLoaderCallbacks() {
        @Override
        public void onMessageDataLoadFinished(LocalMessage message) {
            mMessage = message;

            displayHeaderForLoadingMessage(message);
            recoverRating(message);
            ((MessageList) getActivity()).setMessageViewVisible(true);

            if (!mAccount.isPlanckPrivacyProtected()) {
                pEpRating = pEpRatingUndefined;
            }

            boolean shouldStopProgressDialog = !messageLoaderHelper.hasToBeDecrypted(mMessage);
            if (shouldStopProgressDialog)
                mMessageView.displayViewOnLoadFinished(true);

            setToolbar();
        }

        @Override
        public void onMessageDataLoadFailed() {
            FeedbackTools.showLongFeedback(getView(), getString(R.string.status_loading_error));
        }

        @Override
        public void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo) {
            //At this point MessageTopView is ready, but the message may be going through decryption
            boolean shouldStopProgressDialog = !messageLoaderHelper.hasToBeDecrypted(mMessage);
            showMessage(messageViewInfo, shouldStopProgressDialog);

        }

        @Override
        public void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo) {
            //At this point MessageTopView is ready, but the message may be going through decryption
            boolean shouldStopProgressDialog = !messageLoaderHelper.hasToBeDecrypted(mMessage);
            showMessage(messageViewInfo, shouldStopProgressDialog);
        }

        @Override
        public void setLoadingProgress(int current, int max) {
            mMessageView.setLoadingProgress(current, max);
        }

        @Override
        public void onDownloadErrorMessageNotFound() {
            runOnMainThread(() -> {
                        mMessageView.enableDownloadButton();
                        FeedbackTools.showLongFeedback(getView(), getString(R.string.status_invalid_id_error));
                    }
            );
        }

        @Override
        public void onDownloadErrorNetworkError() {
            runOnMainThread(() -> {
                        mMessageView.enableDownloadButton();
                        FeedbackTools.showLongFeedback(getView(), getString(R.string.status_network_error));
                    }
            );
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

    private void recoverRating(LocalMessage message) {
        // recover pEpRating from db, if is null,
        // then we take the one in the header and store it
        pEpRating = message.getPlanckRating();
        if (pEpRating == null) {
            pEpRating = PlanckUtils.extractRating(message);
            message.setPlanckRating(pEpRating);
        }
    }

    private void showKeyNotFoundFeedback() {
        mMessageView.setToErrorState(
                planckUIArtefactCache.getTitle(Rating.pEpRatingHaveNoKey),
                planckUIArtefactCache.getSuggestion(Rating.pEpRatingHaveNoKey)
        );
    }
    private void showGenericErrorFeedback() {
        mMessageView.setToErrorState(
                planckUIArtefactCache.getTitle(Rating.pEpRatingCannotDecrypt),
                planckUIArtefactCache.getExplanation(Rating.pEpRatingCannotDecrypt)
        );
    }

    private boolean canDecrypt() {
        return pEpRating.value != Rating.pEpRatingCannotDecrypt.value;
    }

    private void refreshMessage() {
        //If get support manager is null, it means that you don't have a fragment to refresh
        if (mFragmentListener != null) {
            mFragmentListener.refreshMessageViewFragment();
        }
    }

    @Override
    public void onViewAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first
        currentAttachmentViewInfo = attachment;
        getAttachmentController(attachment).viewAttachment();
    }

    @Override
    public void onSaveAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first
        currentAttachmentViewInfo = attachment;
        getAttachmentController(attachment).saveAttachment();
    }

    private AttachmentController getAttachmentController(AttachmentViewInfo attachment) {
        return new AttachmentController(mController, downloadManager, this, attachment);
    }
}