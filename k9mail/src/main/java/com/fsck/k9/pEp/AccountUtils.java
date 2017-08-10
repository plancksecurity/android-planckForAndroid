package com.fsck.k9.pEp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.Preferences;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.search.ConditionsTreeNode;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SqlQueryBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AccountUtils {

    private final ThreadExecutor threadExecutor;
    private final PostExecutionThread postExecutionThread;

    @Inject public AccountUtils(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread) {
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = postExecutionThread;
    }

    public void loadSearchAccountStats(Context context,
                                       SearchAccount searchAccount,
                                       AccountStatsCallback callback) {
        threadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                getSearchAccountStatsSynchronous(context, searchAccount, callback);
            }
        });
    }

    public AccountStats getSearchAccountStatsSynchronous(Context context,
                                                         SearchAccount searchAccount,
                                                         AccountStatsCallback callback) {

        Preferences preferences = Preferences.getPreferences(context);
        LocalSearch search = searchAccount.getRelatedSearch();

        // Collect accounts that belong to the search
        String[] accountUuids = search.getAccountUuids();
        List<Account> accounts;
        if (search.searchAllAccounts()) {
            accounts = preferences.getAccounts();
        } else {
            accounts = new ArrayList<>(accountUuids.length);
            for (int i = 0, len = accountUuids.length; i < len; i++) {
                String accountUuid = accountUuids[i];
                accounts.set(i, preferences.getAccount(accountUuid));
            }
        }

        ContentResolver cr = context.getContentResolver();

        int unreadMessageCount = 0;
        int flaggedMessageCount = 0;

        String[] projection = {
                EmailProvider.StatsColumns.UNREAD_COUNT,
                EmailProvider.StatsColumns.FLAGGED_COUNT
        };

        for (Account account : accounts) {
            StringBuilder query = new StringBuilder();
            List<String> queryArgs = new ArrayList<>();
            ConditionsTreeNode conditions = search.getConditions();
            SqlQueryBuilder.buildWhereClause(account, conditions, query, queryArgs);

            String selection = query.toString();
            String[] selectionArgs = queryArgs.toArray(new String[queryArgs.size()]);

            Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI,
                    "account/" + account.getUuid() + "/stats");

            // Query content provider to get the account stats
            Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    unreadMessageCount += cursor.getInt(0);
                    flaggedMessageCount += cursor.getInt(1);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        // Create AccountStats instance...
        AccountStats stats = new AccountStats();
        stats.unreadMessageCount = unreadMessageCount;
        stats.flaggedMessageCount = flaggedMessageCount;

        // ...and notify the listener
        this.postExecutionThread.post(() -> callback.accountStatusChanged(searchAccount, stats));

        return stats;
    }
}
