package io.aware.ava;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent serviceIntent = new Intent(context, wakeword.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
