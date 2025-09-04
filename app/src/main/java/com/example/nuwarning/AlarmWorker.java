package com.example.nuwarning;

import static com.example.nuwarning.MainActivity.getFullScreenIntent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;

public class AlarmWorker extends Worker {

    public static final int NOTIFICATION_ID = 1051;
    public static final String STOP_ID_KEY = "sukisuki_tenka_an";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final String TAG = "AlarmWorker";
    public static final String ACTION_STOP_ALARM = "com.example.nuwarning.ACTION_STOP_ALARM";
    public static final String KEY_NOTIFICATION_ID = "notification_id";
    public static final String KEY_ALARM_NOTIFICATION_ID = "alarm_notification_id";

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private int originalVolume;

    public AlarmWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        int notificationId = getInputData().getInt(KEY_NOTIFICATION_ID, -1); // デフォルト値を -1 に変更

        if (notificationId == -1) { // notificationId が -1 の場合にエラーとする
            Log.e(TAG, "Notification ID not provided");
            return Result.failure();
        }

        // フォアグラウンド通知の設定
//        setForegroundAsync(createForegroundInfo(context, notificationId));

        startAlarm(context, notificationId); // notificationId を渡す

        // アラーム停止を待機
        while (!isStopped()) {
            try {
                Thread.sleep(1000); // 1秒ごとに停止状態を確認
            } catch (InterruptedException e) {
                Log.e(TAG, "Sleep interrupted", e);
                return Result.failure();
            }
        }

        stopAlarm();
        // 修正箇所: outputData を設定して Result.success() を返す
        Data outputData = new Data.Builder()
                .putInt(KEY_ALARM_NOTIFICATION_ID, NOTIFICATION_ID)
                .build();
        return Result.success(outputData);
    }

    private void startAlarm(Context context, int notificationId) {
        // 最大音量を保存
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        // アラーム音量を最大に設定
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

        mediaPlayer = new MediaPlayer();
        try {
            // デフォルトの着信音を使用
            Uri alarmUri = Settings.System.DEFAULT_RINGTONE_URI;
            mediaPlayer.setDataSource(context, alarmUri);

            // AudioAttributes を使用してアラームストリームを指定
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mediaPlayer.setAudioAttributes(audioAttributes);

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting alarm", e);
            // エラーが発生した場合はデフォルトの通知音で代用
            mediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_NOTIFICATION_URI);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else {
                Log.e(TAG, "Failed to play default notification sound");
                return;
            }
        }

        // 停止ボタン付きの通知を表示
        showAlarmNotification(context,notificationId);
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // 音量を元に戻す
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
    }


    private void showAlarmNotification(Context context, int notificationId) {
        // 停止ボタンの PendingIntent を作成
        Intent stopIntent = new Intent(context, AlarmBroadcastReceiver.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        stopIntent.putExtra(KEY_NOTIFICATION_ID, notificationId); // notificationId を extra に設定

        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, notificationId, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 通知の作成
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.nuwa_icon)
                .setContentTitle("ヌヮ―ニング！！！")
                .setContentText("配信が始まっています！")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
//                .setFullScreenIntent(getFullScreenIntent(context), true)
                .addAction(android.R.drawable.ic_media_pause, "停止", stopPendingIntent);

        // 通知チャンネルの作成（Android O 以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "アラーム通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("アラーム音を鳴らす通知");
            channel.setSound(null, null); // 通知音を無効にする
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // 通知の表示
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(notificationId, builder.build());
    }
    private void showAlarmNotificationB(Context context) {
        createNotificationChannel(context);

        // 停止ボタンの PendingIntent
        Intent stopIntent = new Intent(context, AlarmBroadcastReceiver.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        stopIntent.putExtra(KEY_NOTIFICATION_ID, getInputData().getInt(KEY_NOTIFICATION_ID, 0)); // Workerに紐づく通知IDを渡す
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Bundle extras = new Bundle();
        extras.putInt(STOP_ID_KEY,NOTIFICATION_ID);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.nuwa_icon)
                .setContentTitle("ヌヮ―ニング！！！")
                .setContentText("配信が始まっています！")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(false) // ユーザーがスワイプして消せないようにする(一旦falseで)
                .setExtras(extras)
                .addAction(R.drawable.nuwa_icon, "停止", stopPendingIntent)
                .build();

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "アラーム通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("アラームを停止するための通知");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private ForegroundInfo createForegroundInfo(Context context, int notificationId) {
        createNotificationChannel(context);

        Intent stopIntent = new Intent(context, AlarmBroadcastReceiver.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        stopIntent.putExtra(KEY_NOTIFICATION_ID, notificationId);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.nuwa_icon)
                .setContentTitle("アラーム")
                .setContentText("アラームが鳴っています")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .addAction(R.drawable.nuwa_icon, "停止", stopPendingIntent)
                .build();

        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

}

