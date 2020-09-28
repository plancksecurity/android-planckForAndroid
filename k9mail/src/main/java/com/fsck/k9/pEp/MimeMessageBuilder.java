package com.fsck.k9.pEp;


import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.fsck.k9.Globals;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MessageIdGenerator;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.SimpleMessageFormat;

import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.util.MimeUtil;
import org.jsoup.Jsoup;
import foundation.pEp.jniadapter.Blob;
import foundation.pEp.jniadapter.Message;
import foundation.pEp.jniadapter.Pair;
import timber.log.Timber;

import java.util.Locale;
import java.util.Vector;

/**
 * ripped from MessageBuilder and adopted:
 * - keep attachments in Memory
 */


public class MimeMessageBuilder extends MessageBuilder {
    private SimpleMessageFormat messageFormat = SimpleMessageFormat.TEXT;

    private Message pEpMessage;

    public MimeMessageBuilder newInstance() {
        Context context = Globals.getContext();
        MessageIdGenerator messageIdGenerator = MessageIdGenerator.getInstance();
        BoundaryGenerator boundaryGenerator = BoundaryGenerator.getInstance();
        return new MimeMessageBuilder(context, messageIdGenerator, boundaryGenerator);
    }

    @VisibleForTesting
    MimeMessageBuilder(Context context, MessageIdGenerator messageIdGenerator, BoundaryGenerator boundaryGenerator) {
        super(context, messageIdGenerator, boundaryGenerator);
    }

    @Override
    protected void buildMessageInternal() {
        try {
            MimeMessage message = build();
            queueMessageBuildSuccess(message);
        } catch (MessagingException me) {
            queueMessageBuildException(me);
        }
    }

    @Override
    protected void buildMessageOnActivityResult(int requestCode, Intent data) {
        throw new UnsupportedOperationException();
    }

    //--------------------------
    public MimeMessageBuilder(Message m) {
        super(null, null, null);
        this.pEpMessage = m;
    }

    @NonNull
    MimeMessage createMessage() throws MessagingException {
        MimeMessage mimeMsg = new MimeMessage();
        evaluateMessageFormat();
        buildHeader(mimeMsg);
        buildBody(mimeMsg);
        return mimeMsg;
    }

    @NonNull
    public MimeMessage parseMessage(Message m) throws MessagingException {
        this.pEpMessage = m;
        MimeMessage message = new MimeMessage();

        evaluateMessageFormat();
        buildHeaderForMessage(message);
        buildBodyForMessage(message);

        return message;
    }

    private void evaluateMessageFormat() {
        if (!TextUtils.isEmpty(pEpMessage.getLongmsgFormatted()))
            messageFormat = SimpleMessageFormat.HTML;
        else
            messageFormat = SimpleMessageFormat.TEXT;
    }

    private void buildHeaderForMessage(MimeMessage mimeMsg) throws MessagingException {
        //if (pEpMessage.getSent() != null) mimeMsg.addSentDate(pEpMessage.getSent(), K9.hideTimeZone());
        //else Log.e("pep", "sent daten == null from engine.");       // FIXME: this should never happen
        buildHeader(mimeMsg);
        mimeMsg.setMessageId(pEpMessage.getId());

        mimeMsg.setReplyTo(PEpUtils.createAddresses(pEpMessage.getReplyTo()));
        mimeMsg.setInReplyTo(clobberVector(pEpMessage.getInReplyTo()));
        mimeMsg.setReferences(clobberVector(pEpMessage.getReferences()));

        if (pEpMessage.getOptFields() != null) {
            for (Pair<String, String> field : pEpMessage.getOptFields()) {
                mimeMsg.addHeader(field.first, field.second);
            }
        }
    }

