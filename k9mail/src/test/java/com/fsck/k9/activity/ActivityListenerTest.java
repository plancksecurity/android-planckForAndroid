package com.fsck.k9.activity;


import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
@Config(sdk = 22)
public class ActivityListenerTest {
    private static final String FOLDER = "folder";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final int COUNT = 23;


    private Context context;
    private Context contextSpy;
    private Account account;
    private Message message;
    private ActivityListener activityListener;


    @Before
    public void before() {
        context = ApplicationProvider.getApplicationContext();
        contextSpy = Mockito.spy(context);

        Mockito.doReturn("Syncing messages").when(contextSpy).getString(R.string.status_syncing);
        Mockito.doReturn("1/2").when(contextSpy).getString(R.string.folder_progress, 1, 2);
        Mockito.doReturn("Syncing disabled").when(contextSpy).getString(R.string.status_syncing_off);
        Mockito.doReturn("2/3").when(contextSpy).getString(R.string.folder_progress, 2, 3);

        account = createAccount();
        message = mock(Message.class);

        activityListener = new ActivityListener();
    }

    @Test
    public void getOperation__whenFolderStatusChanged() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.folderStatusChanged(account, FOLDER, COUNT);
        String operation = activityListener.getOperation(contextSpy);

        assertEquals("Syncing messages", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxStarted() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);

        String operation = activityListener.getOperation(contextSpy);

        assertEquals("Syncing messages", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxProgress_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxProgress(account, FOLDER, 1, 2);

        String operation = activityListener.getOperation(contextSpy);

        assertEquals("Syncing messages", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFailed_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxFailed(account, FOLDER, ERROR_MESSAGE);

        String operation = activityListener.getOperation(contextSpy);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFailedAfterHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);
        activityListener.synchronizeMailboxFailed(account, FOLDER, ERROR_MESSAGE);

        String operation = activityListener.getOperation(contextSpy);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFinished() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxFinished(account, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(contextSpy);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);

        String operation = activityListener.getOperation(contextSpy);

        assertEquals("Syncing messages", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersProgress() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);
        activityListener.synchronizeMailboxHeadersProgress(account, FOLDER, 2, 3);

        String operation = activityListener.getOperation(contextSpy);

        assertEquals("Syncing messages", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersFinished() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);
        activityListener.synchronizeMailboxHeadersFinished(account, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(contextSpy);

        assertEquals("", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxNewMessage() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxNewMessage(account, FOLDER, message);

        String operation = activityListener.getOperation(contextSpy);

        assertEquals("Syncing messages", operation);
    }

    private Account createAccount() {
        Account account = mock(Account.class);
        when(account.getDescription()).thenReturn("account");
        return account;
    }
}
