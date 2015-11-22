package com.fsck.k9.pEp;


import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.message.SimpleMessageFormat;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.pEp.jniadapter.Blob;

import java.util.Date;
import java.util.Locale;
import java.util.Vector;


public class PEpMessageBuilder {
    private String subject;
    private Address[] to;
    private Address[] cc;
    private Address[] bcc;
    private String inReplyTo;
    private String references;
    private boolean requestReadReceipt;
    private Identity identity;
    private SimpleMessageFormat messageFormat;
    private String text;

    private Vector<Blob> attachments;

//    private List<Attachment> attachments;

    //    private String signature;
//    private QuoteStyle quoteStyle;
//    private QuotedTextMode quotedTextMode;
//    private String quotedText;
//    private InsertableHtmlContent quotedHtmlContent;
//    private boolean isReplyAfterQuote;
//    private boolean isSignatureBeforeQuotedText;
    private boolean identityChanged;
    private boolean signatureChanged;
    //    private int cursorPosition;
    private MessageReference messageReference;
//    private boolean isDraft;


    public PEpMessageBuilder() {

    }

    /**
     * Build the final message to be sent (or saved). If there is another message quoted in this one, it will be baked
     * into the final message here.
     */
    public MimeMessage build() throws MessagingException {
        //FIXME: check arguments

        MimeMessage message = new MimeMessage();

        buildHeader(message);
        buildBody(message);

        return message;
    }

    private void buildHeader(MimeMessage message) throws MessagingException {
        message.addSentDate(new Date(), K9.hideTimeZone());
        Address from = new Address(identity.getEmail(), identity.getName());
        message.setFrom(from);
        message.setRecipients(RecipientType.TO, to);
        message.setRecipients(RecipientType.CC, cc);
        message.setRecipients(RecipientType.BCC, bcc);
        message.setSubject(subject);

        if (requestReadReceipt) {
            message.setHeader("Disposition-Notification-To", from.toEncodedString());
            message.setHeader("X-Confirm-Reading-To", from.toEncodedString());
            message.setHeader("Return-Receipt-To", from.toEncodedString());
        }

        if (!K9.hideUserAgent()) {       // if ctx not set, forget about user agent...
            message.setHeader("User-Agent", "K9/pEp early beta");
        }

        final String replyTo = identity.getReplyTo();
        if (replyTo != null) {
            message.setReplyTo(new Address[]{new Address(replyTo)});
        }

        if (inReplyTo != null) {
            message.setInReplyTo(inReplyTo);
        }

        if (references != null) {
            message.setReferences(references);
        }

        message.generateMessageId();
    }

    private void buildBody(MimeMessage message) throws MessagingException {
        // Build the body.
        // TODO FIXME - body can be either an HTML or Text part, depending on whether we're in
        // HTML mode or not.  Should probably fix this so we don't mix up html and text parts.
        TextBody body = buildText();

        // text/plain part when messageFormat == MessageFormat.HTML
        TextBody bodyPlain = null;

        if (messageFormat == SimpleMessageFormat.HTML) {
/*            // HTML message (with alternative text part)

            // This is the compiled MIME part for an HTML message.
            MimeMultipart composedMimeMessage = new MimeMultipart();
            composedMimeMessage.setSubType("alternative");   // Let the receiver select either the text or the HTML part.
            composedMimeMessage.addBodyPart(new MimeBodyPart(body, "text/html"));
            bodyPlain = buildText(isDraft, SimpleMessageFormat.TEXT);
            composedMimeMessage.addBodyPart(new MimeBodyPart(bodyPlain, "text/plain"));

            if (hasAttachments) {
                // If we're HTML and have attachments, we have a MimeMultipart container to hold the
                // whole message (mp here), of which one part is a MimeMultipart container
                // (composedMimeMessage) with the user's composed messages, and subsequent parts for
                // the attachments.
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(new MimeBodyPart(composedMimeMessage));
                addAttachmentsToMessage(mp);
                MimeMessageHelper.setBody(message, mp);
            } else {
                // If no attachments, our multipart/alternative part is the only one we need.
                MimeMessageHelper.setBody(message, composedMimeMessage);
            }
*/
        } else if (messageFormat == SimpleMessageFormat.TEXT) {
            // Text-only message.
            MimeMultipart mp = new MimeMultipart();
            mp.addBodyPart(new MimeBodyPart(body, "text/plain"));
            addAttachmentsToMessage(mp);
            MimeMessageHelper.setBody(message, mp);
        }

    }