    private void buildBodyForMessage(MimeMessage mimeMsg) throws MessagingException {
        if (pEpMessage.getEncFormat() != Message.EncFormat.None) {   // we have an encrypted msg. Therefore, just attachments...
            // FIXME: how do I add some text ("this mail encrypted by pEp") before the first mime part?
            MimeMultipart mp = MimeMultipart.newInstance();
            mp.setSubType("encrypted; protocol=\"application/pgp-encrypted\"");     // FIXME: what if other enc types?
            addAttachmentsToMessage(mp);
            MimeMessageHelper.setBody(mimeMsg, mp);
            return;
        }
        // FIXME: 9/01/17 we should use buildbody
        //buildBody(mimeMsg);
        // FIXME: 9/01/17 and we should remove the following
        // the following copied from MessageBuilder...
        TextBody body = buildText(mimeMsg);        // builds eitehr plain or html
        // text/plain part when messageFormat == MessageFormat.HTML
        TextBody bodyPlain;
        boolean hasAttachments = pEpMessage.getAttachments() != null;
        // FIXME: the following is for sure not correct, at least with respect to mime types
        if (messageFormat == SimpleMessageFormat.HTML) {
            // HTML message (with alternative text part)
            // This is the compiled MIME part for an HTML message.
            MimeMultipart composedMimeMessage = MimeMultipart.newInstance();
            composedMimeMessage.setSubType("alternative");   // Let the receiver select either the text or the HTML part.
            bodyPlain = buildText(SimpleMessageFormat.TEXT);
            composedMimeMessage.addBodyPart(new MimeBodyPart(bodyPlain, "text/plain"));
            composedMimeMessage.addBodyPart(new MimeBodyPart(body, "text/html"));
            if (hasAttachments) {
                // If we're HTML and have attachments, we have a MimeMultipart container to hold the
                // whole message (mp here), of which one part is a MimeMultipart container
                // (composedMimeMessage) with the user's composed messages, and subsequent parts for
                // the attachments.
                MimeMultipart mp = MimeMultipart.newInstance();
                mp.addBodyPart(new MimeBodyPart(composedMimeMessage));
                addAttachmentsToMessage(mp);
                MimeMessageHelper.setBody(mimeMsg, mp);
            } else {
                // If no attachments, our multipart/alternative part is the only one we need.
                MimeMessageHelper.setBody(mimeMsg, composedMimeMessage);
            }
        } else if (messageFormat == SimpleMessageFormat.TEXT) {
            // Text-only message.
            if (hasAttachments) {
                MimeMultipart mp = MimeMultipart.newInstance();
                mp.addBodyPart(new MimeBodyPart(body, "text/plain"));
                addAttachmentsToMessage(mp);
                MimeMessageHelper.setBody(mimeMsg, mp);
            } else {
                // No attachments to include, just stick the text body in the message and call it good.
                MimeMessageHelper.setBody(mimeMsg, body);
            }
        }
    }

    private TextBody buildText(MimeMessage mimeMsg) {
        if (isSMimeMessage(mimeMsg)) {
            MimeTextBodyBuilder mimeTextBodyBuilder = new MimeTextBodyBuilder("This is an S/MIME encrypted message and cannot be displayed in this version");

            mimeTextBodyBuilder.setIncludeQuotedText(false);

            mimeTextBodyBuilder.setInsertSeparator(false);

            mimeTextBodyBuilder.setAppendSignature(false);

            return mimeTextBodyBuilder.buildTextPlain();
        } else {
            return buildText(messageFormat);
        }
    }

    private boolean isSMimeMessage(MimeMessage mimeMsg) {
        return mimeMsg.getHeader(MimeHeader.HEADER_CONTENT_DESCRIPTION).length > 0 &&
                mimeMsg.getHeader(MimeHeader.HEADER_CONTENT_DESCRIPTION)[0].contains("S/MIME Encrypted Message");
    }

