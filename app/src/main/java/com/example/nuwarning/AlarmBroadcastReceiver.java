package com.example.nuwarning;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.WorkManager;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AlarmWorker.ACTION_STOP_ALARM.equals(intent.getAction())) {
            int notificationId = intent.getIntExtra(AlarmWorker.KEY_NOTIFICATION_ID, -1);
            Log.d(TAG, "Received stop alarm request for notification ID: " + notificationId);
            if (notificationId != -1) {
                // アラームを停止する処理
                String workName = "alarm_work_" + notificationId;
                WorkManager.getInstance(context).cancelUniqueWork(workName);
                Log.d(TAG, "Cancelled work: " + workName);
                // 通知を消す
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationId);
            }
        }
    }

}