    public TextBody buildText() {
        return buildText(messageFormat);
    }

    /**
     * Add attachments as parts into a MimeMultipart container.
     *
     * @param mp MimeMultipart container in which to insert parts.
     * @throws MessagingException
     */
    private void addAttachmentsToMessage(final MimeMultipart mp) throws MessagingException {
        Body body;
        for (Blob attachment : attachments) {
            String contentType = attachment.mime_type;
            body = new BinaryMemoryBody(attachment.data, contentType);

            MimeBodyPart bp = new MimeBodyPart(body);

            /*
             * Correctly encode the filename here. Otherwise the whole
             * header value (all parameters at once) will be encoded by
             * MimeHeader.writeTo().
             */
            bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\r\n name=\"%s\"",
                    contentType,
                    EncoderUtil.encodeIfNecessary("qwerty",
                            EncoderUtil.Usage.WORD_ENTITY, 7)));

            bp.setEncoding(MimeUtility.getEncodingforType(contentType));

            /*
             * TODO: Oh the joys of MIME...
             *
             * From RFC 2183 (The Content-Disposition Header Field):
             * "Parameter values longer than 78 characters, or which
             *  contain non-ASCII characters, MUST be encoded as specified
             *  in [RFC 2184]."
             *
             * Example:
             *
             * Content-Type: application/x-stuff
             *  title*1*=us-ascii'en'This%20is%20even%20more%20
             *  title*2*=%2A%2A%2Afun%2A%2A%2A%20
             *  title*3="isn't it!"
             */
            bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,
                    "attachment;\r\n filename=\"%s\";\r\n size=%d",
                    "qwerty", attachment.data.length));

            mp.addBodyPart(bp);
        }
    }

    /**
     * Build the {@link Body} that will contain the text of the message.
     * <p/>
     * <p>
     * Draft messages are treated somewhat differently in that signatures are not appended and HTML
     * separators between composed text and quoted text are not added.
     * </p>
     *
     * @param simpleMessageFormat Specifies what type of message to build ({@code text/plain} vs. {@code text/html}).
     * @return {@link TextBody} instance that contains the entered text and possibly the quoted
     * original message.
     */
    private TextBody buildText(SimpleMessageFormat simpleMessageFormat) {
        String messageText = text;

        PEpTextBodyBuilder textBodyBuilder = new PEpTextBodyBuilder(messageText);


        textBodyBuilder.setIncludeQuotedText(false);

        textBodyBuilder.setInsertSeparator(false);

        textBodyBuilder.setAppendSignature(false);

        TextBody body;
        if (simpleMessageFormat == SimpleMessageFormat.HTML) {
            body = textBodyBuilder.buildTextHtml();
        } else {
            body = textBodyBuilder.buildTextPlain();
        }
        return body;
    }

    public PEpMessageBuilder setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public PEpMessageBuilder setTo(Address[] to) {
        this.to = to;
        return this;
    }

    public PEpMessageBuilder setCc(Address[] cc) {
        this.cc = cc;
        return this;
    }

    public PEpMessageBuilder setBcc(Address[] bcc) {
        this.bcc = bcc;
        return this;
    }

    public PEpMessageBuilder setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
        return this;
    }

    public PEpMessageBuilder setReferences(String references) {
        this.references = references;
        return this;
    }

    public PEpMessageBuilder setRequestReadReceipt(boolean requestReadReceipt) {
        this.requestReadReceipt = requestReadReceipt;
        return this;
    }

    public PEpMessageBuilder setIdentity(Identity identity) {
        this.identity = identity;
        return this;
    }

    public PEpMessageBuilder setMessageFormat(SimpleMessageFormat messageFormat) {
        this.messageFormat = messageFormat;
        return this;
    }

    public PEpMessageBuilder setText(String text) {
        this.text = text;
        return this;
    }


    public PEpMessageBuilder setIdentityChanged(boolean identityChanged) {
        this.identityChanged = identityChanged;
        return this;
    }

    public PEpMessageBuilder setSignatureChanged(boolean signatureChanged) {
        this.signatureChanged = signatureChanged;
        return this;
    }

    public PEpMessageBuilder setMessageReference(MessageReference messageReference) {
        this.messageReference = messageReference;
        return this;
    }

    public Vector<Blob> getAttachments() {
        return attachments;
    }

    public PEpMessageBuilder setAttachments(Vector<Blob> attachments) {
        this.attachments = attachments;
        return this;
    }

}