    /**
     * Add attachments as parts into a MimeMultipart container.
     *
     * @param mp MimeMultipart container in which to insert parts.
     * @throws MessagingException
     */
    private void addAttachmentsToMessage(final MimeMultipart mp) throws MessagingException {
        //TODO: Check if Headers.kt can be used and simplify this Look at MessageBuilder.
        Body body;
        Vector<Blob> attachments = pEpMessage.getAttachments();
        if (attachments == null) return;

        for (int i = 0; i < attachments.size(); i++) {
            Blob attachment = attachments.get(i);
            String contentType = attachment.mime_type;
            String filename = attachment.filename;
//            Log.d("pep", "MimeMessageBuilder: BLOB #" + i + ":" + contentType + ":" + filename);

            if (filename != null)
                filename = EncoderUtil.encodeIfNecessary(filename, EncoderUtil.Usage.WORD_ENTITY, 7);

            if (pEpMessage.getEncFormat() != Message.EncFormat.None) body = new BinaryMemoryBody(attachment.data, MimeUtil.ENC_8BIT);
            else body = new BinaryMemoryBody(Base64.encode(attachment.data, Base64.DEFAULT), MimeUtil.ENC_BASE64);

            MimeBodyPart bp = new MimeBodyPart(body);

            /*
             * Correctly encode the filename here. Otherwise the whole
             * header value (all parameters at once) will be encoded by
             * MimeHeader.writeTo().
             */
            boolean isInlineAttachment = false;
            if (filename != null) {
                isInlineAttachment = attachment.filename.startsWith(MimeHeader.CID_SCHEME);
                String[] filenameParts = attachment.filename.split(MimeHeader.URI_SCHEME_SEPARATOR);
                if (filenameParts.length > 1) {
                    bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\r\n name=\"%s\"", contentType, filenameParts[1]));
                } else {
                    bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\r\n name=\"%s\"", contentType, filename));

                }
                if(isInlineAttachment) {
                    if (filenameParts.length > 1) bp.addHeader(MimeHeader.HEADER_CONTENT_ID, filenameParts[1]);
                    else bp.addHeader(MimeHeader.HEADER_CONTENT_ID, filename);
                }
            } else {
                bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);
            }
            // FIXME: the following lines lack clearness of flow...
            /* if msg is plain text or if it's one of the non-special pgp attachments (Attachment #1 and #2 have special meaning,
               see "else" branch then dont't treat special (means, use attachment disposition) */
            if (pEpMessage.getEncFormat() == Message.EncFormat.None || i > 1) {

                if (pEpMessage.getEncFormat() == Message.EncFormat.None) {
                    bp.setEncoding(MimeUtil.ENC_BASE64);
                }
                else bp.setEncoding(MimeUtil.ENC_8BIT);

                boolean isFileAttachment = filename != null && attachment.filename.startsWith(MimeHeader.FILE_SCHEME);
                if (filename != null && isFileAttachment) {
                    String[] filenameParts = attachment.filename.split(MimeHeader.URI_SCHEME_SEPARATOR);
                    if (filenameParts.length > 1) {
                        bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,
                            "attachment;\r\n filename=\"%s\";\r\n size=%d",
                            filenameParts[1], attachment.data.length));
                    } else {
                        bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,
                                "attachment;\r\n filename=\"%s\";\r\n size=%d",
                                filename, attachment.data.length));
                    }
                } else if(filename != null && isInlineAttachment) {
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "inline");
                } else {
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,
                            "attachment;\r\n size=%d",
                            attachment.data.length));
                }
            } else {                // we all live in pgp...
                if (i == 0) {        // 1st. attachment is pgp version if encrypted.
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DESCRIPTION, "PGP/MIME version identification");
                } else if (i == 1) {
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,    // 2nd field is enc'd content.
                            "inline;\r\n filename=\"%s\";\r\n size=%d",
                            filename.split(MimeHeader.URI_SCHEME_SEPARATOR)[1], attachment.data.length));
                }
            }

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

    /* FIXME: the following logic needs some intense testing. Not completely sure whether I broke threading and quoting badly somewhere... */
    private TextBody buildText(SimpleMessageFormat simpleMessageFormat) {
        String messageText = null;
        if (simpleMessageFormat == SimpleMessageFormat.HTML)
            messageText = pEpMessage.getLongmsgFormatted();
        else {
            if (messageFormat == SimpleMessageFormat.HTML
                    && (pEpMessage.getLongmsg() == null || pEpMessage.getLongmsg().isEmpty())) {
                messageText = Jsoup.parse(pEpMessage.getLongmsgFormatted()).text();
            } else if (pEpMessage.getLongmsg() != null){
                messageText = pEpMessage.getLongmsg();
            }
        }

        if (messageText == null) {       // FIXME: This must (should?) never happen!
            messageText = "";
            Log.e("pEp", "Got null msg text (This Is A Bug, please report!)");
            Timber.e("pEp", "\"Got null msg text (This Is A Bug, please report!)\"=" + pEpMessage.getLongmsg() + " format=" + pEpMessage.getLongmsgFormatted());
        }

        MimeTextBodyBuilder mimeTextBodyBuilder = new MimeTextBodyBuilder(messageText);

        mimeTextBodyBuilder.setIncludeQuotedText(false);

        mimeTextBodyBuilder.setInsertSeparator(false);

        mimeTextBodyBuilder.setAppendSignature(false);

        TextBody body;
        if (simpleMessageFormat == SimpleMessageFormat.HTML) {
            body = mimeTextBodyBuilder.buildTextHtml();
        } else {
            body = mimeTextBodyBuilder.buildTextPlain();
        }
        return body;
    }

    // move to peputils somewhen soon
    private String clobberVector(Vector<String> sv) {   // FIXME: how do revs come out of array? "<...>" or "...."?
        String rt = "";
        if (sv != null)
            for (String cur : sv)
                rt += cur + "; ";
        return rt;
    }
}