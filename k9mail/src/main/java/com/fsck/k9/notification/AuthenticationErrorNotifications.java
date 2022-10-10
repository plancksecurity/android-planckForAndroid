package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;


import static com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE;
import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_FAILURE_COLOR;


class AuthenticationErrorNotifications {
    private final NotificationController controller;


    public AuthenticationErrorNotifications(NotificationController controller) {
        this.controller = controller;
    }

    public void showAuthenticationErrorNotification(Account account, boolean incoming) {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming);
        Context context = controller.getContext();

        PendingIntent editServerSettingsPendingIntent = createContentIntent(context, account, incoming);
        String title = context.getString(R.string.notification_authentication_error_title);
        String text = context.getString(R.string.notification_authentication_error_text, account.getDescription());

        NotificationCompat.Builder builder = controller
                .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
                .setSmallIcon(R.drawable.notification_icon_warning)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(editServerSettingsPendingIntent)
                .setStyle(new BigTextStyle().bigText(text))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ERROR);

        controller.configureNotification(builder, null, null,
                NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        getNotificationManager().notify(notificationId, builder.build());
    }

    public void clearAuthenticationErrorNotification(Account account, boolean incoming) {
        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, incoming);
        getNotificationManager().cancel(notificationId);
    }

    PendingIntent createContentIntent(Context context, Account account, boolean incoming) {
        Intent editServerSettingsIntent = incoming ?
                AccountSetupBasics.intentActionEditIncomingSettings(context, account.getUuid()) :
                AccountSetupBasics.intentActionEditOutgoingSettings(context, account.getUuid());

        return PendingIntent.getActivity(context, account.getAccountNumber(), editServerSettingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    private NotificationManagerCompat getNotificationManager() {
        return controller.getNotificationManager();
    }
}
