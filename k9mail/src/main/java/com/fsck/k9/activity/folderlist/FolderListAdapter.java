package com.fsck.k9.activity.folderlist;

import android.text.TextUtils.TruncateAt;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.ActivityListener;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import security.pEp.ui.PEpUIUtils;
import security.pEp.ui.resources.PEpResourcesProvider;
import security.pEp.ui.resources.ResourcesProvider;
import timber.log.Timber;

public class FolderListAdapter extends BaseAdapter implements Filterable, FolderFilterListener {

    private FontSizes mFontSizes = K9.getFontSizes();

    private List<FolderInfoHolder> folders;
    private List<FolderInfoHolder> filteredFolders;
    private FolderListFilter filter;
    private Account account;
    private FolderList activity;
    private FolderList.FolderListHandler handler;

    private ResourcesProvider resourcesProvider;


    private ActivityListener listener = new ActivityListener() {
        @Override
        public void informUserOfStatus() {
            activity.getHandler().refreshTitle();
            handler.dataChanged();
        }

        @Override
        public void accountStatusChanged(BaseAccount account, AccountStats stats) {
            if (!account.equals(FolderListAdapter.this.account)) {
                return;
            }
            if (stats == null) {
                return;
            }
            handler.refreshTitle();
        }

        @Override
        public void listFoldersStarted(Account account) {
            if (account.equals(FolderListAdapter.this.account)) {
                handler.progress(true);
            }
            super.listFoldersStarted(account);

        }

        @Override
        public void listFoldersFailed(Account account, String message) {
            if (account.equals(FolderListAdapter.this.account)) {
                handler.progress(false);
                FeedbackTools.showShortFeedback(activity.getListView(), R.string.fetching_folders_failed);
            }
            super.listFoldersFailed(account, message);
        }

        @Override
        public void listFoldersFinished(Account account) {
            if (account.equals(FolderListAdapter.this.account)) {

                handler.progress(false);
                MessagingController.getInstance(activity).refreshListener(listener);
                handler.dataChanged();
            }
            super.listFoldersFinished(account);

        }

        @Override
        public void listFolders(Account account, List<LocalFolder> folders) {
            if (account.equals(FolderListAdapter.this.account)) {

                List<FolderInfoHolder> newFolders = new LinkedList<>();
                List<FolderInfoHolder> topFolders = new LinkedList<>();

                Account.FolderMode aMode = account.getFolderDisplayMode();
                for (LocalFolder folder : folders) {
                    Folder.FolderClass fMode = folder.getDisplayClass();

                    if ((aMode == Account.FolderMode.FIRST_CLASS && fMode != Folder.FolderClass.FIRST_CLASS)
                            || (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                            fMode != Folder.FolderClass.FIRST_CLASS &&
                            fMode != Folder.FolderClass.SECOND_CLASS)
                            || (aMode == Account.FolderMode.NOT_SECOND_CLASS && fMode == Folder.FolderClass.SECOND_CLASS)) {
                        continue;
                    }

                    FolderInfoHolder holder = null;

                    int folderIndex = getFolderIndex(folder.getName());
                    if (folderIndex >= 0) {
                        holder = (FolderInfoHolder) getItem(folderIndex);
                    }

                    if (holder == null) {
                        holder = new FolderInfoHolder(activity, folder, FolderListAdapter.this.account, -1);
                    } else {
                        holder.populate(activity, folder, FolderListAdapter.this.account, -1);

                    }
                    if (folder.isInTopGroup()) {
                        topFolders.add(holder);
                    } else {
                        newFolders.add(holder);
                    }
                }
                Collections.sort(newFolders);
                Collections.sort(topFolders);
                topFolders.addAll(newFolders);
                topFolders = PEpUIUtils.orderFolderInfoLists(FolderListAdapter.this.account, topFolders);
                handler.newFolders(topFolders);
            }
            super.listFolders(account, folders);
        }

        @Override
        public void synchronizeMailboxStarted(Account account, String folder) {
            super.synchronizeMailboxStarted(account, folder);
            if (account.equals(FolderListAdapter.this.account)) {

                handler.progress(true);
                handler.folderLoading(folder, true);
                handler.dataChanged();
            }

        }

        @Override
        public void synchronizeMailboxFinished(Account account, String folder, int totalMessagesInMailbox, int numNewMessages) {
            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
            if (account.equals(FolderListAdapter.this.account)) {
                handler.progress(false);
                handler.folderLoading(folder, false);

                refreshFolder(account, folder);
            }

        }

        private void refreshFolder(Account account, String folderName) {
            // There has to be a cheaper way to get at the localFolder object than this
            LocalFolder localFolder = null;
            try {
                if (account != null && folderName != null) {
                    if (!account.isAvailable(activity)) {
                        Timber.i("not refreshing folder of unavailable account");
                        return;
                    }
                    localFolder = account.getLocalStore().getFolder(folderName);
                    FolderInfoHolder folderHolder = getFolder(folderName);
                    if (folderHolder != null) {
                        folderHolder.populate(activity, localFolder, FolderListAdapter.this.account, -1);
                        folderHolder.flaggedMessageCount = -1;

                        handler.dataChanged();
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "Exception while populating folder");
            } finally {
                if (localFolder != null) {
                    localFolder.close();
                }
            }

        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder, String message) {
            super.synchronizeMailboxFailed(account, folder, message);
            if (!account.equals(FolderListAdapter.this.account)) {
                return;
            }


            handler.progress(false);

            handler.folderLoading(folder, false);

            //   String mess = truncateStatus(message);

            //   handler.folderStatus(folder, mess);
            FolderInfoHolder holder = getFolder(folder);

            if (holder != null) {
                holder.lastChecked = 0;
            }

            handler.dataChanged();

        }

        @Override
        public void setPushActive(Account account, String folderName, boolean enabled) {
            if (!account.equals(FolderListAdapter.this.account)) {
                return;
            }
            FolderInfoHolder holder = getFolder(folderName);

            if (holder != null) {
                holder.pushActive = enabled;

                handler.dataChanged();
            }
        }


        @Override
        public void messageDeleted(Account account, String folder, Message message) {
            synchronizeMailboxRemovedMessage(account, folder, message);
        }

        @Override
        public void emptyTrashCompleted(Account account) {
            if (account.equals(FolderListAdapter.this.account)) {
                refreshFolder(account, FolderListAdapter.this.account.getTrashFolderName());
            }
        }

        @Override
        public void folderStatusChanged(Account account, String folderName, int unreadMessageCount) {
            if (account.equals(FolderListAdapter.this.account)) {
                refreshFolder(account, folderName);
                informUserOfStatus();
            }
        }

        @Override
        public void sendPendingMessagesCompleted(Account account) {
            super.sendPendingMessagesCompleted(account);
            if (account.equals(FolderListAdapter.this.account)) {
                refreshFolder(account, FolderListAdapter.this.account.getOutboxFolderName());
            }
        }

        @Override
        public void sendPendingMessagesStarted(Account account) {
            super.sendPendingMessagesStarted(account);

            if (account.equals(FolderListAdapter.this.account)) {
                handler.dataChanged();
            }
        }

        @Override
        public void sendPendingMessagesFailed(Account account) {
            super.sendPendingMessagesFailed(account);
            if (account.equals(FolderListAdapter.this.account)) {
                refreshFolder(account, FolderListAdapter.this.account.getOutboxFolderName());
            }
        }

        @Override
        public void accountSizeChanged(Account account, long oldSize, long newSize) {
            if (account.equals(FolderListAdapter.this.account)) {
                handler.accountSizeChanged(oldSize, newSize);
            }
        }
    };

    public FolderListAdapter(FolderList activity,
                             Account account) {
        this.activity = activity;
        this.account = account;
        this.handler = activity.getHandler();
        this.resourcesProvider = new PEpResourcesProvider(activity);
        filter = new FolderListFilter(this);
        setFolders(new ArrayList<>());
        setFilteredFolders(Collections.unmodifiableList(folders));
    }

    public List<FolderInfoHolder> getFolders() {
        return folders;
    }

    public void setFolders(List<FolderInfoHolder> folders) {
        this.folders = folders;
        filter.setFolders(folders);
    }

    void setFilteredFolders(List<FolderInfoHolder> filteredFolders) {
        this.filteredFolders = filteredFolders;
    }

    public ActivityListener getListener() {
        return listener;
    }

    public Object getItem(long position) {
        return getItem((int) position);
    }

    public Object getItem(int position) {
        return filteredFolders.get(position);
    }

    public long getItemId(int position) {
        return filteredFolders.get(position).folder.getName().hashCode();
    }

    public int getCount() {
        return filteredFolders.size();
    }

    @Override
    public boolean isEnabled(int item) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    private int getFolderIndex(String folder) {
        FolderInfoHolder searchHolder = new FolderInfoHolder();
        searchHolder.name = folder;
        return filteredFolders.indexOf(searchHolder);
    }

    public FolderInfoHolder getFolder(String folder) {
        FolderInfoHolder holder;

        int index = getFolderIndex(folder);
        if (index >= 0) {
            holder = (FolderInfoHolder) getItem(index);
            return holder;
        }
        return null;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (position <= getCount()) {
            return getItemView(position, convertView, parent);
        } else {
            Timber.e("getView with illegal position=%d called! count is only %d", position, getCount());
            return null;
        }
    }

    private View getItemView(int itemPosition, View convertView, ViewGroup parent) {
        FolderInfoHolder folder = (FolderInfoHolder) getItem(itemPosition);
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = LayoutInflater.from(activity).inflate(R.layout.folder_list_item, parent, false);
        }

        FolderViewHolder holder = (FolderViewHolder) view.getTag();


        if (holder == null) {
            holder = new FolderViewHolder();
            holder.startView(view, mFontSizes, account, resourcesProvider);
            view.setTag(holder);
        }

        if (folder == null) {
            return view;
        }

        final String folderStatusText;

        if (folder.loading) {
            folderStatusText = activity.getString(R.string.status_loading);
        } else if (folder.status != null) {
            folderStatusText = folder.status;
        } else if (folder.lastChecked != 0) {
            long now = System.currentTimeMillis();
            int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            CharSequence formattedDate;

            if (Math.abs(now - folder.lastChecked) > DateUtils.WEEK_IN_MILLIS) {
                formattedDate = activity.getString(R.string.preposition_for_date,
                        DateUtils.formatDateTime(activity, folder.lastChecked, flags));
            } else {
                formattedDate = DateUtils.getRelativeTimeSpanString(folder.lastChecked,
                        now, DateUtils.MINUTE_IN_MILLIS, flags);
            }

            folderStatusText = activity.getString(folder.pushActive
                            ? R.string.last_refresh_time_format_with_push
                            : R.string.last_refresh_time_format,
                    formattedDate);
        } else {
            folderStatusText = null;
        }

        holder.bindView(this, folderStatusText, folder);

        return view;
    }

