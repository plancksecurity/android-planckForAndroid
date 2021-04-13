package com.fsck.k9.ui.messageview;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.ShowPictures;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.AttachmentResolver;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.NonLockingScrollView;
import com.fsck.k9.view.ToolableViewAnimator;

import java.util.Map;
public class MessageTopView extends RelativeLayout {

    public static final int PROGRESS_MAX = 1000;
    public static final int PROGRESS_MAX_WITH_MARGIN = 950;
    public static final int PROGRESS_STEP_DURATION = 180;


    private ToolableViewAnimator viewAnimator;
    private ProgressBar progressBar;
    private TextView progressText;

    private TextView errorTitle;
    private TextView errorText;

    private MessageHeader mHeaderContainer;
    private MessageContainerView containerView;
    private Button mDownloadRemainder;
    private AttachmentViewCallback attachmentCallback;
    private View showPicturesButton;
    private boolean isShowingProgress;
    private boolean messageLoaded = false;
    private NonLockingScrollView scrollView;

    private MessageCryptoPresenter messageCryptoPresenter;
    private SavedState savedState;

    public MessageTopView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageHeader getMessageHeader() {
        return mHeaderContainer;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState savedState = new SavedState(superState);

        savedState.scrollY = getScrollViewPercentage();
        return savedState;
    }

    private double getScrollViewPercentage() {
        double scrollViewHeight = scrollView.getChildAt(0).getBottom() - scrollView.getHeight();
        double getScrollY = scrollView.getScrollY();
        return (getScrollY / scrollViewHeight) * 100d;
    }

    private int getScrollFromPercentage(double percentage){
        double scrollViewHeight = scrollView.getChildAt(0).getBottom() - scrollView.getHeight();
        return (int) (scrollViewHeight * percentage / 100d);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());

        this.savedState = savedState;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        // mHeaderContainer.setOnLayoutChangedListener(this);

        viewAnimator = (ToolableViewAnimator) findViewById(R.id.message_layout_animator);
        progressBar = (ProgressBar) findViewById(R.id.message_progress);
        progressText = (TextView) findViewById(R.id.message_progress_text);

        errorTitle = (TextView) findViewById(R.id.error_title);
        errorText = (TextView) findViewById(R.id.error_message);

        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mDownloadRemainder.setVisibility(View.GONE);

        showPicturesButton = (View) findViewById(R.id.show_pictures);
        setShowPicturesButtonListener();

