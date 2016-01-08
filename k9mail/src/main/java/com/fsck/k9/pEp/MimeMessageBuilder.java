package com.fsck.k9.pEp;


import android.util.Log;

import com.fsck.k9.Identity;
import com.fsck.k9.K9;
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
import org.pEp.jniadapter.Message;

import java.util.Date;
import java.util.Locale;
import java.util.Vector;

/**
 * ripped from MessageBuilder and adopted:
 * - keep attachments in Memory
 *
 */

// FIXME: give pep msg as parameter and work on that directly...

class MimeMessageBuilder {
    private String subject;
    private Address[] to;
    private Address[] cc;
    private Address[] bcc;
    private Vector<String> inReplyTo;
    private Vector<String> references;
    private boolean requestReadReceipt;
    private Identity identity;
    private SimpleMessageFormat messageFormat;
    private String text;

    private Vector<Blob> attachments;

    private Message pEpMessage;

    public MimeMessageBuilder(Message m) {
        this.pEpMessage = m;
    }

    MimeMessage createMessage() {
        // FIXME: are these new String()s really necessary? I think, the adapter does that already...
        com.fsck.k9.Identity me = new com.fsck.k9.Identity();
        org.pEp.jniadapter.Identity from = pEpMessage.getFrom();
        if(from != null) {
            me.setEmail(new String(from.address));
            me.setName(new String(from.username));
        }
        try {

            // FIXME: the following sucks. It makes no sense, to shovel stuff from Message to the builder. But builder has to be reworked anyway.
            // FIXME: Are the new String()s really necessary? Iirc I added them to work around some memory mgmt issues in jniadapter, but it would not work that way anyway...
            setSubject(new String(pEpMessage.getShortmsg()))
                    .setTo(PEpUtils.createAddresses(pEpMessage.getTo()))
                    .setCc(PEpUtils.createAddresses(pEpMessage.getCc()))
                    .setBcc(PEpUtils.createAddresses(pEpMessage.getBcc()))
                    .setInReplyTo(pEpMessage.getInReplyTo())
                    .setReferences(pEpMessage.getReferences())
                    .setIdentity(me)
                    .setMessageFormat(SimpleMessageFormat.TEXT)             // FIXME: pEp: not only text
                    .setText(new String(pEpMessage.getLongmsg()))
                    .setAttachments(pEpMessage.getAttachments());

            //TODO: other header fields. See Message.getOpt<something>

            MimeMessage rv = build();

            rv.setMessageId(pEpMessage.getId());
            rv.setHeader("User-Agent", "k9+pEp late alpha");

            return rv;
        }
        catch (Exception e) {
            Log.e("pepdump", "Could not create MimeMessage: ", e);
        };
        return null;
    }



    private MimeMessage build() throws MessagingException {
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

        if (requestReadReceipt) {                       // FIXME: not used atm. remove or enable.
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
            message.setInReplyTo(clobberVector(inReplyTo));
        }

        if (references != null) {
            message.setReferences(clobberVector(references));
        }
    }

    // move to peputils somewhen soon
    private String clobberVector(Vector<String> sv) {   // FIXME: how do revs come out of array? "<...>" or "...."?
        String rt = "";
        for( String cur : inReplyTo)
            rt += cur + " ";
        return rt;
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
            mp.setSubType("encrypted");
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
        if(attachments == null) return;
        for (Blob attachment : attachments) {
            String contentType = attachment.mime_type;
            String filename = attachment.filename;
            if(filename == null) filename = "file";                 // TODO: why does pep engine not give file names?
            body = new BinaryMemoryBody(attachment.data, contentType);

            MimeBodyPart bp = new MimeBodyPart(body);

            /*
             * Correctly encode the filename here. Otherwise the whole
             * header value (all parameters at once) will be encoded by
             * MimeHeader.writeTo().
             */
            bp.addHeader(MimeHeader.HEADER_CONTENT_TYPE, String.format("%s;\r\n name=\"%s\"",
                    contentType,
                    EncoderUtil.encodeIfNecessary(filename,
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

        TextBodyBuilder textBodyBuilder = new TextBodyBuilder(messageText);


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

    private MimeMessageBuilder setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    private MimeMessageBuilder setTo(Address[] to) {
        this.to = to;
        return this;
    }

    private MimeMessageBuilder setCc(Address[] cc) {
        this.cc = cc;
        return this;
    }

    private MimeMessageBuilder setBcc(Address[] bcc) {
        this.bcc = bcc;
        return this;
    }

    private MimeMessageBuilder setInReplyTo(Vector<String> inReplyTo) {
        this.inReplyTo = inReplyTo;
        return this;
    }

    private MimeMessageBuilder setReferences(Vector<String> references) {
        this.references = references;
        return this;
    }

    private MimeMessageBuilder setRequestReadReceipt(boolean requestReadReceipt) {
        this.requestReadReceipt = requestReadReceipt;
        return this;
    }

    private MimeMessageBuilder setIdentity(Identity identity) {
        this.identity = identity;
        return this;
    }

    private MimeMessageBuilder setMessageFormat(SimpleMessageFormat messageFormat) {
        this.messageFormat = messageFormat;
        return this;
    }

    private MimeMessageBuilder setText(String text) {
        this.text = text;
        return this;
    }

    private Vector<Blob> getAttachments() {
        return attachments;
    }

    private MimeMessageBuilder setAttachments(Vector<Blob> attachments) {
        this.attachments = attachments;
        return this;
    }

}

