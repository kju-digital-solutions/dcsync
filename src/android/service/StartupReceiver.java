package at.kju.datacollector.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Created by Leo on 28.01.2016.
 */
public class StartupReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

       SyncService.Schedule(context);
    }
}
