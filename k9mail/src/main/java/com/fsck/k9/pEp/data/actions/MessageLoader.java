package com.fsck.k9.pEp.data.actions;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.fragment.MessageListFragmentComparators;
import com.fsck.k9.helper.MergeCursorWithUniqueId;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.data.executor.JobExecutor;
import com.fsck.k9.pEp.data.executor.PostExecutionThread;
import com.fsck.k9.pEp.data.executor.ThreadExecutor;
import com.fsck.k9.pEp.data.models.Message;
import com.fsck.k9.pEp.ui.infrastructure.UIThread;

import org.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MessageLoader implements MessagesLoaderAction {

    private static final int ID_COLUMN = 0;
    private static final int UID_COLUMN = 1;
    static final int INTERNAL_DATE_COLUMN = 2;
    private static final int SUBJECT_COLUMN = 3;
    private static final int DATE_COLUMN = 4;
    private static final int SENDER_LIST_COLUMN = 5;
    private static final int TO_LIST_COLUMN = 6;
    private static final int CC_LIST_COLUMN = 7;
    private static final int READ_COLUMN = 8;
    private static final int FLAGGED_COLUMN = 9;
    private static final int ANSWERED_COLUMN = 10;
    private static final int FORWARDED_COLUMN = 11;
    private static final int ATTACHMENT_COUNT_COLUMN = 12;
    private static final int FOLDER_ID_COLUMN = 13;
    private static final int PREVIEW_TYPE_COLUMN = 14;
    private static final int PREVIEW_COLUMN = 15;
    private static final int THREAD_ROOT_COLUMN = 16;
    private static final int ACCOUNT_UUID_COLUMN = 17;
    private static final int FOLDER_NAME_COLUMN = 18;
    private static final int PEP_RATING_COLUMN = 19;
    private static final int THREAD_COUNT_COLUMN = 20;

    private static final Map<Account.SortType, Comparator<Cursor>> SORT_COMPARATORS;

    static {
        // fill the mapping at class time loading

        final Map<Account.SortType, Comparator<Cursor>> map =
                new EnumMap<>(Account.SortType.class);
        map.put(Account.SortType.SORT_ATTACHMENT, new MessageListFragmentComparators.AttachmentComparator());
        map.put(Account.SortType.SORT_DATE, new MessageListFragmentComparators.DateComparator());
        map.put(Account.SortType.SORT_ARRIVAL, new MessageListFragmentComparators.ArrivalComparator());
        map.put(Account.SortType.SORT_FLAGGED, new MessageListFragmentComparators.FlaggedComparator());
        map.put(Account.SortType.SORT_SUBJECT, new MessageListFragmentComparators.SubjectComparator());
        map.put(Account.SortType.SORT_SENDER, new MessageListFragmentComparators.SenderComparator());
        map.put(Account.SortType.SORT_UNREAD, new MessageListFragmentComparators.UnreadComparator());

        // make it immutable to prevent accidental alteration (content is immutable already)
        SORT_COMPARATORS = Collections.unmodifiableMap(map);
    }

    private PostExecutionThread postExecutionThread;
    private Context context;
    private Boolean mThreadedList;
    private Account.SortType mSortType = Account.SortType.SORT_DATE;
    private Cursor[] mCursors;
    private boolean[] mCursorValid;
    private Callback<Message> callback;
    private CursorLoader cursorLoader;
    private Preferences mPreferences;

    public void getMessages(Context context, Callback<Message> callback, Boolean mThreadedList, Cursor[] mCursors, boolean[] mCursorValid, CursorLoader cursorLoader, Preferences mPreferences) {
        ThreadExecutor threadExecutor = new JobExecutor();
        postExecutionThread = new UIThread();
        this.callback = callback;
        this.context = context;
        this.mThreadedList = mThreadedList;
        this.mCursors = mCursors;
        this.mCursorValid = mCursorValid;
        this.cursorLoader = cursorLoader;
        this.mPreferences = mPreferences;
        threadExecutor.execute(this);
    }

    @Override
    public void run() {
        obtainMessages();
    }

    private void obtainMessages() {
        List<Message> messages = new ArrayList<>();
        Cursor data = cursorLoader.loadInBackground();

        final int loaderId = cursorLoader.getId();

        mCursors[loaderId] = data;
        mCursorValid[loaderId] = true;

        Cursor cursor;
        int mUniqueIdColumn;
        if (mCursors.length > 1) {
            cursor = new MergeCursorWithUniqueId(mCursors, getComparator());
            mUniqueIdColumn = cursor.getColumnIndex("_id");
        } else {
            cursor = data;
            mUniqueIdColumn = ID_COLUMN;
        }

        while (cursor.moveToNext()) {
            mapCursorToMessage(messages, cursor, mUniqueIdColumn);
        }
        notifyLoaded(messages);
    }

    private void mapCursorToMessage(List<Message> messages, Cursor cursor, int mUniqueIdColumn) {
        String fromList = cursor.getString(SENDER_LIST_COLUMN);
        String toList = cursor.getString(TO_LIST_COLUMN);
        String ccList = cursor.getString(CC_LIST_COLUMN);

        Address[] fromAddrs = Address.unpack(fromList);
        Address[] toAddrs = Address.unpack(toList);
        Address[] ccAddrs = Address.unpack(ccList);
        Rating pEpRating = Rating.valueOf(cursor.getString(PEP_RATING_COLUMN));
        CharSequence displayDate = DateUtils.getRelativeTimeSpanString(context, cursor.getLong(DATE_COLUMN));
        int threadCount = (mThreadedList) ? cursor.getInt(THREAD_COUNT_COLUMN) : 0;
        String subject = cursor.getString(SUBJECT_COLUMN);
        boolean read = (cursor.getInt(READ_COLUMN) == 1);
        boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);
        boolean answered = (cursor.getInt(ANSWERED_COLUMN) == 1);
        boolean forwarded = (cursor.getInt(FORWARDED_COLUMN) == 1);
        boolean hasAttachments = (cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0);
        long uniqueId = cursor.getLong(mUniqueIdColumn);
        int position = cursor.getPosition();
        String uid = cursor.getString(UID_COLUMN);
        String folderName = cursor.getString(FOLDER_NAME_COLUMN);
        String previewTypeString = cursor.getString(PREVIEW_TYPE_COLUMN);
        String preview = cursor.getString(PREVIEW_COLUMN);

        long folderId = cursor.getLong(FOLDER_ID_COLUMN);
        long rootId = cursor.getLong(THREAD_ROOT_COLUMN);
        Account account = mPreferences.getAccount(cursor.getString(ACCOUNT_UUID_COLUMN));

        Message message = new Message();
        message.setFrom(fromAddrs);
        message.setTo(toAddrs);
        message.setCC(ccAddrs);
        message.setPEpRating(pEpRating);
        message.setDisplayDate(displayDate);
        message.setThreadCount(threadCount);
        message.setSubject(subject);
        message.setRead(read);
        message.setFlagged(flagged);
        message.setAnswered(answered);
        message.setForwarded(forwarded);
        message.setHasAttachments(hasAttachments);
        message.setUniqueId(uniqueId);
        message.setUid(uid);
        message.setPosition(position);
        message.setFolderName(folderName);
        message.setPreviewTypeString(previewTypeString);
        message.setPreview(preview);
        message.setFolderId(folderId);
        message.setRootId(rootId);
        message.setAccount(account);

        messages.add(message);
    }

    private void notifyLoaded(final List<Message> messages) {
        this.postExecutionThread.post(new Runnable() {
            @Override
            public void run() {
                callback.onLoaded(messages);
            }
        });
    }

    protected Comparator<Cursor> getComparator() {
        final List<Comparator<Cursor>> chain =
                new ArrayList<>(3 /* we add 3 comparators at most */);
        // Add the specified comparator
        final Comparator<Cursor> comparator = SORT_COMPARATORS.get(mSortType);
        chain.add(comparator);

        // Add the date comparator if not already specified
        if (mSortType != Account.SortType.SORT_DATE && mSortType != Account.SortType.SORT_ARRIVAL) {
            final Comparator<Cursor> dateComparator = SORT_COMPARATORS.get(Account.SortType.SORT_DATE);
            chain.add(new MessageListFragmentComparators.ReverseComparator<>(dateComparator));
        }

        // Add the id comparator
        chain.add(new MessageListFragmentComparators.ReverseIdComparator());

        // Build the comparator chain
        return new MessageListFragmentComparators.ComparatorChain<>(chain);
    }
}
