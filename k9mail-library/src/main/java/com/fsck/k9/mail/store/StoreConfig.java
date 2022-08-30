package com.fsck.k9.mail.store;


import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;

public interface StoreConfig {
    String getStoreUri();
    String getTransportUri();

    OAuth2TokenProvider getOAuth2TokenProvider();

    boolean subscribedFoldersOnly();
    boolean useCompression(NetworkType type);

    String getInboxFolderName();
    String getOutboxFolderName();
    String getDraftsFolderName();

    void setArchiveFolderName(String name);
    void setDraftsFolderName(String name);
    void setTrashFolderName(String name);
    void setSpamFolderName(String name);
    void setSentFolderName(String name);
    void setAutoExpandFolderName(String name);
    void setInboxFolderName(String name);

    int getMaximumAutoDownloadMessageSize();

    boolean allowRemoteSearch();
    boolean isRemoteSearchFullText();

    boolean isPushPollOnConnect();

    int getDisplayCount();

    int getIdleRefreshMinutes();
}
