package com.fsck.k9.helper;


import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.planck.ui.keys.FakeAndroidKeyStore;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class IdentityHelperTest extends RobolectricTest {

    private Account account;
    private MimeMessage msg;


    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Context contextSpy = Mockito.spy(context);
        Mockito.doReturn(contextSpy).when(contextSpy).getApplicationContext();
        Mockito.doReturn("text").when(contextSpy).getString(anyInt());


        createDummyAccount(contextSpy);


        BinaryTempFileBody.setTempDirectory(new File("."));
        msg = parseWithoutRecurse(toStream(
                "From: <adam@example.org>\r\n" +
                        "To: <eva@example.org>\r\n" +
                        "Subject: Testmail\r\n" +
                        "MIME-Version: 1.0\r\n" +
                        "Content-type: text/plain\r\n" +
                        "Content-Transfer-Encoding: 7bit\r\n" +
                        "\r\n" +
                        "this is some test text."));
    }


    private static MimeMessage parseWithoutRecurse(InputStream data) throws Exception {
        return MimeMessage.parseMimeMessage(data, false);
    }

    private static ByteArrayInputStream toStream(String rawMailData) throws Exception {
        return new ByteArrayInputStream(rawMailData.getBytes(StandardCharsets.ISO_8859_1));
    }

    private void createDummyAccount(Context context) {
        account = new DummyAccount(context);
        setIdentity();
    }

    private void setIdentity() {
        Identity identity = new Identity();
        identity.setEmail("test@mail.com");
        identity.setName("test");
        Identity identity2 = new Identity();
        identity2.setEmail("test2@mail.com");
        identity2.setName("test2");
        Identity eva = new Identity();
        eva.setEmail("eva@example.org");
        eva.setName("Eva");

        List<Identity> identityList = new ArrayList<>();
        identityList.add(identity);
        identityList.add(identity2);
        identityList.add(eva);
        account.setIdentities(identityList);
    }

    @Test
    public void testXOriginalTo() {
        Address[] addresses = {new Address("test2@mail.com")};
        msg.setRecipients(Message.RecipientType.X_ORIGINAL_TO, addresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));
    }

    @Test
    public void testTo_withoutXOriginalTo() {
        Identity eva = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(eva.getEmail().equalsIgnoreCase("eva@example.org"));
    }

    @Test
    public void testDeliveredTo() {
        Address[] addresses = {new Address("test2@mail.com")};
        msg.setRecipients(Message.RecipientType.DELIVERED_TO, addresses);
        msg.removeHeader("X-Original-To");

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));

    }

    @Test
    public void testXEnvelopeTo() {
        Address[] addresses = {new Address("test@mail.com")};
        msg.setRecipients(Message.RecipientType.X_ENVELOPE_TO, addresses);
        msg.removeHeader("X-Original-To");
        msg.removeHeader("Delivered-To");

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test@mail.com"));
    }

    @Test
    public void testXEnvelopeTo_withXOriginalTo() {
        Address[] addresses = {new Address("test@mail.com")};
        Address[] xoriginaltoaddresses = {new Address("test2@mail.com")};
        msg.setRecipients(Message.RecipientType.X_ENVELOPE_TO, addresses);
        msg.setRecipients(Message.RecipientType.X_ORIGINAL_TO, xoriginaltoaddresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));
    }


    static class DummyAccount extends Account {

        protected DummyAccount(Context context) {
            super(context);
        }
    }

    @BeforeClass
    public static void beforeClass() {
        FakeAndroidKeyStore.setup();
    }
}
