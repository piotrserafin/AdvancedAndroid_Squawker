package android.example.com.squawker.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.example.com.squawker.MainActivity;
import android.example.com.squawker.R;
import android.example.com.squawker.provider.SquawkContract;
import android.example.com.squawker.provider.SquawkProvider;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.lang.ref.WeakReference;
import java.util.Map;

public class SquawkFirebaseMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = SquawkFirebaseMessagingService.class.getSimpleName();

    private static final String JSON_KEY_AUTHOR = SquawkContract.COLUMN_AUTHOR;
    private static final String JSON_KEY_AUTHOR_KEY = SquawkContract.COLUMN_AUTHOR_KEY;
    private static final String JSON_KEY_MESSAGE = SquawkContract.COLUMN_MESSAGE;
    private static final String JSON_KEY_DATE = SquawkContract.COLUMN_DATE;

    private static final int NOTIFICATION_MAX_CHARACTERS = 30;
    private static final String CHANNEL_ID = "android.example.com.squawker";

    NotificationManagerCompat notificationManager;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(LOG_TAG, "RemoteMessage From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();

        if(data != null) {
            Log.d(LOG_TAG, "RemoteMessage payload: " + data);

            insertSquawk(data);
            sendNotification(data);
        }
    }

    private void insertSquawk(Map<String, String> data) {
        FirebaseMessageAsyncTask firebaseMessageAsyncTask = new FirebaseMessageAsyncTask(getApplicationContext(),data);
        firebaseMessageAsyncTask.execute();
    }

    private void sendNotification(Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Create the pending intent to launch the activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String author = data.get(JSON_KEY_AUTHOR);
        String message = data.get(JSON_KEY_MESSAGE);

        // If the message is longer than the max number of characters we want in our
        // notification, truncate it and add the unicode character for ellipsis
        if (message.length() > NOTIFICATION_MAX_CHARACTERS) {
            message = message.substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026";
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_duck)
                .setContentTitle(String.format(getString(R.string.notification_message), author))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManagerCompat notificationManager = getManager();
        notificationManager.notify(0, notification);

        //notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private NotificationManagerCompat getManager() {
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
        }
        return notificationManager;
    }

    static class FirebaseMessageAsyncTask extends AsyncTask<Void, Void, Void> {

        Map<String, String> data;
        WeakReference<Context> context;

        FirebaseMessageAsyncTask(Context context, Map<String, String> data) {
            this.data = data;
            this.context = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentValues newMessage = new ContentValues();
            newMessage.put(SquawkContract.COLUMN_AUTHOR, data.get(JSON_KEY_AUTHOR));
            newMessage.put(SquawkContract.COLUMN_MESSAGE, data.get(JSON_KEY_MESSAGE).trim());
            newMessage.put(SquawkContract.COLUMN_DATE, data.get(JSON_KEY_DATE));
            newMessage.put(SquawkContract.COLUMN_AUTHOR_KEY, data.get(JSON_KEY_AUTHOR_KEY));
            context.get().getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, newMessage);
            return null;
        }
    }
}
