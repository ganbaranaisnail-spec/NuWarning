package com.example.nuwarning;

import static com.example.nuwarning.StateView.STATE_MONITORING;
import static com.example.nuwarning.StateView.STATE_MONITORING_STOP;
import static com.example.nuwarning.Util.APP_SETTINGS;
import static com.example.nuwarning.Util.APP_SETTINGS_KEY_STATE;
import static com.example.nuwarning.Util.PREFERENCES_NOTIFY;
import static com.example.nuwarning.Util.containsString;
import static com.example.nuwarning.Util.getDateSt;
import static com.example.nuwarning.Util.isAppTC;
import static com.example.nuwarning.Util.sLogd;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import android.content.pm.ServiceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SNotificationListenerService extends NotificationListenerService {

    private static final String CHANNEL_ID = "foreground_service_channel";
    private static final int NOTIFICATION_ID = 105139;
    private static final String TAG = "MyNotificationListener";

    public static final String ACTION_STOP_FOREGROUND = "com.example.nuwarning.ACTION_STOP_FOREGROUND";

    //追加。フォアグラウンドサービスタイプを設定する。
    private static final int FOREGROUND_SERVICE_TYPE = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC | ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

    private ComponentName componentName = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // バックグラウンドスレッド用のExecutorService
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // UIスレッドで実行するためのHandler

    public static final String CONECTED_TITLE = "配信開始通知を監視します";
    public static final String CONECTED_TEXT = "キーワードを検知した場合、警告音を鳴らし続けます。\n" +
            "アプリを維持するために、この通知は消さないで下さい。\n" +
            "サイレントモードで使用する場合、サイレントモードでのアラームをオンにして下さい。";

    private int alarmNotificationId = -1;

    private static final boolean isDebug = false;

    private final List<AlarmWorkerInfo> alarmWorkers = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        sLogd(TAG, "MyNotificationListenerService onCreate", this);
        // フォアグラウンドサービス開始
        startForegroundService();
    }

    private void startForegroundService() {
        // 通知の作成
        Notification notification = createForegroundNotification("配信開始通知を監視する準備をしています");

        // フォアグラウンドサービス開始 (Android 8.0 以降)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 14以降は引数を指定する必要がある。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        }
    }

    private Notification createForegroundNotification(String text) {
        // 通知チャンネルの作成 (Android O 以上)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "通知監視サービス",
                    NotificationManager.IMPORTANCE_LOW // 重要度 (LOW, DEFAULT, HIGH など)
            );
            channel.setDescription("通知を監視しています");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 通知の作成
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("配信開始監視準備中")
                .setContentText(text)
                .setSmallIcon(R.drawable.nuwa_icon) // 通知アイコン (適切なアイコンに置き換えてください)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 通知の優先度
                .setOngoing(true) // 常駐通知
                .build();
    }

    //通知発生時の通知音とロック画面での表示を追加したい
    private void updateNotification(String title, String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.nuwa_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sLogd(TAG, "onStartCommand_start", this);

        if (ACTION_STOP_FOREGROUND.equals(intent.getAction())) {
            stopAlarm(-1);
            stopSelf();
        }

        // componentName が null の場合にインスタンスを作成
        if (componentName == null) {
            sLogd(TAG, "onStartCommand_componentName == null", this);
            componentName = new ComponentName(this, SNotificationListenerService.class);

            requestRebind(componentName); // 通知リスナーサービスに再バインド
            sLogd(TAG, "onStartCommand_requestRebind", this);
            toggleNotificationListenerService(componentName); // サービスを有効/無効に切り替える

            sLogd(TAG, "onStartCommand_end", this);
            // 次回インテントを再送するために START_REDELIVER_INTENT を返す
            return START_REDELIVER_INTENT;

        }

        sLogd(TAG, "onStartCommand_end", this);
        // 次回インテントを再送するために START_REDELIVER_INTENT を返す
//        return START_REDELIVER_INTENT;
        return START_STICKY;

    }

    // 通知リスナーサービスを有効/無効に切り替えるメソッド
    private void toggleNotificationListenerService(ComponentName componentName) {
        sLogd(TAG, "toggleNotificationListenerService_start", this);
        executorService.submit(() -> {
            PackageManager pm = getPackageManager();
            // サービスを無効化
            pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
            // サービスを再度有効化
            pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
            // UIスレッドでログ出力
            mainHandler.post(() -> sLogd(TAG, "toggleNotificationListenerService_end", this));

            updateNotification(CONECTED_TITLE, CONECTED_TEXT);

        });
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        updateNotification(CONECTED_TITLE, CONECTED_TEXT);

        sLogd(TAG, "MyNotificationListenerService onListenerConnected", this);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        // リスナーが切断されたときに呼ばれる
        sLogd("MyNotificationListener", "Listener Disconnected", this);

        // 再バインドを試みる
        ComponentName componentName = new ComponentName(getApplicationContext(), SNotificationListenerService.class);
        requestRebind(componentName);  // ここで再バインドをリクエスト

        sLogd("MyNotificationListener", "Listener Disconnected_end", this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if(!Util.isAppAlive(this)){
            Log.d(TAG, "onNotificationPosted: isAppAlive false");
            return;
        }

        executorService.submit(() -> {
            String packageName = sbn.getPackageName();
            Util.sLogd(TAG, "Notification from package: " + packageName, this);

            if ("com.google.android.youtube".equals(packageName)) {
                // ... (既存の処理)
                Notification notification = sbn.getNotification();
                saveNotificationData("Youtube", sbn.getId(), notification);
                String notificationText = notification.extras.getCharSequence(Notification.EXTRA_TEXT, "No text available").toString();
                String notificationTitle = notification.extras.getCharSequence(Notification.EXTRA_TITLE, "null").toString();
                sLogd(TAG, "YouTube notificationTitle: " + notificationTitle, this);
                sLogd(TAG, "YouTube chime.thread_id: " + getExVal(notification, "chime.thread_id"), this);

                if (getState() == STATE_MONITORING
                        && containsString(getExVal(notification, "chime.thread_id"), "STREAM")
                        && (containsKeyword(notificationTitle) || containsKeyword(notificationText))) {
                    sLogd(TAG, "YouTube : STREAM" + notificationTitle + ":" + notificationText, this);
                    // アラームを開始
                    startAlarm(sbn.getId());
                }

            } else if ("com.twitter.android".equals(packageName)) {
                // ... (既存の処理)
                Notification notification = sbn.getNotification();
                saveNotificationData("X", sbn.getId(), notification);
                String notificationTitle = notification.extras.getCharSequence(Notification.EXTRA_TITLE,"null").toString();
                String notificationText = notification.extras.getCharSequence(Notification.EXTRA_TEXT, "No text available").toString();

                sLogd(TAG, "X notification: " + notificationText + " :title:" + notificationTitle, this);

                if (getState() == STATE_MONITORING
                        && containsString(notificationText, "x.com/i/spaces/")
                        && containsKeyword(notificationTitle)) {
                    // アラームを開始
                    startAlarm(sbn.getId());
                }

                //テスト用
                if (isDebug && containsString(notificationTitle,"さりげなくピース")){
                    // アラームを開始
                    startAlarm(sbn.getId());
                }
            } else if ("com.sidefeed.TCViewer".equals(packageName)) {
                // ... (既存の処理)
                Notification notification = sbn.getNotification();
                saveNotificationData("TCViewer", sbn.getId(), notification);
                String notificationText = notification.extras.getCharSequence(Notification.EXTRA_TEXT, "No text available").toString();
                String notificationTitle = notification.extras.getCharSequence(Notification.EXTRA_TITLE,"null").toString();
                String notificationId = notification.extras.getString("mChannelId", "No text available");

                sLogd(TAG, "TCViewer notification.extras: " + notification.extras, this);

                sLogd(TAG, "TCViewer notification: " + notificationText, this);

                if (getState() == STATE_MONITORING
                        && isAppTC(getApplicationContext())
                        && (containsString(notificationId, "live_start") || containsKeyword(notificationText) || containsKeyword(notificationTitle))) {
                    // アラームを開始
                    startAlarm(sbn.getId());
                }

                // ...
//                startAlarm(sbn.getId());//警告音テスト
            } else {
                Notification notification = sbn.getNotification();
                saveNotificationData("UnKnown", sbn.getId(), notification);
            }
        });
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        // 通知が削除されたときの処理

        String packageName = sbn.getPackageName();
        int notificationId = sbn.getId();  // 削除された通知の ID
        sLogd(TAG, "Notification removed from package: " + packageName + ", ID: " + notificationId, this);

        // 削除された通知のIDを引数に渡してアラームを停止
        if(AlarmWorker.NOTIFICATION_ID == sbn.getNotification().extras.getInt(AlarmWorker.STOP_ID_KEY,-1)){
            stopAlarm(notificationId);
        }

    }

    private void stopAlarm(int notificationId) {
        // WorkManager を使用して AlarmWorker を停止
//        WorkManager.getInstance(this).cancelUniqueWork("alarm_work_" + notificationId);
//        sLogd(TAG, "Alarm stopped for notification ID: " + notificationId, this);
        // 全てのアラームを停止
        for (AlarmWorkerInfo info : alarmWorkers) {
            WorkManager.getInstance(this).cancelUniqueWork(info.workerName);
            sLogd(TAG, "Alarm stopped in onDestroy: " + info.workerName, this);
        }
        alarmWorkers.clear(); // リストをクリア
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        sLogd(TAG, "MyNotificationListenerService onDestroy_start", this);

        // 全てのアラームを停止
        for (AlarmWorkerInfo info : alarmWorkers) {
            WorkManager.getInstance(this).cancelUniqueWork(info.workerName);
            sLogd(TAG, "Alarm stopped in onDestroy: " + info.workerName, this);
        }
        alarmWorkers.clear(); // リストをクリア

        // フォアグラウンドサービスを停止
        stopForeground(STOP_FOREGROUND_REMOVE);

        sLogd(TAG, "MyNotificationListenerService onDestroy_end", this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        sLogd(TAG, "MyNotificationListenerService onTaskRemoved", this);

        // 全てのアラームを停止
        stopAlarm(-1); // -1 は全てのアラームを停止するための特別な値

        // フォアグラウンドサービスを停止
        stopForeground(STOP_FOREGROUND_REMOVE);

        // フォアグラウンド通知をキャンセル
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFICATION_ID); // ここで通知をキャンセル

        // サービスの停止
        stopSelf();
    }

    private void startAlarm(int notificationId) {
        String workerName = "alarm_work_" + notificationId;

        // 既存のアラームをキャンセル
        WorkManager.getInstance(this).cancelUniqueWork(workerName);

        // アラームWorkerをスケジュール
        OneTimeWorkRequest alarmWorkRequest = new OneTimeWorkRequest.Builder(AlarmWorker.class)
                .setInputData(
                        new androidx.work.Data.Builder()
                                .putInt(AlarmWorker.KEY_NOTIFICATION_ID, notificationId)
                                .build()
                )
                .addTag(workerName) // タグを設定
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                workerName, // 名前を設定
                ExistingWorkPolicy.REPLACE, // 同じ名前のWorkerがあればキャンセルして置き換える
                alarmWorkRequest
        );

        // リストに情報を追加
        alarmWorkers.add(new AlarmWorkerInfo(notificationId, workerName));
    }

    private void saveNotificationData(String source, int notificationId, Notification notification) {
       if(!isDebug){
           return;
       }

        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // GsonBuilder を使用
        String jsonNotification = gson.toJson(notification);

        getSharedPreferences(PREFERENCES_NOTIFY, MODE_PRIVATE).edit()
                .putString(source + ":" + "notification_" + notificationId + ":" + Util.getDateSt(), jsonNotification)
                .apply();
    }

    private boolean containsKeyword(String notificationText) {
        List<String> keywords = getKeywordList();
        for (String keyword : keywords) {
            if (notificationText.contains(keyword)) {
                return true; // キーワードが見つかったら true を返す
            }
        }
        return false; // どのキーワードも見つからなかったら false を返す
    }
    private List<String> getKeywordList() {
        SharedPreferences prefs = getSharedPreferences("keyword_prefs", MODE_PRIVATE);
        String keywordsJson = prefs.getString("keywords", "");
        List<String> keywordList = new ArrayList<>();
        if (!keywordsJson.isEmpty()) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<Keyword>>(){}.getType();
                List<Keyword> keywords = gson.fromJson(keywordsJson, type);
                for (Keyword keyword : keywords) {
                    if (keyword.isChecked()) { // isChecked() メソッドを使用
                        keywordList.add(keyword.getName()); // getName() メソッドを使用
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse keywords JSON: " + e.getMessage());
            }
        }
        return keywordList;
    }

    private int getState(){
        SharedPreferences prefs = getSharedPreferences(APP_SETTINGS, MODE_PRIVATE);
        return prefs.getInt(APP_SETTINGS_KEY_STATE, STATE_MONITORING_STOP); //
    }


    public String getExVal(Notification notification, String key) {
        Bundle extras = notification.extras;

        String exVal = "null";
        if (extras != null && extras.containsKey(key)) {
            exVal = extras.getString(key);
        }

        return exVal;
    }

    @Override
    public IBinder onBind(Intent intent) {
        sLogd(TAG, "MyNotificationListener.onBind", this);
        return super.onBind(intent);
    }

    private static class AlarmWorkerInfo {
        int notificationId; // 監視対象の通知 ID
        String workerName; // AlarmWorker の名前 ("alarm_work_" + notificationId)

        AlarmWorkerInfo(int notificationId, String workerName) {
            this.notificationId = notificationId;
            this.workerName = workerName;
        }
    }
}