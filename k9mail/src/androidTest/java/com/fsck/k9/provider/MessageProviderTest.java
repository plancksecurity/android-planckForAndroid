package com.fsck.k9.provider;

import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.provider.ProviderTestRule;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class MessageProviderTest {

    private Cursor cursor;

    @Rule
    public ProviderTestRule mProviderRule =
            new ProviderTestRule
                    .Builder(MessageProvider.class, MessageProvider.AUTHORITY)
                    .build();

    @Before
    public void setUp() {
        deleteAllAccounts();
    }

    @After
    public void tearDown() {
        deleteAllAccounts();
    }

    private void deleteAllAccounts() {
        Preferences preferences = Preferences.getPreferences(ApplicationProvider.getApplicationContext());
        List<Account> accounts = preferences.getAccounts();
        for(Account account : accounts) {
            preferences.deleteAccount(account);
        }
    }

    private void createAccount() {
        Preferences preferences = Preferences.getPreferences(ApplicationProvider.getApplicationContext());
        Account account = preferences.newAccount();
        account.setDescription("TestAccount");
        account.setChipColor(10);
        account.setStoreUri("imap://user@domain.com/");
        account.save(preferences);
    }

    @Test
    public void query_forAccounts_withNoAccounts_returnsEmptyCursor() {
        cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + MessageProvider.AUTHORITY + "/accounts/"),
                        null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertFalse(isNotEmpty);
    }

    @Test
    public void query_forAccounts_withAccount_returnsCursorWithData() {
        createAccount();
        cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + MessageProvider.AUTHORITY + "/accounts/"),
                        null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertTrue(isNotEmpty);
    }

    @Test
    public void query_forAccounts_withAccount_withNoProjection_returnsNumberAndName() {
        createAccount();

        cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + MessageProvider.AUTHORITY + "/accounts/"),
                        null, null, null, null);
        cursor.moveToFirst();

        assertEquals(2, cursor.getColumnCount());
        assertEquals(0, cursor.getColumnIndex(MessageProvider.AccountColumns.ACCOUNT_NUMBER));
        assertEquals(1, cursor.getColumnIndex(MessageProvider.AccountColumns.ACCOUNT_NAME));
        assertEquals(0, cursor.getInt(0));
        assertEquals("TestAccount", cursor.getString(1));
    }


    @Test
    public void query_forInboxMessages_whenEmpty_returnsEmptyCursor() {
        cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + MessageProvider.AUTHORITY + "/inbox_messages/"),
                        null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertFalse(isNotEmpty);
    }

    @Test
    public void query_forAccountUnreadMessages_whenNoAccount_returnsEmptyCursor() {
        cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + MessageProvider.AUTHORITY + "/account_unread/0"),
                        null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertFalse(isNotEmpty);
    }

    @Test
    public void query_forAccountUnreadMessages_whenNoMessages_returns0Unread() {
        createAccount();
        cursor = mProviderRule
                .getResolver()
                .query(
                        Uri.parse("content://" + MessageProvider.AUTHORITY + "/account_unread/0"),
                        null, null, null, null);
        cursor.moveToFirst();

        assertEquals(2, cursor.getColumnCount());
        assertEquals(0, cursor.getColumnIndex(MessageProvider.UnreadColumns.ACCOUNT_NAME));
        assertEquals(1, cursor.getColumnIndex(MessageProvider.UnreadColumns.UNREAD));
        assertEquals(0, cursor.getInt(1));
        assertEquals("TestAccount", cursor.getString(0));
    }
}
