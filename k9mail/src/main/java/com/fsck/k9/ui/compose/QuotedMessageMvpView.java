package com.fsck.k9.ui.compose;


import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;

import com.fsck.k9.FontSizes;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.mailstore.AttachmentResolver;
import com.fsck.k9.message.QuotedTextMode;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.ui.EolConvertingEditText;
import com.fsck.k9.ui.ImageRefusingEditText;
import com.fsck.k9.view.MessageWebView;


public class QuotedMessageMvpView {
    private final Button mQuotedTextShow;
    private final View mQuotedTextBar;
    private final ImageButton mQuotedTextEdit;
    private final EolConvertingEditText mQuotedText;
    private final MessageWebView mQuotedHTML;
    private final ImageRefusingEditText mMessageContentView;
    private final ImageButton mQuotedTextDelete;
    private final DisplayHtml displayHtml;

    public QuotedMessageMvpView(MessageCompose messageCompose, DisplayHtml displayHtml) {
        mQuotedTextShow = (Button) messageCompose.findViewById(R.id.quoted_text_show);
        mQuotedTextBar = messageCompose.findViewById(R.id.quoted_text_bar);
        mQuotedTextEdit = (ImageButton) messageCompose.findViewById(R.id.quoted_text_edit);
        mQuotedTextDelete = (ImageButton) messageCompose.findViewById(R.id.quoted_text_delete);
        mQuotedText = (EolConvertingEditText) messageCompose.findViewById(R.id.quoted_text);
        mQuotedText.getInputExtras(true).putBoolean("allowEmoji", true);

        mQuotedHTML = (MessageWebView) messageCompose.findViewById(R.id.quoted_html);
        mQuotedHTML.refreshTheme();
        mQuotedHTML.configure();
        // Disable the ability to click links in the quoted HTML page. I think this is a nice feature, but if someone
        // feels this should be a preference (or should go away all together), I'm ok with that too. -achen 20101130
        mQuotedHTML.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });

        mMessageContentView = messageCompose.findViewById(R.id.message_content);
        this.displayHtml = displayHtml;
    }

    public void setOnClickPresenter(final QuotedMessagePresenter presenter) {
        mQuotedTextShow.setOnClickListener(v ->presenter.onClickShowQuotedText());
        mQuotedTextEdit.setOnClickListener(v -> presenter.onClickEditQuotedText());
        mQuotedTextDelete.setOnClickListener(v -> presenter.onClickDeleteQuotedText());
    }

    public void addTextChangedListener(TextWatcher draftNeedsChangingTextWatcher) {
        mQuotedText.addTextChangedListener(draftNeedsChangingTextWatcher);
    }

    public void showOrHideQuotedText(QuotedTextMode mode, SimpleMessageFormat quotedTextFormat) {
        switch (mode) {
            case NONE: {
                mQuotedTextShow.setVisibility(View.GONE);
                mQuotedTextBar.setVisibility(View.GONE);
                mQuotedText.setVisibility(View.GONE);
                mQuotedHTML.setVisibility(View.GONE);
                mQuotedTextEdit.setVisibility(View.GONE);
                mQuotedTextDelete.setVisibility(View.GONE);
                break;
            }
            case HIDE: {
                mQuotedTextShow.setVisibility(View.VISIBLE);
                mQuotedTextBar.setVisibility(View.GONE);
                mQuotedText.setVisibility(View.GONE);
                mQuotedHTML.setVisibility(View.GONE);
                mQuotedTextEdit.setVisibility(View.GONE);
                mQuotedTextDelete.setVisibility(View.GONE);
                break;
            }
            case SHOW: {
                mQuotedTextShow.setVisibility(View.GONE);
                mQuotedTextBar.setVisibility(View.VISIBLE);

                if (quotedTextFormat == SimpleMessageFormat.HTML) {
                    mQuotedText.setVisibility(View.GONE);
                    mQuotedHTML.setVisibility(View.VISIBLE);
                    mQuotedTextEdit.setVisibility(View.VISIBLE);
                    mQuotedTextDelete.setVisibility(View.VISIBLE);
                } else {
                    mQuotedText.setVisibility(View.VISIBLE);
                    mQuotedHTML.setVisibility(View.GONE);
                    mQuotedTextEdit.setVisibility(View.GONE);
                    mQuotedTextDelete.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    public void setFontSizes(FontSizes mFontSizes, int fontSize) {
        mFontSizes.setViewTextSize(mQuotedText, fontSize);
    }

    public String getQuotedText() {
        return mQuotedText.getCharacters();
    }

    public void setQuotedHtml(String quotedContent, AttachmentResolver attachmentResolver) {
        mQuotedHTML.displayHtmlContentWithInlineAttachments(
                displayHtml.wrapMessageContent(quotedContent),
                attachmentResolver, null);
    }

    public void setQuotedText(String quotedText) {
        mQuotedText.setCharacters(quotedText);
    }

    public void setMessageContentCharacters(String text) {
        mMessageContentView.setCharacters(text);
    }

    public void setMessageContentCursorPosition(int messageContentCursorPosition) {
        mMessageContentView.setSelection(messageContentCursorPosition);
    }
}
