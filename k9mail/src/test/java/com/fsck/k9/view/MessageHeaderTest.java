package com.fsck.k9.view;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class MessageHeaderTest {

    private static final Address FROM_ADDRESS = Address.parse("from@example1.com")[0];
    private static final Address SENDER_ADDRESS = Address.parse("sender@example2.com")[0];

    @Test
    public void shouldShowSender_withSender_shouldReturnTrue() {
        Message message = createMessage(FROM_ADDRESS, SENDER_ADDRESS);

        boolean showSender = MessageHeader.shouldShowSender(message);

        assertTrue(showSender);
    }

    @Test
    public void shouldShowSender_withoutSender_shouldReturnFalse() {
        Message message = createMessage(FROM_ADDRESS, null);

        boolean showSender = MessageHeader.shouldShowSender(message);

        assertFalse(showSender);
    }

    private Message createMessage(Address from, Address sender) {
        Message message = new MimeMessage();
        message.setFrom(from);
        message.setSender(sender);
        return message;
    }
}
