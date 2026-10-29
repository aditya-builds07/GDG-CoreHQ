package com.gdgcorehq.app.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.gdgcorehq.app.MemberAgendaActivity;
import com.gdgcorehq.app.MemberDashboardActivity;
import com.gdgcorehq.app.R;
import com.gdgcorehq.app.data.FirebaseDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.model.User;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging Service for GDG CoreHQ.
 * Strictly adheres to PRD §3.4, §3.6, and design.md §8.7:
 * - Handles incoming agenda notifications and 1-2 strike accountability warnings.
 * - Deep-links agendas to MemberAgendaActivity and strikes to MemberDashboardActivity.
 * - Automatically registers and updates device FCM tokens in Firestore /users/{uid}.
 */
public class CoreHQMessagingService extends FirebaseMessagingService {

    public static final String CHANNEL_AGENDAS = "gdg_channel_agendas";
    public static final String CHANNEL_STRIKES = "gdg_channel_strikes";
    public static final String CHANNEL_GENERAL = "gdg_channel_general";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            FirebaseDataRepository.getInstance().updateUserFcmToken(currentUser.getId(), token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        createNotificationChannels();

        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        Map<String, String> data = remoteMessage.getData();
        if (title == null && data.containsKey("title")) {
            title = data.get("title");
        }
        if (body == null && data.containsKey("body")) {
            body = data.get("body");
        }

        if (title == null) title = "GDG CoreHQ";
        if (body == null) body = "You have a new update.";

        String type = data.containsKey("type") ? data.get("type") : "general";

        Intent intent;
        String channelId;
        int iconRes;

        if ("agenda".equalsIgnoreCase(type)) {
            channelId = CHANNEL_AGENDAS;
            iconRes = R.drawable.ic_calendar;
            intent = new Intent(this, MemberAgendaActivity.class);
            if (data.containsKey("meetingId")) {
                intent.putExtra("meetingId", data.get("meetingId"));
            }
        } else if ("strike".equalsIgnoreCase(type)) {
            channelId = CHANNEL_STRIKES;
            iconRes = R.drawable.ic_warning;
            intent = new Intent(this, MemberDashboardActivity.class);
        } else {
            channelId = CHANNEL_GENERAL;
            iconRes = R.drawable.ic_dashboard;
            intent = new Intent(this, MemberDashboardActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            // Agendas channel
            NotificationChannel agendaChannel = new NotificationChannel(
                    CHANNEL_AGENDAS,
                    "Meeting Agendas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            agendaChannel.setDescription("Notifications for newly published chapter meeting agendas");
            manager.createNotificationChannel(agendaChannel);

            // Strikes channel
            NotificationChannel strikeChannel = new NotificationChannel(
                    CHANNEL_STRIKES,
                    "Accountability Warnings",
                    NotificationManager.IMPORTANCE_HIGH
            );
            strikeChannel.setDescription("Accountability warnings for missed meetings or overdue tasks");
            manager.createNotificationChannel(strikeChannel);

            // General channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General GDG CoreHQ chapter announcements");
            manager.createNotificationChannel(generalChannel);
        }
    }
}
