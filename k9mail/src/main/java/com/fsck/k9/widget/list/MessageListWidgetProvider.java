package com.fsck.k9.widget.list;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.pEp.ui.activities.SplashActivity;
import com.fsck.k9.pEp.ui.tools.ThemeManager;
import com.fsck.k9.search.SearchAccount;


public class MessageListWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_UPDATE_MESSAGE_LIST = "UPDATE_MESSAGE_LIST";


    public static void triggerMessageListWidgetUpdate(Context context) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName widget = new ComponentName(appContext, MessageListWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widget);

        Intent intent = new Intent(context, MessageListWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_MESSAGE_LIST);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.message_list_widget_layout);

        views.setTextViewText(R.id.folder, context.getString(R.string.integrated_inbox_title));

        Intent intent = new Intent(context, MessageListWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.listView, intent);
        updateColorsFollowingTheme(context, views);

        PendingIntent viewAction = viewActionTemplatePendingIntent(context);
        views.setPendingIntentTemplate(R.id.listView, viewAction);

        boolean availableAccount = Preferences.getPreferences(context).getAvailableAccounts().size() != 0;

        if (!availableAccount) {
            PendingIntent noAccountsAction = noAccountPendingIntent(context);
            views.setOnClickPendingIntent(R.id.new_message, noAccountsAction);
            views.setOnClickPendingIntent(R.id.top_controls, noAccountsAction);
        } else {
            PendingIntent composeAction = composeActionPendingIntent(context);
            PendingIntent headerClickAction = viewUnifiedInboxPendingIntent(context);
            views.setOnClickPendingIntent(R.id.new_message, composeAction);
            views.setOnClickPendingIntent(R.id.top_controls, headerClickAction);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void updateColorsFollowingTheme(Context context, RemoteViews views) {
        int listBackground = ContextCompat.getColor(context,
                ThemeManager.isDarkTheme()
                ? R.color.dark_theme_default_background
                : R.color.white);
        views.setInt(R.id.listView, "setBackgroundColor", listBackground);
        int toolbarBackground = ContextCompat.getColor(context,
                ThemeManager.isDarkTheme()
                        ? R.color.dark_theme_overlay_1
                        : R.color.message_list_widget_header_background);
        views.setInt(R.id.top_controls, "setBackgroundColor", toolbarBackground);
        int toolbarText = ContextCompat.getColor(context,
                ThemeManager.isDarkTheme()
                        ? R.color.toolbar_content_default_dark_theme
                        : R.color.message_list_widget_header_text);
        views.setTextColor(R.id.folder, toolbarText);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (action.equals(ACTION_UPDATE_MESSAGE_LIST)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listView);
        }
    }

    private PendingIntent viewActionTemplatePendingIntent(Context context) {
        Intent intent = new Intent(context, MessageList.class);
        intent.setAction(Intent.ACTION_VIEW);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent viewUnifiedInboxPendingIntent(Context context) {
        SearchAccount unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(context);
        Intent intent = MessageList.intentDisplaySearch(
                context, unifiedInboxAccount.getRelatedSearch(), true, true, true);

        return PendingIntent.getActivity(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent composeActionPendingIntent(Context context) {
        Intent intent = new Intent(context, MessageCompose.class);
        intent.setAction(MessageCompose.ACTION_COMPOSE);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent noAccountPendingIntent(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
