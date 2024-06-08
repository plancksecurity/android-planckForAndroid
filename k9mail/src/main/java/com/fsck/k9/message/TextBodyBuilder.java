package com.fsck.k9.message;


import android.text.TextUtils;
import timber.log.Timber;

import com.fsck.k9.Globals;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.message.html.HtmlConverter;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.quote.InsertableHtmlContent;


class TextBodyBuilder {
    private static final String PLANCK = "Planck";
    private static final String PLANCK_LINK = "<a href=\"https://www.planck.security/get-started?utm_source=android&utm_medium=plg&utm_campaign=sign\">Planck</a>";
    private boolean mIncludeQuotedText = true;
    private boolean mReplyAfterQuote = false;
    private boolean mSignatureBeforeQuotedText = false;
    private boolean mInsertSeparator = false;
    private boolean mAppendSignature = true;

    private final String mMessageContent;
    private String mSignature;
    private String mQuotedText;
    private InsertableHtmlContent mQuotedTextHtml;
    private final boolean isHtml;

    public TextBodyBuilder(String messageContent) {
        mMessageContent = messageContent;
        isHtml = false;
    }

    public TextBodyBuilder(String messageContent, boolean isHtml) {
        mMessageContent = messageContent;
        this.isHtml = isHtml;
    }

    /**
     * Build the {@link Body} that will contain the text of the message.
     *
     * @return {@link com.fsck.k9.mail.internet.TextBody} instance that contains the entered text and
     *         possibly the quoted original message.
     */
    public TextBody buildTextHtml() {
        // The length of the formatted version of the user-supplied text/reply
        int composedMessageLength;

        // The offset of the user-supplied text/reply in the final text body
        int composedMessageOffset;

        // Get the user-supplied text
        String text = mMessageContent;

        String signature = "";

        // Do we have to modify an existing message to include our reply?
        if (mIncludeQuotedText) {
            InsertableHtmlContent quotedHtmlContent = getQuotedTextHtml();

            if (K9.isDebug()) {
                Timber.d("insertable: %s", quotedHtmlContent.toDebugString());
            }

            if (mAppendSignature) {
                // Append signature to the reply
                if (mReplyAfterQuote || mSignatureBeforeQuotedText) {
                    signature = getSignatureHtml();
                }
            }

            // Convert the text to HTML
            text = textToHtmlFragment(text) + signature;

            /*
             * Set the insertion location based upon our reply after quote
             * setting. Additionally, add some extra separators between the
             * composed message and quoted message depending on the quote
             * location. We only add the extra separators when we're
             * sending, that way when we load a draft, we don't have to know
             * the length of the separators to remove them before editing.
             */
            if (mReplyAfterQuote) {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.AFTER_QUOTE);
                if (mInsertSeparator) {
                    text = "<br clear=\"all\">" + text;
                }
            } else {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.BEFORE_QUOTE);
                if (mInsertSeparator) {
                    text += "<br><br>";
                }
            }

            if (mAppendSignature) {
                // Place signature immediately after the quoted text
                if (!(mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                    quotedHtmlContent.insertIntoQuotedFooter(getSignatureHtml());
                }
            }

            quotedHtmlContent.setUserContent(text);

            // Save length of the body and its offset.  This is used when thawing drafts.
            composedMessageLength = text.length();
            composedMessageOffset = quotedHtmlContent.getInsertionPoint();
            text = quotedHtmlContent.toString();

        } else {
            // There is no text to quote so simply append the signature if available
            if (mAppendSignature) {
                signature = getSignatureHtml();
            }

            // Convert the text to HTML
            text = textToHtmlFragment(text) + signature;

            //TODO: Wrap this in proper HTML tags

            composedMessageLength = text.length();
            composedMessageOffset = 0;
        }

        TextBody body = new TextBody(text);
        body.setComposedMessageLength(composedMessageLength);
        body.setComposedMessageOffset(composedMessageOffset);

        return body;
    }

    /**
     * Build the {@link Body} that will contain the text of the message.
     *
     * @return {@link TextBody} instance that contains the entered text and
     *         possibly the quoted original message.
     */
    public TextBody buildTextPlain() {
        // The length of the formatted version of the user-supplied text/reply
        int composedMessageLength;

        // The offset of the user-supplied text/reply in the final text body
        int composedMessageOffset;

        // Get the user-supplied text
        String text = htmlFragmentToText(mMessageContent);

        // Capture composed message length before we start attaching quoted parts and signatures.
        composedMessageLength = text.length();
        composedMessageOffset = 0;

        // Do we have to modify an existing message to include our reply?
        if (mIncludeQuotedText) {
            String quotedText = getQuotedText();

            if (mAppendSignature) {
                // Append signature to the text/reply
                if (mReplyAfterQuote || mSignatureBeforeQuotedText) {
                    text += getSignature();
                }
            }

            if (mReplyAfterQuote) {
                composedMessageOffset = quotedText.length() + "\r\n".length();
                text = quotedText + "\r\n" + text;
            } else {
                text += "\r\n\r\n" + quotedText;
            }

            if (mAppendSignature) {
                // Place signature immediately after the quoted text
                if (!(mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                    text += getSignature();
                }
            }
        } else {
            // There is no text to quote so simply append the signature if available
            if (mAppendSignature) {
                // Append signature to the text/reply
                text += getSignature();
            }
        }

        TextBody body = new TextBody(text);
        body.setComposedMessageLength(composedMessageLength);
        body.setComposedMessageOffset(composedMessageOffset);

        return body;
    }

    private String getSignature() {
        String signature = "";
        if (!TextUtils.isEmpty(mSignature)) {
            signature = "\r\n\r\n" + mSignature;
        }

        return signature;
    }

    private String getSignatureHtml() {
        String signature = "";
        if (mSignature.equals(Globals.getContext().getString(R.string.default_signature))) {
            signature = "<br><br>" + mSignature.replace(PLANCK, PLANCK_LINK);
        } else if (!TextUtils.isEmpty(mSignature)) {
            signature = textToHtmlFragment("\r\n\r\n" + mSignature);
        }
        return signature;
    }

    private String getQuotedText() {
        String quotedText = "";
        if (!TextUtils.isEmpty(mQuotedText)) {
            quotedText = mQuotedText;
        }
        return quotedText;
    }

    private InsertableHtmlContent getQuotedTextHtml() {
        return mQuotedTextHtml;
    }

    /**
     * protected for unit-test purposes
     */
    protected String textToHtmlFragment(String text) {
        if (isHtml) return text;
        return HtmlConverter.textToHtmlFragment(text);
    }

    protected String htmlFragmentToText(String text) {
        if (!isHtml) return text;
        return HtmlConverter.htmlToText(text);
    }

    public void setSignature(String signature) {
        mSignature = signature;
    }

    public void setIncludeQuotedText(boolean includeQuotedText) {
        mIncludeQuotedText = includeQuotedText;
    }

    public void setQuotedText(String quotedText) {
        mQuotedText = quotedText;
    }

    public void setQuotedTextHtml(InsertableHtmlContent quotedTextHtml) {
        mQuotedTextHtml = quotedTextHtml;
    }

    public void setInsertSeparator(boolean insertSeparator) {
        mInsertSeparator = insertSeparator;
    }

    public void setSignatureBeforeQuotedText(boolean signatureBeforeQuotedText) {
        mSignatureBeforeQuotedText = signatureBeforeQuotedText;
    }

    public void setReplyAfterQuote(boolean replyAfterQuote) {
        mReplyAfterQuote = replyAfterQuote;
    }

    public void setAppendSignature(boolean appendSignature) {
        mAppendSignature = appendSignature;
    }
}
