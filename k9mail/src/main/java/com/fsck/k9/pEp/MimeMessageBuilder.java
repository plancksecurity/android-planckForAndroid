package com.fsck.k9.pEp;


import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.K9;
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
import org.apache.james.mime4j.util.MimeUtil;
import org.pEp.jniadapter.Blob;
import org.pEp.jniadapter.Message;

import java.util.Date;
import java.util.Locale;
import java.util.Vector;

/**
 * ripped from MessageBuilder and adopted:
 * - keep attachments in Memory
 *
 */


class MimeMessageBuilder {
    private SimpleMessageFormat messageFormat = SimpleMessageFormat.TEXT;

    private Message pEpMessage;

    public MimeMessageBuilder(Message m) {
        this.pEpMessage = m;
    }

    MimeMessage createMessage() {
        try {
            MimeMessage mimeMsg = new MimeMessage();

            evaluateMessageFormat();
            buildHeader(mimeMsg);
            buildBody(mimeMsg);

            return mimeMsg;
        }
        catch (Exception e) {
            Log.e("pepdump", "Could not create MimeMessage: ", e);
        };
        return null;
    }

    private void evaluateMessageFormat() {
        if(!TextUtils.isEmpty(pEpMessage.getLongmsgFormatted()))
            messageFormat = SimpleMessageFormat.HTML;
        else
            messageFormat = SimpleMessageFormat.TEXT;
    }

    private void buildHeader(MimeMessage mimeMsg) throws MessagingException {
        if(pEpMessage.getSent() != null) mimeMsg.addSentDate(pEpMessage.getSent(), K9.hideTimeZone());
        else Log.e("pep", "sent daten == null from engine.");       // FIXME: this should never happen
        mimeMsg.setFrom(PEpUtils.createAddress(pEpMessage.getFrom()));
        mimeMsg.setRecipients(RecipientType.TO, PEpUtils.createAddresses(pEpMessage.getTo()));
        mimeMsg.setRecipients(RecipientType.CC, PEpUtils.createAddresses(pEpMessage.getCc()));
        mimeMsg.setRecipients(RecipientType.BCC, PEpUtils.createAddresses(pEpMessage.getBcc()));
        mimeMsg.setSubject(pEpMessage.getShortmsg());
        mimeMsg.setMessageId(pEpMessage.getId());
        if (!K9.hideUserAgent()) {
            mimeMsg.setHeader("User-Agent", "K9/pEp early beta");       // FIXME: put to ressource (or use predef'd one...)
        }

        mimeMsg.setReplyTo(PEpUtils.createAddresses(pEpMessage.getReplyTo()));
        mimeMsg.setInReplyTo(clobberVector(pEpMessage.getInReplyTo()));
        mimeMsg.setReferences(clobberVector(pEpMessage.getReferences()));
        //TODO: other header fields. See Message.getOpt<something>
    }

    private void buildBody(MimeMessage mimeMsg) throws MessagingException {
        if (pEpMessage.getEncFormat() != Message.EncFormat.None) {   // we have an encrypted msg. Therefore, just attachments...
            // FIXME: how do I add some text ("this mail encrypted by pEp") before the first mime part?
            MimeMultipart mp = new MimeMultipart();
            mp.setSubType("encrypted; protocol=\"application/pgp-encrypted\"");     // FIXME: what if other enc types?
            addAttachmentsToMessage(mp);
            MimeMessageHelper.setBody(mimeMsg, mp);

            return;
        }

        // the following copied from MessageBuilder...

        TextBody body = buildText();        // builds eitehr plain or html

        // text/plain part when messageFormat == MessageFormat.HTML
        TextBody bodyPlain = null;
        boolean hasAttachments = pEpMessage.getAttachments() != null;
        // FIXME: the following is for sure not correct, at least with respect to mime types

        if (messageFormat == SimpleMessageFormat.HTML) {
            // HTML message (with alternative text part)

            // This is the compiled MIME part for an HTML message.
            MimeMultipart composedMimeMessage = new MimeMultipart();
            composedMimeMessage.setSubType("alternative");   // Let the receiver select either the text or the HTML part.
            composedMimeMessage.addBodyPart(new MimeBodyPart(body, "text/html"));
            bodyPlain = buildText(SimpleMessageFormat.TEXT);
            composedMimeMessage.addBodyPart(new MimeBodyPart(bodyPlain, "text/plain"));

            if (hasAttachments) {
                // If we're HTML and have attachments, we have a MimeMultipart container to hold the
                // whole message (mp here), of which one part is a MimeMultipart container
                // (composedMimeMessage) with the user's composed messages, and subsequent parts for
                // the attachments.
                MimeMultipart mp = new MimeMultipart();
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
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(new MimeBodyPart(body, "text/plain"));
                addAttachmentsToMessage(mp);
                MimeMessageHelper.setBody(mimeMsg, mp);
            } else {
                // No attachments to include, just stick the text body in the message and call it good.
                MimeMessageHelper.setBody(mimeMsg, body);
            }

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
        Vector<Blob> attachments = pEpMessage.getAttachments();
        if(attachments == null) return;

        for (int i = 0; i < attachments.size(); i++) {
            Blob attachment = attachments.get(i);
            String contentType = attachment.mime_type;
            String filename = attachment.filename;

            Log.d("pep", "MimeMessageBuilder: BLOB #" + i + ":" + contentType + ":" + filename);
            Log.d("pep", ">"+new String(attachment.data)+"<");

            if (filename != null) filename = EncoderUtil.encodeIfNecessary(filename, EncoderUtil.Usage.WORD_ENTITY, 7);

            body = new BinaryMemoryBody(attachment.data, MimeUtil.ENC_8BIT);

            MimeBodyPart bp = new MimeBodyPart(body);

            /*
             * Correctly encode the filename here. Otherwise the whole
             * header value (all parameters at once) will be encoded by
             * MimeHeader.writeTo().
             */
            if(filename != null)
                bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\r\n name=\"%s\"", contentType, filename));
            else
                bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);

            // FIXME: the following lines lack clearness of flow...
            /* if msg is plain text or if it's one of the non-special pgp attachments (Attachment #1 and #2 have special meaning,
               see "else" branch then dont't treat special (means, use attachment disposition) */
            if (pEpMessage.getEncFormat() == Message.EncFormat.None || i > 1) {
                bp.setEncoding(MimeUtil.ENC_8BIT);

                if (filename != null)
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,
                            "attachment;\r\n filename=\"%s\";\r\n size=%d",
                            filename, attachment.data.length));
                else
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,
                            "attachment;\r\n size=%d",
                            attachment.data.length));
            } else {                // we all live in pgp...
                if(i==0) {        // 1st. attachment is pgp version if encrypted.
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DESCRIPTION, "PGP/MIME version identification");
                } else if (i==1) {
                    bp.addHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, String.format(Locale.US,    // 2nd field is enc'd content.
                            "inline;\r\n filename=\"%s\";\r\n size=%d",
                            filename, attachment.data.length));
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
        if(simpleMessageFormat == SimpleMessageFormat.HTML)
            messageText = pEpMessage.getLongmsgFormatted();
        else
            messageText = pEpMessage.getLongmsg();

        if(messageText==null) {       // FIXME: This must (should?) never happen!
            messageText="Got null msg text (This Is A Bug, please report!)";                // FIXME: Other text for production?
            Log.e("pep", "got null msg txt longmsg="+pEpMessage.getLongmsg()+" format=" + pEpMessage.getLongmsgFormatted());
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
        if(sv != null)
            for( String cur : sv)
                rt += cur + "; ";
        return rt;
    }
}