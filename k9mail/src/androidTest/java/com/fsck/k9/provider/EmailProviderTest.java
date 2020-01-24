package com.fsck.k9.provider;


import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.provider.ProviderTestRule;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EmailProviderTest {
    private MimeMessage message;
    private MimeMessage laterMessage;
    private MimeMessage reply;
    private MimeMessage replyAtSameTime;


    @Rule
    public ProviderTestRule mProviderRule =
            new ProviderTestRule
                    .Builder(EmailProvider.class, MessageProvider.AUTHORITY)
                    .build();

    @Before
    public void buildMessages() {
        message = new MimeMessage();
        message.setSubject("Test Subject");
        message.setSentDate(new GregorianCalendar(2016, 1, 2).getTime(), false);
        message.setMessageId("<uid001@email.com>");

        laterMessage = new MimeMessage();
        laterMessage.setSubject("Test Subject2");
        laterMessage.setSentDate(new GregorianCalendar(2016, 1, 3).getTime(), false);

        reply = new MimeMessage();
        reply.setSubject("Re: Test Subject");
        reply.setSentDate(new GregorianCalendar(2016, 1, 3).getTime(), false);
        reply.setMessageId("<uid002@email.com>");
        reply.setInReplyTo("<uid001@email.com>");

        replyAtSameTime = new MimeMessage();
        replyAtSameTime.setSubject("Re: Test Subject");
        replyAtSameTime.setSentDate(new GregorianCalendar(2016, 1, 2).getTime(), false);
        replyAtSameTime.setMessageId("<uid002@email.com>");
        replyAtSameTime.setInReplyTo("<uid001@email.com>");
    }

    @Test
    public void onCreate_shouldReturnTrue() {
        assertNotNull(mProviderRule.getResolver());

        //     boolean returnValue = mProviderRule.getResolver().onCreate();

        //     assertEquals(true, returnValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void query_withInvalidURI_throwsIllegalArgumentException() {
        mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://com.google.www"),
                        new String[]{},
                        "",
                        new String[]{},
                        "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void query_forMessagesWithInvalidAccount_throwsIllegalArgumentException() {
        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/1/messages"),
                        new String[]{},
                        "",
                        new String[]{},
                        "");

        assertNotNull(cursor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void query_forMessagesWithAccountAndWithoutRequiredFields_throwsIllegalArgumentException() {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() + "/messages"),
                        new String[]{},
                        "",
                        new String[]{},
                        "");

        assertNotNull(cursor);
        assertTrue(cursor.isAfterLast());
    }

    @Test(expected = SQLException.class) //Handle this better?
    public void query_forMessagesWithAccountAndRequiredFieldsWithNoOrderBy_throwsSQLiteException() {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() + "/messages"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT
                        },
                        "",
                        new String[]{},
                        "");

        assertNotNull(cursor);
        assertTrue(cursor.isAfterLast());
    }

    @Test
    public void query_forMessagesWithEmptyAccountAndRequiredFieldsAndOrderBy_providesEmptyResult() {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() + "/messages"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                                EmailProvider.MessageColumns.SUBJECT
                        },
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE);

        assertNotNull(cursor);
        assertFalse(cursor.moveToFirst());
    }

    @Test
    public void query_forMessagesWithAccountAndRequiredFieldsAndOrderBy_providesResult() throws MessagingException {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();
        account.getLocalStore().getFolder("Inbox").appendMessages(Collections.singletonList(message));

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() + "/messages"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                                EmailProvider.MessageColumns.SUBJECT},
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE);

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(message.getSubject(), cursor.getString(3));
    }

    @Test
    public void query_forMessagesWithAccountAndRequiredFieldsAndOrderBy_sortsCorrectly() throws MessagingException {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();
        account.getLocalStore().getFolder("Inbox").appendMessages(Arrays.asList(message, laterMessage));

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() + "/messages"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                                EmailProvider.MessageColumns.SUBJECT
                        },
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE + " DESC");

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(laterMessage.getSubject(), cursor.getString(3));
        cursor.moveToNext();
        assertEquals(message.getSubject(), cursor.getString(3));
    }

    @Test
    public void query_forThreadedMessages_sortsCorrectly() throws MessagingException {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();
        account.getLocalStore().getFolder("Inbox").appendMessages(Arrays.asList(message, laterMessage));

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() +
                                "/messages/threaded"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                                EmailProvider.MessageColumns.SUBJECT,
                                EmailProvider.MessageColumns.DATE
                        },
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE + " DESC");
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(laterMessage.getSubject(), cursor.getString(3));
        cursor.moveToNext();
        assertEquals(message.getSubject(), cursor.getString(3));
    }

    @Test
    public void query_forThreadedMessages_showsThreadOfEmailOnce() throws MessagingException {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();
        account.getLocalStore().getFolder("Inbox").appendMessages(Collections.singletonList(message));
        account.getLocalStore().getFolder("Inbox").appendMessages(Collections.singletonList(reply));

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() +
                                "/messages/threaded"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                                EmailProvider.MessageColumns.SUBJECT,
                                EmailProvider.MessageColumns.DATE,
                                EmailProvider.SpecialColumns.THREAD_COUNT
                        },
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE + " DESC");

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(2, cursor.getInt(5));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void query_forThreadedMessages_showsThreadOfEmailWithSameSendTimeOnce() throws MessagingException {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();
        account.getLocalStore().getFolder("Inbox").appendMessages(Collections.singletonList(message));
        account.getLocalStore().getFolder("Inbox").appendMessages(Collections.singletonList(replyAtSameTime));

        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() +
                                "/messages/threaded"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                                EmailProvider.MessageColumns.SUBJECT,
                                EmailProvider.MessageColumns.DATE,
                                EmailProvider.SpecialColumns.THREAD_COUNT
                        },
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE + " DESC");

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(2, cursor.getInt(5));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void query_forAThreadOfMessages_returnsMessage() throws MessagingException {
        Account account = Preferences.getPreferences(ApplicationProvider.getApplicationContext()).newAccount();
        account.getUuid();
        Message message = new MimeMessage();
        message.setSubject("Test Subject");
        message.setSentDate(new GregorianCalendar(2016, 1, 2).getTime(), false);
        account.getLocalStore().getFolder("Inbox").appendMessages(Collections.singletonList(message));

        //Now get the thread id we just put in.
        Cursor cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() + "/messages"),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                        },
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE);

        assertNotNull(cursor);
        cursor.moveToFirst();
        String threadId = cursor.getString(2);

        //Now check the message is listed under that thread
        Cursor threadCursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + EmailProvider.AUTHORITY + "/account/" + account.getUuid() +
                                "/thread/" + threadId),
                        new String[]{
                                EmailProvider.MessageColumns.ID,
                                EmailProvider.MessageColumns.FOLDER_ID,
                                EmailProvider.ThreadColumns.ROOT,
                                EmailProvider.MessageColumns.SUBJECT,
                                EmailProvider.MessageColumns.DATE
                        },
                        "",
                        new String[]{},
                        EmailProvider.MessageColumns.DATE);

        assertNotNull(threadCursor);
        assertTrue(threadCursor.moveToFirst());
        assertEquals(message.getSubject(), threadCursor.getString(3));
    }
}
