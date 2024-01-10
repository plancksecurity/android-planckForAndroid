package com.fsck.k9.ui.messageview;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.fsck.k9.ui.messageview.MessageViewEffect.ErrorDownloadingMessageNotFound;
import static com.fsck.k9.ui.messageview.MessageViewEffect.ErrorDownloadingNetworkError;
import static com.fsck.k9.ui.messageview.MessageViewEffect.ErrorLoadingMessage;
import static com.fsck.k9.ui.messageview.MessageViewEffect.MessageMovedToSuspiciousFolder;
import static com.fsck.k9.ui.messageview.MessageViewState.DecryptedMessageLoaded;
import static com.fsck.k9.ui.messageview.MessageViewState.EncryptedMessageLoaded;
import static com.fsck.k9.ui.messageview.MessageViewState.ErrorDecodingMessage;
import static com.fsck.k9.ui.messageview.MessageViewState.ErrorDecryptingMessage;
import static com.fsck.k9.ui.messageview.MessageViewState.ErrorDecryptingMessageKeyMissing;
import static com.fsck.k9.ui.messageview.MessageViewState.Loading;
import static com.fsck.k9.ui.messageview.MessageViewState.MessageDecoded;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.MessageList;
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
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.infrastructure.MessageView;
import com.fsck.k9.planck.infrastructure.extensions.ContextKt;
import com.fsck.k9.planck.infrastructure.livedata.Event;
import com.fsck.k9.planck.ui.infrastructure.DrawerLocker;
import com.fsck.k9.planck.ui.listeners.OnMessageOptionsListener;
import com.fsck.k9.planck.ui.listeners.SimpleRecipientHandshakeClickListener;
import com.fsck.k9.planck.ui.tools.FeedbackTools;
import com.fsck.k9.planck.ui.tools.KeyboardUtils;
import com.fsck.k9.ui.messageview.CryptoInfoDialog.OnClickShowCryptoKeyListener;
import com.fsck.k9.ui.messageview.MessageCryptoPresenter.MessageCryptoMvpView;
import com.fsck.k9.view.MessageCryptoDisplayStatus;
import com.fsck.k9.view.MessageHeader;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;
import kotlin.ExceptionsKt;
import security.planck.permissions.PermissionChecker;
import security.planck.permissions.PermissionRequester;
import security.planck.print.Print;
import security.planck.print.PrintMessage;
import security.planck.ui.message_compose.PlanckFabMenu;
import security.planck.ui.toolbar.PlanckSecurityStatusLayout;
import security.planck.ui.toolbar.ToolBarCustomizer;
import security.planck.ui.verifypartner.VerifyPartnerFragment;
import security.planck.ui.verifypartner.VerifyPartnerFragmentKt;
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
    private static final int DANGEROUS_MESSAGE_MOVED_FEEDBACK_DURATION = 5000;
    private static final int DANGEROUS_MESSAGE_MOVED_FEEDBACK_MAX_LINES = 10;
    private static final int ERROR_DEBUG_FEEDBACK_MAX_LINES = 10;
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
    private MessagingController mController;
    private DownloadManager downloadManager;
    private Handler handler = new Handler();
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

    private final SimpleRecipientHandshakeClickListener simpleRecipientHandshakeClickListener =
            () -> onPEpPrivacyStatus();

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

    private boolean justStarted;
    private MessageViewViewModel viewModel;

    @Override
    public void onAttach(@NonNull Context context) {
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
        initializeVerifyPartnerResultListener();

        // This fragments adds options to the action bar
        setHasOptionsMenu(true);


        Context context = Globals.getContext();
        mController = MessagingController.getInstance(context);
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        messageCryptoPresenter = new MessageCryptoPresenter(savedInstanceState, messageCryptoMvpView);
        doOnMessageList(K9Activity::hideSearchView);
    }

    private void initializeVerifyPartnerResultListener() {
        getParentFragmentManager().setFragmentResultListener(
                VerifyPartnerFragment.REQUEST_KEY,
                this,
                (requestKey, result) -> {
                    if (requestKey.equals(VerifyPartnerFragment.REQUEST_KEY)) {
                        String ratingString = result.getString(VerifyPartnerFragment.RESULT_KEY_RATING);
                        if (ratingString != null) {
                            try {
                                Rating rating = Rating.valueOf(ratingString);
                                refreshRating(rating);
                            } catch (Exception ex) {
                                Timber.e(ex, "wrong rating");
                            }
                            displayMessage();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.message, container, false);

        Toolbar toolbar = ((MessageList) requireActivity()).getToolbar();
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
            viewModel.downloadCompleteMessage();
        });
        // onDownloadRemainder();;
        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        setMessageOptionsListener();
        setSimpleRecipientHandshakeClickListener();

        planckUIArtefactCache = PlanckUIArtefactCache.getInstance(getApplicationContext());
        viewModel = new ViewModelProvider(this).get(MessageViewViewModel.class);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String messageReferenceString = requireArguments().getString(ARG_REFERENCE);
        observeViewModel();
        if (savedInstanceState == null) {
            viewModel.initialize(messageReferenceString);
        }
        mMessageReference = viewModel.getMessageReference();
        Timber.d("MessageView displaying message %s", mMessageReference);
        mAccount = viewModel.getAccount();
    }

    private void observeViewModel() {
        viewModel.getMessageViewState().observe(getViewLifecycleOwner(), this::renderMessageViewState);
        viewModel.getMessageViewEffect().observe(getViewLifecycleOwner(), event -> {
            MessageViewEffect effect = event.getContentIfNotHandled();
            if (effect != null) {
                renderMessageViewEffect(effect);
            }
        });
        viewModel.getAllowHandshakeSender().observe(getViewLifecycleOwner(), this::allowOrDisallowSenderHandshake);
        viewModel.getFlaggedToggled().observe(getViewLifecycleOwner(), this::flaggedToggled);
        viewModel.getReadToggled().observe(getViewLifecycleOwner(), this::readToggled);
    }

    private void readToggled(Event<Boolean> event) {
        Boolean value = event.getContentIfNotHandled();
        if (value != null && value) {
            mMessageView.setHeaders(viewModel.getMessage(), mAccount);
            String subject = viewModel.getMessageSubject();
            displayMessageSubject(subject);
            mFragmentListener.updateMenu();
        }
    }

    private void flaggedToggled(Event<Boolean> event) {
        Boolean value = event.getContentIfNotHandled();
        if (value != null && value) {
            mMessageView.setHeaders(viewModel.getMessage(), mAccount);
        }
    }

    private void allowOrDisallowSenderHandshake(Event<Boolean> event) {
        Boolean value = event.getContentIfNotHandled();
        if (value != null) {
            if (value) {
                allowHandshakeWithSender();
            } else {
                disAllowHandshakeWithSender();
            }
        }
    }

    private void renderMessageViewState(MessageViewState messageViewState) {
        if (messageViewState.equals(Loading.INSTANCE)) {
            mMessageView.setToLoadingState();
        } else if (messageViewState.equals(ErrorDecryptingMessageKeyMissing.INSTANCE)) {
            showKeyNotFoundFeedback();
        } else if (messageViewState instanceof ErrorDecryptingMessage) {
            showGenericErrorFeedback(((ErrorDecryptingMessage) messageViewState).getThrowable());
        } else if (messageViewState instanceof ErrorDecodingMessage) {
            showMessage(((ErrorDecodingMessage) messageViewState).getInfo(), true);
        } else if (messageViewState instanceof EncryptedMessageLoaded) {
            encryptedMessageLoaded((EncryptedMessageLoaded) messageViewState);
        } else if (messageViewState instanceof DecryptedMessageLoaded) {
            decryptedMessageLoaded((DecryptedMessageLoaded) messageViewState);
        } else if (messageViewState instanceof MessageViewState.MessageDecoded) {
            showMessage(((MessageDecoded) messageViewState).getInfo(), true);
        }
    }

    private void renderMessageViewEffect(MessageViewEffect effect) {
        if (effect instanceof ErrorLoadingMessage) {
            showMessageLoadErrorFeedback((ErrorLoadingMessage) effect);
        } else if (effect instanceof ErrorDownloadingMessageNotFound) {
            showDownloadMessageNotFound((ErrorDownloadingMessageNotFound) effect);
        } else if (effect instanceof ErrorDownloadingNetworkError) {
            showDownloadMessageNetworkError((ErrorDownloadingNetworkError) effect);
        } else if (effect.equals(MessageMovedToSuspiciousFolder.INSTANCE)) {
            messageMovedToSuspiciousFolder();
        }
    }

    private void messageMovedToSuspiciousFolder() {
        FeedbackTools.showLongFeedback(
                getRootView(), getString(R.string.dangerous_message_moved_to_suspicious_folder),
                DANGEROUS_MESSAGE_MOVED_FEEDBACK_DURATION,
                DANGEROUS_MESSAGE_MOVED_FEEDBACK_MAX_LINES
        );
        mFragmentListener.goBack();
    }

    private void encryptedMessageLoaded(EncryptedMessageLoaded messageViewState) {
        messageLoaded(messageViewState.getMessage());
        setToolbar();
    }

    private void messageLoaded(LocalMessage message) {
        displayHeaderForLoadingMessage(message);
        recoverRating(message);
        ((MessageList) requireActivity()).setMessageViewVisible(true);
    }

    private void decryptedMessageLoaded(DecryptedMessageLoaded messageViewState) {
        messageLoaded(messageViewState.getMessage());
        setToolbar();
        mFragmentListener.updateMenu();
        mMessageView.displayViewOnLoadFinished(true);
    }

    private void showDownloadMessageNetworkError(ErrorDownloadingNetworkError messageViewState) {
        mMessageView.enableDownloadButton();
        FeedbackTools.showLongFeedback(
                getView(),
                BuildConfig.DEBUG
                        ? ExceptionsKt.stackTraceToString(messageViewState.getThrowable())
                        : getString(R.string.status_network_error),
                Snackbar.LENGTH_LONG,
                ERROR_DEBUG_FEEDBACK_MAX_LINES
        );
    }

    private void showDownloadMessageNotFound(ErrorDownloadingMessageNotFound messageViewState) {
        mMessageView.enableDownloadButton();
        FeedbackTools.showLongFeedback(
                getView(),
                BuildConfig.DEBUG
                        ? ExceptionsKt.stackTraceToString(messageViewState.getThrowable())
                        : getString(R.string.status_invalid_id_error),
                Snackbar.LENGTH_LONG,
                ERROR_DEBUG_FEEDBACK_MAX_LINES
        );
    }

    private void showMessageLoadErrorFeedback(ErrorLoadingMessage messageViewState) {
        String errorText = "";
        if (BuildConfig.DEBUG) {
            errorText += "Message loading error\n";
            Throwable throwable = messageViewState.getThrowable();
            if (throwable != null) {
                errorText += ExceptionsKt.stackTraceToString(throwable);
            }
        } else {
            errorText = getString(R.string.status_loading_error);
        }
        FeedbackTools.showLongFeedback(
                getView(),
                errorText,
                Snackbar.LENGTH_LONG,
                ERROR_DEBUG_FEEDBACK_MAX_LINES
        );
        if (messageViewState.getClose()) {
            requireActivity().onBackPressed();
        }
    }

    @Override
    public void onDestroyView() {
        if (planckSecurityStatusLayout != null) {
            planckSecurityStatusLayout.hideRating();
        }
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        justStarted = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (justStarted) { // TODO: 30/11/23 To remove this workaround we may need to first remove the ViewAnimator in MessageList
            justStarted = false;
            ((MessageList) requireActivity()).setMessageViewVisible(true);
            setupSwipeDetector();
            ((DrawerLocker) requireActivity()).setDrawerEnabled(false);
            displayMessage();
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
        planckSecurityStatusLayout.setOnClickListener(null);
        doOnActivity(activity -> activity.setResult(RESULT_CANCELED));
    }

    public boolean shouldDisplayResetSenderKeyOption() {
        return viewModel.canResetSenderKeys();
    }

    public void resetSenderKey() {
        if (shouldDisplayResetSenderKeyOption()) {
            ResetPartnerKeyDialog.showResetPartnerKeyDialog(this);
        }
    }

    private void setupSwipeDetector() {
        doOnMessageList(messageList -> messageList.setupGestureDetector(MessageViewFragment.this));
    }

    @Override
    public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        doOnMessageList(MessageList::showNextMessage);
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        doOnMessageList(MessageList::showPreviousMessage);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        messageCryptoPresenter.onSaveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    private void setMessageOptionsListener() {
        mMessageView.getMessageHeader().setOnMessageOptionsListener(messageOptionsListener);
        pEpFabMenu.setClickListeners(messageOptionsListener);
    }

    private void setSimpleRecipientHandshakeClickListener() {
        mMessageView.getMessageHeader().setSimpleRecipientHandshakeClickListener(
                simpleRecipientHandshakeClickListener);
    }

    public void displayMessage() {
        mMessageView.getMessageHeader().hideSingleRecipientHandshakeBanner();
        mInitialized = true;
        viewModel.loadMessage();
        mFragmentListener.updateMenu();
    }

    @Override
    public void onStop() {
        ((MessageList) ContextKt.getRootContext(requireActivity())).removeGestureDetector();
        planckSecurityStatusLayout.setVisibility(View.GONE);
        super.onStop();
    }

    public void onPendingIntentResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode & REQUEST_MASK_LOADER_HELPER) == REQUEST_MASK_LOADER_HELPER) {
            //requestCode ^= REQUEST_MASK_LOADER_HELPER;
            //if (messageLoaderHelper != null) {
            //    messageLoaderHelper.onActivityResult(requestCode, resultCode, data);
            //}
            return;
        }

        if ((requestCode & REQUEST_MASK_CRYPTO_PRESENTER) == REQUEST_MASK_CRYPTO_PRESENTER) {
            requestCode ^= REQUEST_MASK_CRYPTO_PRESENTER;
            messageCryptoPresenter.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void refreshRating(Rating rating) {
        pEpRating = rating;
        setToolbar();
        mMessageView.setHeaders(viewModel.getMessage(), mAccount);
    }

    private void setToolbar() {
        if (isAdded()) {
            planckSecurityStatusLayout.setIncomingRating(
                    pEpRating,
                    !mAccount.isPlanckPrivacyProtected()
                            || viewModel.isMessageSMime()
            );
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
        if (K9.confirmDelete() || (K9.confirmDeleteStarred() && viewModel.isMessageFlagged())) {
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
        if (viewModelHasMessage()) {
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
        if (viewModelHasMessage()) {
            mFragmentListener.onReply(viewModel.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), viewModel.extractMessageRating());
        }
    }

    public void onReplyAll() {
        if (viewModelHasMessage()) {
            mFragmentListener.onReplyAll(viewModel.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), viewModel.extractMessageRating());
        }
    }

    public void onForward() {
        if (viewModelHasMessage()) {
            mFragmentListener.onForward(viewModel.makeMessageReference(), messageCryptoPresenter.getDecryptionResultForReply(), viewModel.extractMessageRating());
        }
    }

    public void onToggleFlagged() {
        viewModel.toggleFlagged();
    }

    public void onMove() {
        if ((!mController.isMoveCapable(mAccount))
                || (!viewModelHasMessage())) {
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
                || (!viewModelHasMessage())) {
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

    private void startRefileActivity(int which) {
        doOnActivity(activity -> {
            Intent intent = new Intent(activity, ChooseFolder.class);
            intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
            intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.getFolderName());
            intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
            intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference.toIdentityString());
            requireActivity().startActivityForResult(intent, which);
        });
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
        if (viewModelHasMessage()) {
            doOnActivity(activity -> mController.sendAlternate(activity, mAccount, viewModel.getMessage()));
        }
    }


    public void onPrintMessage() {
        Print printMessage = new PrintMessage(
                requireContext(),
                permissionChecker,
                mMessageView.getCurrentAttachmentResolver(),
                mMessageView.getCurrentAttachments(),
                viewModel.getMessage(),
                mMessageView.toHtml());
        printMessage.print();
    }

    public void onToggleRead() {
        viewModel.toggleRead();
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
        return mMessageReference != null ? mMessageReference : MessageReference.parse(requireArguments().getString(ARG_REFERENCE));
    }

    public boolean isMessageFlagged() {
        return viewModel.isMessageFlagged();
    }

    public boolean isMessageRead() {
        return viewModel.isMessageRead();
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
        if (viewModelHasMessage()) {
            displayMessageSubject(viewModel.getMessageSubject());
        }
    }

    private boolean viewModelHasMessage() {
        return viewModel != null && viewModel.hasMessage();
    }

    public Context getApplicationContext() {
        return Globals.getContext();
    }

    public View getRootView() {
        MessageList activity = getMessageList();
        return activity != null ? activity.getRootView() : null;
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

    private final MessageCryptoMvpView messageCryptoMvpView = new MessageCryptoMvpView() {
        @Override
        public void redisplayMessage() {
            viewModel.loadMessage();
        }

        @Override
        public void startPendingIntentForCryptoPresenter(IntentSender si, Integer requestCode, Intent fillIntent,
                                                         int flagsMask, int flagValues, int extraFlags) throws SendIntentException {
            Activity activity = getActivity();
            if (activity == null) return;
            if (requestCode == null) {
                activity.startIntentSender(si, fillIntent, flagsMask, flagValues, extraFlags);
                return;
            }

            requestCode |= REQUEST_MASK_CRYPTO_PRESENTER;
            activity.startIntentSenderForResult(
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
            //if (messageLoaderHelper != null) { // not needed as the account has no PgpProvider set
            //    mMessageView.setToLoadingState();
            //    messageLoaderHelper.asyncRestartMessageCryptoProcessing();
            //}
        }
    };

    @Override
    public void onClickShowCryptoKey() {
        messageCryptoPresenter.onClickShowCryptoKey();
    }

    private void refreshRecipients(Context context) {
        ArrayList<Identity> addresses = new ArrayList<>();
        addresses.addAll(PlanckUtils.createIdentities(Arrays.asList(viewModel.getMessageFrom()), context));
        addresses.addAll(PlanckUtils.createIdentities(Arrays.asList(viewModel.getMessageRecipients(Message.RecipientType.TO)), context));
        addresses.addAll(PlanckUtils.createIdentities(Arrays.asList(viewModel.getMessageRecipients(Message.RecipientType.CC)), context));
        planckUIArtefactCache.setRecipients(mAccount, addresses);
    }

    public void onPEpPrivacyStatus() {
        refreshRecipients(getContext());
        if (viewModel.isMessageValidForHandshake()) {
            String myAddress = mAccount.getEmail();
            VerifyPartnerFragmentKt.showVerifyPartnerDialog(this, viewModel.getMessageFrom()[0].getAddress(), myAddress, getMessageReference(), true);
        }
    }

    private void allowHandshakeWithSender() {
        if (isAdded()) {
            planckSecurityStatusLayout.setOnClickListener(view -> onPEpPrivacyStatus());
            mMessageView.getMessageHeader().showSingleRecipientHandshakeBanner();
        }
    }

    private void disAllowHandshakeWithSender() {
        if (isAdded()) {
            planckSecurityStatusLayout.setOnClickListener(null);
            mMessageView.getMessageHeader().hideSingleRecipientHandshakeBanner();
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

        void goBack();

        void messageHeaderViewAvailable(MessageHeader messageHeaderView);

        void updateMenu();

        void refreshMessageViewFragment();
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    private void recoverRating(LocalMessage message) {
        // recover pEpRating from db, if is null,
        // then we take the one in the header and store it
        pEpRating = message.getPlanckRating();
    }

    private void showKeyNotFoundFeedback() {
        mMessageView.setToErrorState(
                planckUIArtefactCache.getTitle(Rating.pEpRatingHaveNoKey),
                planckUIArtefactCache.getSuggestion(Rating.pEpRatingHaveNoKey)
        );
    }

    private void showGenericErrorFeedback(Throwable throwable) {
        mMessageView.setToErrorState(
                planckUIArtefactCache.getTitle(Rating.pEpRatingCannotDecrypt),
                throwable != null
                        ? ExceptionsKt.stackTraceToString(throwable)
                        : planckUIArtefactCache.getExplanation(Rating.pEpRatingCannotDecrypt)
        );
    }

    private boolean canDecrypt() {
        return pEpRating.value != Rating.pEpRatingCannotDecrypt.value;
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

    private MessageList getMessageList() {
        return (MessageList) getActivity();
    }

    private void doOnActivity(Consumer<Activity> block) {
        Activity activity = getActivity();
        if (activity != null) {
            block.accept(activity);
        }
    }

    private void doOnMessageList(Consumer<MessageList> block) {
        MessageList activity = (MessageList) getActivity();
        if (activity != null) {
            block.accept(activity);
        }
    }
}