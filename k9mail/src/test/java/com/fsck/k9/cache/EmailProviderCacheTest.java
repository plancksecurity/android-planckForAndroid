package com.fsck.k9.cache;


import androidx.test.core.app.ApplicationProvider;

import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


public class EmailProviderCacheTest extends RobolectricTest {

    private EmailProviderCache cache;
    @Mock
    private LocalMessage mockLocalMessage;
    @Mock
    private LocalFolder mockLocalMessageFolder;
    private Long localMessageId = 1L;
    private Long localMessageFolderId = 2L;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        cache = EmailProviderCache.getCache(UUID.randomUUID().toString(), ApplicationProvider.getApplicationContext());
        when(mockLocalMessage.getId()).thenReturn(localMessageId);
        when(mockLocalMessage.getFolder()).thenReturn(mockLocalMessageFolder);
        when(mockLocalMessageFolder.getId()).thenReturn(localMessageFolderId);
    }

    @Test
    public void getCache_returnsDifferentCacheForEachUUID() {
        EmailProviderCache cache = EmailProviderCache.getCache("u001", ApplicationProvider.getApplicationContext());
        EmailProviderCache cache2 = EmailProviderCache.getCache("u002", ApplicationProvider.getApplicationContext());

        assertNotEquals(cache, cache2);
    }

    @Test
    public void getCache_returnsSameCacheForAUUID() {
        EmailProviderCache cache = EmailProviderCache.getCache("u001", ApplicationProvider.getApplicationContext());
        EmailProviderCache cache2 = EmailProviderCache.getCache("u001", ApplicationProvider.getApplicationContext());

        assertSame(cache, cache2);
    }

    @Test
    public void getValueForMessage_returnsValueSetForMessage() {
        cache.setValueForMessages(Collections.singletonList(1L), "subject", "Subject");

        String result = cache.getValueForMessage(1L, "subject");

        assertEquals("Subject", result);
    }

    @Test
    public void getValueForUnknownMessage_returnsNull() {
        String result = cache.getValueForMessage(1L, "subject");

        assertNull(result);
    }

    @Test
    public void getValueForUnknownMessage_returnsNullWhenRemoved() {
        cache.setValueForMessages(Collections.singletonList(1L), "subject", "Subject");
        cache.removeValueForMessages(Collections.singletonList(1L), "subject");

        String result = cache.getValueForMessage(1L, "subject");

        assertNull(result);
    }

    @Test
    public void getValueForThread_returnsValueSetForThread() {
        cache.setValueForThreads(Collections.singletonList(1L), "subject", "Subject");

        String result = cache.getValueForThread(1L, "subject");

        assertEquals("Subject", result);
    }

    @Test
    public void getValueForUnknownThread_returnsNull() {
        String result = cache.getValueForThread(1L, "subject");

        assertNull(result);
    }

    @Test
    public void getValueForUnknownThread_returnsNullWhenRemoved() {
        cache.setValueForThreads(Collections.singletonList(1L), "subject", "Subject");
        cache.removeValueForThreads(Collections.singletonList(1L), "subject");

        String result = cache.getValueForThread(1L, "subject");

        assertNull(result);
    }

    @Test
    public void isMessageHidden_returnsTrueForHiddenMessage() {
        cache.hideMessages(Collections.singletonList(mockLocalMessage));

        boolean result = cache.isMessageHidden(localMessageId, localMessageFolderId);

        assertTrue(result);
    }

    @Test
    public void isMessageHidden_returnsFalseForUnknownMessage() {
        boolean result = cache.isMessageHidden(localMessageId, localMessageFolderId);

        assertFalse(result);
    }

    @Test
    public void isMessageHidden_returnsFalseForUnhidenMessage() {
        cache.hideMessages(Collections.singletonList(mockLocalMessage));
        cache.unhideMessages(Collections.singletonList(mockLocalMessage));

        boolean result = cache.isMessageHidden(localMessageId, localMessageFolderId);

        assertFalse(result);
    }

}
