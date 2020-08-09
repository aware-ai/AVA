package io.aware.ava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import static android.content.Context.NOTIFICATION_SERVICE;

public class ScreenReceiver extends BroadcastReceiver {

    private boolean screenOff;
    public static String NOTIFICATION_ID = "notification-id" ;
    public static String NOTIFICATION = "notification" ;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.i("BroadcastReceiver", "MyReceiver");

        SharedPreferences pref = context.getSharedPreferences("settings", 0);
        if(pref.getInt("lckscr",0) <= 1){
            if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                screenOff = false;
                wakeword.screenOn = true;
                Log.i("BroadcastReceiver", "Screen ON");

                if(pref.getInt("lckscr",0) == 1){
                    showNotification(context);
                }

            }
            else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                screenOff = true;
                wakeword.screenOn = false;
                Log.i("BroadcastReceiver", "Screen OFF");

            }
            Intent i = new Intent(context, BackgroundService.class);
            i.putExtra("screen_state", screenOff);
            context.startService(i);
        }
    }

    private void showNotification(Context context) {
        Log.i("BroadcastReceiver", "Send notification");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            String id = "id_product";
            // The user-visible name of the channel.
            CharSequence name = "Product";
            // The user-visible description of the channel.
            String description = "Notifications regarding our products";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            // Configure the notification channel.
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.setLightColor(Color.RED);
            notificationManager.createNotificationChannel(mChannel);

            Intent intent1 = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 123, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,"id_product")
                    .setSmallIcon(R.drawable.avalogo) //your app icon
                    .setChannelId(id)
                    .setContentTitle("Ava not running")
                    .setAutoCancel(true).setContentIntent(pendingIntent)
                    .setNumber(1)
                    .setColor(255)
                    .setContentText("Tap to open ava")
                    .setWhen(System.currentTimeMillis());
            notificationManager.notify(1, notificationBuilder.build());
        }


    }
}