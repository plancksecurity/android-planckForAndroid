package com.fsck.k9.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.UnreadWidgetConfiguration;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.pEp.ui.activities.SplashActivity;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;

import timber.log.Timber;

public class UnreadWidgetProvider extends AppWidgetProvider {
    private static final int MAX_COUNT = 9999;

    /**
     * Trigger update for all of our unread widgets.
     *
     * @param context
     *         The {@code Context} object to use for the broadcast intent.
     */
    public static void updateUnreadCount(Context context) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);

        ComponentName thisWidget = new ComponentName(appContext, UnreadWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        Intent intent = new Intent(context, UnreadWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);

        context.sendBroadcast(intent);
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                                    int appWidgetId, String accountUuid) {
        try {

            final SearchAccount searchAccount;
            if (SearchAccount.UNIFIED_INBOX.equals(accountUuid)) {
                searchAccount = SearchAccount.createUnifiedInboxAccount(context);
            } else if (SearchAccount.ALL_MESSAGES.equals(accountUuid)) {
                searchAccount = SearchAccount.createAllMessagesAccount(context);
            } else {
                searchAccount = null;
            }

            if (searchAccount != null) {
                MessagingController controller = MessagingController.getInstance(context);
                controller.getSearchAccountStats(searchAccount, new SimpleMessagingListener() {

                    @Override
                    public void accountStatusChanged(BaseAccount account, AccountStats stats) {
                        super.accountStatusChanged(account, stats);
                        Intent clickIntent = MessageList.intentDisplaySearch(context,
                                searchAccount.getRelatedSearch(), false, true, true);
                        updateWidgetAfterStats(context, searchAccount, stats, clickIntent, appWidgetId, appWidgetManager);
                    }

                });

            } else {
                Account realAccount = Preferences.getPreferences(context).getAccount(accountUuid);
                if (realAccount != null) {
                    AccountStats stats = realAccount.getStats(context);

                    Intent clickIntent;
                    if (K9.FOLDER_NONE.equals(realAccount.getAutoExpandFolderName())) {
                        clickIntent = FolderList.actionHandleAccountIntent(context, realAccount, false);
                    } else {
                        LocalSearch search = new LocalSearch(realAccount.getAutoExpandFolderName());
                        search.addAllowedFolder(realAccount.getAutoExpandFolderName());
                        search.addAccountUuid(((BaseAccount) realAccount).getUuid());
                        clickIntent = MessageList.intentDisplaySearch(context, search, false, true,
                                true);
                    }
                    clickIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    updateWidgetAfterStats(context, realAccount, stats, clickIntent, appWidgetId, appWidgetManager);
                }

            }

        } catch (Exception e) {
            Timber.e(e, "Error getting widget configuration");
        }
    }

    private static void updateWidgetAfterStats(
            Context context,
            BaseAccount account,
            AccountStats stats,
            Intent clickIntent,
            int appWidgetId,
            AppWidgetManager appWidgetManager) {

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.unread_widget_layout);

        int unreadCount = stats != null ? stats.unreadMessageCount: 0;
        String accountName = account != null ? account.getDescription() :context.getString(R.string.app_name);

        setUnreadCount(unreadCount, remoteViews);

        remoteViews.setTextViewText(R.id.account_name, accountName);

        setClickIntent(context, clickIntent, appWidgetId);

        setAvailableAccounts(context, remoteViews, clickIntent, appWidgetId);

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private static void setUnreadCount(int unreadCount, RemoteViews remoteViews) {
        if (unreadCount <= 0) {
            // Hide TextView for unread count if there are no unread messages.
            remoteViews.setViewVisibility(R.id.unread_count, View.GONE);
        } else {
            remoteViews.setViewVisibility(R.id.unread_count, View.VISIBLE);

            String displayCount = (unreadCount <= MAX_COUNT) ?
                    String.valueOf(unreadCount) : MAX_COUNT + "+";
            remoteViews.setTextViewText(R.id.unread_count, displayCount);
        }
    }


    private static void setClickIntent(Context context, Intent clickIntent, int appWidgetId) {
        if (clickIntent == null) {
            // If the widget configuration couldn't be loaded we open the configuration
            // activity when the user clicks the widget.
            clickIntent = new Intent(context, UnreadWidgetConfiguration.class);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        }
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    }

    private static void setAvailableAccounts(Context context, RemoteViews remoteViews, Intent clickIntent, int appWidgetId) {
        boolean availableAccounts = Preferences.getPreferences(context).getAvailableAccounts().size() != 0;

        if (!availableAccounts) {
            PendingIntent noAccountsAction = noAccountPendingIntent(context);
            remoteViews.setOnClickPendingIntent(R.id.unread_widget_layout, noAccountsAction);
        } else {
            PendingIntent pendingIntent = viewUnreadInboxPendingIntent(context, appWidgetId, clickIntent);
            remoteViews.setOnClickPendingIntent(R.id.unread_widget_layout, pendingIntent);
        }
    }

    private static PendingIntent viewUnreadInboxPendingIntent(Context context, int appWidgetId, Intent clickIntent) {
        return  PendingIntent.getActivity(context, appWidgetId, clickIntent, 0);
    }

    private static PendingIntent noAccountPendingIntent(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Called when one or more widgets need to be updated.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            String accountUuid = UnreadWidgetConfiguration.getAccountUuid(context, widgetId);

            updateWidget(context, appWidgetManager, widgetId, accountUuid);
        }
    }

    /**
     * Called when a widget instance is deleted.
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            UnreadWidgetConfiguration.deleteWidgetConfiguration(context, appWidgetId);
        }
    }
}