        containerView = findViewById(R.id.message_container);
        scrollView = findViewById(R.id.scrollview);
        hideHeaderView();
    }

    private void setShowPicturesButtonListener() {
        showPicturesButton.setOnClickListener(v -> showPicturesInAllContainerViews());
    }

    private void showPicturesInAllContainerViews() {
        containerView.showPictures();
        hideShowPicturesButton();
    }

    private void resetAndPrepareMessageView(MessageViewInfo messageViewInfo) {
        mDownloadRemainder.setVisibility(View.GONE);
        setShowDownloadButton(messageViewInfo);
    }

    public void showMessage(Account account, MessageViewInfo messageViewInfo, boolean shouldStopProgressDialog) {
        if (!messageLoaded) {
            messageLoaded = true;
            resetAndPrepareMessageView(messageViewInfo);

            ShowPictures showPicturesSetting = account.getShowPictures();
            boolean automaticallyLoadPictures =
                    shouldAutomaticallyLoadPictures(showPicturesSetting, messageViewInfo.message);

            boolean hideUnsignedTextDivider = account.getOpenPgpHideSignOnly();
            containerView.displayMessageViewContainer(
                    messageViewInfo,
                    () -> {
                        if (shouldStopProgressDialog) {
                            displayViewOnLoadFinished(true);
                        }
                        if (savedState != null) {
                            scrollView.postDelayed(() ->
                                            scrollView.scrollTo(0, getScrollFromPercentage(savedState.scrollY)),
                                    300);
                        }
                    },
                    automaticallyLoadPictures, hideUnsignedTextDivider, attachmentCallback);

            if (containerView.hasHiddenExternalImages()) {
                showShowPicturesButton();
            }
        }
    }

    /**
     * Fetch the message header view.  This is not the same as the message headers; this is the View shown at the top
     * of messages.
     * @return MessageHeader View.
     */
    public MessageHeader getMessageHeaderView() {
        return mHeaderContainer;
    }

    public void setHeaders(final Message message, Account account) {
        mHeaderContainer.populate(message, account);
        mHeaderContainer.setVisibility(View.VISIBLE);
    }

    public void showAllHeaders() {
        mHeaderContainer.onShowAdditionalHeaders(false);
    }

    public boolean additionalHeadersVisible() {
        return mHeaderContainer.additionalHeadersVisible();
    }

    private void hideHeaderView() {
        mHeaderContainer.setVisibility(View.GONE);
    }

    public void setOnDownloadButtonClickListener(OnClickListener listener) {
        mDownloadRemainder.setOnClickListener(listener);
    }

    public void setAttachmentCallback(AttachmentViewCallback callback) {
        attachmentCallback = callback;
    }

    public void setMessageCryptoPresenter(MessageCryptoPresenter messageCryptoPresenter) {
        this.messageCryptoPresenter = messageCryptoPresenter;
        mHeaderContainer.setOnCryptoClickListener(messageCryptoPresenter);
    }

    public void enableDownloadButton() {
        mDownloadRemainder.setEnabled(true);
    }

    public void disableDownloadButton() {
        mDownloadRemainder.setEnabled(false);
    }

    private void setShowDownloadButton(MessageViewInfo messageViewInfo) {
        if (messageViewInfo.isMessageIncomplete) {
            mDownloadRemainder.setEnabled(true);
            mDownloadRemainder.setVisibility(View.VISIBLE);
        } else {
            mDownloadRemainder.setVisibility(View.GONE);
        }
    }

    private void showShowPicturesButton() {
        showPicturesButton.setVisibility(View.VISIBLE);
    }

    private void hideShowPicturesButton() {
        showPicturesButton.setVisibility(View.GONE);
    }

    private boolean shouldAutomaticallyLoadPictures(ShowPictures showPicturesSetting, Message message) {
        return showPicturesSetting == ShowPictures.ALWAYS || shouldShowPicturesFromSender(showPicturesSetting, message);
    }

    private boolean shouldShowPicturesFromSender(ShowPictures showPicturesSetting, Message message) {
        if (showPicturesSetting != ShowPictures.ONLY_FROM_CONTACTS) {
            return false;
        }

        String senderEmailAddress = getSenderEmailAddress(message);
        if (senderEmailAddress == null) {
            return false;
        }

        Contacts contacts = Contacts.getInstance(getContext());
        return contacts.isInContacts(senderEmailAddress);
    }

    private String getSenderEmailAddress(Message message) {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) {
            return null;
        }

        return from[0].getAddress();
    }

    public void displayViewOnLoadFinished(boolean finishProgressBar) {
        if (!finishProgressBar || !isShowingProgress) {
            viewAnimator.setDisplayedChild(2);
            return;
        }

        ObjectAnimator animator = ObjectAnimator.ofInt(
                progressBar, "progress", progressBar.getProgress(), PROGRESS_MAX);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                viewAnimator.setDisplayedChild(2);
            }
        });
        animator.setDuration(PROGRESS_STEP_DURATION);
        animator.start();
    }

    public void setToLoadingState() {
        viewAnimator.setDisplayedChild(0);
        progressBar.setProgress(0);
        isShowingProgress = true;
    }

    public void setToErrorState(String title, String message) {
        errorTitle.setText(title);
        errorText.setText(message);
        viewAnimator.setDisplayedChild(3);
    }

    public void setLoadingProgress(int progress, int max) {
        if (!isShowingProgress) {
            viewAnimator.setDisplayedChild(1);
            isShowingProgress = true;
            return;
        }

        int newPosition = (int) (progress / (float) max * PROGRESS_MAX_WITH_MARGIN);
        int currentPosition = progressBar.getProgress();
        if (newPosition > currentPosition) {
            ObjectAnimator.ofInt(progressBar, "progress", currentPosition, newPosition)
                    .setDuration(PROGRESS_STEP_DURATION).start();
        } else {
            progressBar.setProgress(newPosition);
        }
    }

    public void setPrivacyProtected(boolean ispEpEnabled) {
        mHeaderContainer.setPrivacyProtected(ispEpEnabled);
    }

    static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        double scrollY;


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.scrollY = in.readDouble();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeDouble(this.scrollY);
        }
    }

    public String toHtml() {
        View messageContainerViewCandidate = containerView.getChildAt(0);
        if (messageContainerViewCandidate instanceof MessageContainerView) {
            return ((MessageContainerView) messageContainerViewCandidate).toHtml();
        }
        return null;
    }

    public AttachmentResolver getCurrentAttachmentResolver() {
        View messageContainerViewCandidate = containerView.getChildAt(0);
        if (messageContainerViewCandidate instanceof MessageContainerView) {
            return ((MessageContainerView) messageContainerViewCandidate).getCurrentAttachmentResolver();
        }
        return null;
    }


    public Map<Uri, AttachmentViewInfo> getCurrentAttachments() {
        View messageContainerViewCandidate = containerView.getChildAt(0);
        if (messageContainerViewCandidate instanceof MessageContainerView) {
            return ((MessageContainerView) messageContainerViewCandidate).getCurrentAttachments();
        }
        return null;
    }
}