    OnClickListener createFlaggedSearch(Account account, FolderInfoHolder folder) {
        String searchTitle =
                activity.getString(R.string.search_title,
                        activity.getString(R.string.message_list_title, account.getDescription(), folder.displayName),
                        activity.getString(R.string.flagged_modifier)
                );

        LocalSearch search = new LocalSearch(searchTitle);
        search.and(SearchField.FLAGGED, "1", Attribute.EQUALS);
        search.addAllowedFolder(folder.name);
        search.addAccountUuid(account.getUuid());

        return new FolderClickListener(search);
    }

    OnClickListener createUnreadSearch(Account account, FolderInfoHolder folder) {
        String searchTitle =
                activity.getString(R.string.search_title,
                        activity.getString(R.string.message_list_title, account.getDescription(), folder.displayName),
                        activity.getString(R.string.unread_modifier)
                );

        LocalSearch search = new LocalSearch(searchTitle);
        search.and(SearchField.READ, "1", Attribute.NOT_EQUALS);

        search.addAllowedFolder(folder.name);
        search.addAccountUuid(account.getUuid());

        return new FolderClickListener(search);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public Filter getFilter() {
        return filter;
    }

    void restorePreviousData(ArrayList<FolderInfoHolder> previousData) {
        setFolders(previousData);
        setFilteredFolders(Collections.unmodifiableList(folders));
    }

    @Override
    public void publishResults(List<FolderInfoHolder> filteredFolders) {
        setFilteredFolders(filteredFolders);
        notifyDataSetChanged();
    }
}