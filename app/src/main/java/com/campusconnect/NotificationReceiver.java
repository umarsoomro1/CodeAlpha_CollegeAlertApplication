package com.campusconnect;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "campus_connect_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("event_title");
        String eventId = intent.getStringExtra("event_id");

        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Campus Connect", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Upcoming Event")
                .setContentText(title + " is coming up!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(eventId.hashCode(), builder.build());

        // Delete event from Firestore
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Event deleted
                })
                .addOnFailureListener(e -> {
                    // Handle failure if needed
                });
    }
}