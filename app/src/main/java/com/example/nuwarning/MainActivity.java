package com.example.nuwarning;

import static com.example.nuwarning.Util.APP_SETTINGS;
import static com.example.nuwarning.Util.sLogd;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.app.NotificationManager;
import android.Manifest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements KeywordAdapter.OnKeywordInteractionListener {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private KeywordAdapter keywordAdapter;
    private StateView stateView;
    private MyViewModel myViewModel;
    private AlertDialog permissionDialog = null;
    private static final String PREFS_NAME = "keyword_prefs";
    private static final String KEY_KEYWORDS = "keywords";
    private static final String INIT_KEYWORDS = "チャンネル名";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        sLogd(TAG, "onCreate: start",this);

        setContentView(R.layout.activity_main);

        // RecyclerViewの設定
        recyclerView = findViewById(R.id.keywordsRecyclerView);
        keywordAdapter = new KeywordAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(keywordAdapter);

        // フローティングアクションボタンの設定
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> onAddKeyword());

        ImageButton settingButton = findViewById(R.id.settingButton); // ImageButton の ID を指定
        settingButton.setOnClickListener(v -> {
            // SettingActivity への Intent を作成
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            // Activity を開始
            startActivity(intent);
        });

        // 初回起動フラグのチェックと設定
        SharedPreferences preferences = getSharedPreferences(APP_SETTINGS, MODE_PRIVATE);
        boolean isFirstLaunch = preferences.getBoolean("isFirstLaunch", true);
        if (isFirstLaunch) {
            // 初回起動時の処理
            List<Keyword> initialKeywords = new ArrayList<>();
            initialKeywords.add(new Keyword(INIT_KEYWORDS)); // チェックされた状態
            keywordAdapter.setKeywordList(initialKeywords);
            saveKeywords(); // 初期データを保存
            // 初回起動フラグを false に設定
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstLaunch", false);
            editor.apply();
        }

        // 保存されたキーワードを読み込む
        loadKeywords();

        Util.saveAppIsAlive(true,getApplicationContext());

        // StateViewのインスタンスを取得
        stateView = findViewById(R.id.stateView);
        keywordAdapter.setStateView(stateView);

        // 通知権限が許可されているか確認
        if (!isNotificationPermissionGranted()) {
            stateView.setState(StateView.STATE_REQUEST_PERMISSION);
            requestNotificationPermission();
        }else if (keywordAdapter.getCheckedKeywords().isEmpty()){
            stateView.setState(StateView.STATE_MONITORING_REQUEST_KEYWORD);
//            startMyNotificationListenerService();
        }else {
            stateView.setState(StateView.STATE_MONITORING_STOP);
//            startMyNotificationListenerService();
        }
//        else{checkAndRebindNotificationListener();}
//        else{startMyNotificationListenerService();}

        // 通知ポスト権限を確認し、無ければリクエスト
        if(!hasNotifyPermission(this)){
            requestNotificationPostPermission();
        }

        // Switchの状態変更リスナーをセット
        stateView.setOnSwitchStateChangeListener(isChecked -> {
            if (isChecked && !keywordAdapter.getCheckedKeywords().isEmpty()) {
                // スイッチがオン且つ監視キーワードがある場合の処理
                stateView.setState(StateView.STATE_MONITORING);
            } else if (isChecked && keywordAdapter.getCheckedKeywords().isEmpty()){
                // スイッチがオンで監視キーワードが無い場合の処理
                stateView.setState(StateView.STATE_MONITORING_REQUEST_KEYWORD);
            }else {
                // スイッチがオフの場合の処理
                stateView.setState(StateView.STATE_MONITORING_STOP);
            }
        });

        // ViewModel を onCreate 内で初期化
        myViewModel = new ViewModelProvider(this).get(MyViewModel.class);

        // 通知を受け取った際の処理
        myViewModel.getNotificationDetails().observe(this, notificationDetails -> {
            sLogd(TAG, "onCreate: myViewModel.getNotificationDetails().observe",this);
            // 通知を受け取った時の処理（ここで警告音を鳴らす）
//            triggerAlarmOnNotification();
        });

        // BroadcastReceiver を登録
        IntentFilter filter = new IntentFilter("com.example.nuwarning.NOTIFICATION_LOG");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(notificationReceiver, filter,Context.RECEIVER_NOT_EXPORTED);

        sLogd(TAG, "onCreate: end",this);
    }

    @Override
    protected void onStart() {
        sLogd(TAG, "onStart: start",this);
        super.onStart();
        sLogd(TAG, "onStart: end",this);
    }

    @Override
    protected void onResume() {
        sLogd(TAG, "onResume: start",this);
        super.onResume();

        // ダイアログが表示されている場合は閉じる
        if (permissionDialog != null && permissionDialog.isShowing()) {
            permissionDialog.dismiss();
        }

        // 通知ポスト権限を確認し、無ければリクエスト
        if(!hasNotifyPermission(this)){
            requestNotificationPostPermission();
        }

        // 通知監視の許可がされていない場合は、リクエストする
        if (!NotificationUtils.isNotificationAccessGranted(this)) {
            stateView.setState(StateView.STATE_REQUEST_PERMISSION);
            requestNotificationPermission();
        }else if (keywordAdapter.getCheckedKeywords().isEmpty()){
            reenableNotificationListenerService();
//            startMyNotificationListenerService();//アプリが応答しない、になる原因ここか？
            stateView.setState(StateView.STATE_MONITORING_REQUEST_KEYWORD);
        }else {
            reenableNotificationListenerService();
//            startMyNotificationListenerService();//アプリが応答しない、になる原因ここか？
            stateView.setState(StateView.STATE_MONITORING);
        }

        sLogd(TAG, "onResume: end",this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sLogd(TAG, "onPause",this);
        saveKeywords();
    }

    @Override
    protected void onStop() {

        sLogd(TAG, "onStop",this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // Activity を破棄するときに Receiver を解除
        unregisterReceiver(notificationReceiver);
        stopMyNotificationListenerService();
        Util.saveAppIsAlive(false,getApplicationContext());
        sLogd(TAG, "onDestroy: end",this);

        stopNotificationListenerService();

        super.onDestroy();

    }

    // BroadcastReceiver で通知の内容を受け取る
    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String notificationDetails = intent.getStringExtra("notification_details");
            if (notificationDetails != null) {
                myViewModel.setNotificationDetails(notificationDetails);
            } else {
                Log.e("NotificationReceiver", "通知内容が null です。");
            }
        }
    };

    // 通知アクセスの許可を求めるメソッド
    public void requestNotificationPermission() {
        if (permissionDialog != null && permissionDialog.isShowing()) {
            return;
        }

        permissionDialog = new AlertDialog.Builder(this)
                .setTitle("通知アクセスの許可")
                .setMessage("通知監視を有効にするため、設定画面に移動します。通知へのアクセスを許可してください。")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("キャンセル", (dialog, which) -> {
                    Toast.makeText(MainActivity.this, "通知アクセスが必要です。", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // 通知権限が許可されているか確認
    private boolean isNotificationPermissionGranted() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(getPackageName());
    }

    private void requestNotificationPostPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 通知権限をリクエスト
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

    }

    public static boolean hasNotifyPermission(Context context) {
        int checkPermissionResult = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        );
        return checkPermissionResult == PackageManager.PERMISSION_GRANTED;
    }

    private void reenableNotificationListenerService() {
        sLogd(TAG,"reenableNotificationListenerService_start",this);
        ComponentName componentName = new ComponentName(this, SNotificationListenerService.class); // サービスの実装クラスを指定
        PackageManager packageManager = getPackageManager();

        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    // MyNotificationListenerService を開始するメソッド
    private void startMyNotificationListenerService() {
        Intent serviceIntent = new Intent(this, SNotificationListenerService.class);

//        startForegroundService(serviceIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sLogd(TAG,"startMyNotificationListenerService_Build.VERSION.SDK_INT >= Build.VERSION_CODES.O",this);
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // MyNotificationListenerService の停止を指示
    private void stopMyNotificationListenerService() {
        Intent stopIntent = new Intent(this, SNotificationListenerService.class);
        stopIntent.setAction(SNotificationListenerService.ACTION_STOP_FOREGROUND);
        startService(stopIntent);
    }

    private void checkAndRebindNotificationListener() {
        // NotificationListenerServiceがバインドされているか確認
        ComponentName componentName = new ComponentName(this, SNotificationListenerService.class);

        // バインドされていない場合は再バインドをリクエスト
        if (!isNotificationListenerServiceConnected()) {
            requestRebind(componentName);  // 再バインドをリクエスト
        }
    }

    private boolean isNotificationListenerServiceConnected() {
        // 現在NotificationListenerServiceが接続されているか確認する
        // これを実現するためには、NotificationListenerService側で接続状態を管理する必要がある場合が多い
        // 例えば、接続時にフラグを立てておき、切断時にフラグを戻すようにする
        return false; // 実際には状態管理を追加して確認する
    }

    private void requestRebind(ComponentName componentName) {
        // NotificationListenerServiceへの再バインドをリクエスト
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setAction("android.service.notification.NotificationListenerService");
        startService(intent);
    }

    private void onAddKeyword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("キーワード追加");

        EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newKeyword = input.getText().toString().trim().replaceAll("[\\r\\n]+", "");
            if (!newKeyword.isEmpty()) {
                Keyword newKeywordItem = new Keyword(newKeyword);
                List<Keyword> currentList = new ArrayList<>(keywordAdapter.getCurrentList());
                currentList.add(newKeywordItem);

                keywordAdapter.setKeywordList(currentList);
                saveKeywords();
                if (stateView.getState() == StateView.STATE_MONITORING_REQUEST_KEYWORD){
                    stateView.setState(StateView.STATE_MONITORING);
                }

            } else {
                Toast.makeText(MainActivity.this, "キーワードを入力してください", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void saveKeywords() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_KEYWORDS, serializeKeywordList(keywordAdapter.getCurrentList()));
        editor.apply();
    }

    private void loadKeywords() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String serializedList = preferences.getString(KEY_KEYWORDS, null);
        if (serializedList != null) {
            List<Keyword> keywords = deserializeKeywordList(serializedList);
            keywordAdapter.setKeywordList(keywords);
        }
    }

    private String serializeKeywordList(List<Keyword> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    private List<Keyword> deserializeKeywordList(String serializedList) {
        Gson gson = new Gson();
        Keyword[] keywordsArray = gson.fromJson(serializedList, Keyword[].class);
        List<Keyword> list = new ArrayList<>();
        for (Keyword keyword : keywordsArray) {
            list.add(keyword);
        }
        return list;
    }

    @Override
    public void onEditKeyword(int position) {
        Keyword keyword = keywordAdapter.getCurrentList().get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("キーワード編集");

        EditText input = new EditText(this);
        input.setText(keyword.getName());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newKeyword = input.getText().toString().trim().replaceAll("[\\r\\n]+", "");
            if (!newKeyword.isEmpty()) {
                keyword.setName(newKeyword);
                List<Keyword> currentList = new ArrayList<>(keywordAdapter.getCurrentList());
                currentList.set(position, keyword);
                keywordAdapter.setKeywordList(currentList);
                saveKeywords();
                Toast.makeText(MainActivity.this, "キーワードが更新されました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "キーワードを入力してください", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void onDeleteKeyword(int position) {
        List<Keyword> currentList = new ArrayList<>(keywordAdapter.getCurrentList());
        currentList.remove(position);
        keywordAdapter.setKeywordList(currentList);
        saveKeywords();

        if(keywordAdapter.getCheckedKeywords().isEmpty() && stateView.getState() == StateView.STATE_MONITORING){
            stateView.setState(StateView.STATE_MONITORING_REQUEST_KEYWORD);
        }
    }

    @Override
    public void onToggleKeywordCheck(int position) {
        Keyword keyword = keywordAdapter.getCurrentList().get(position);
        keyword.setChecked(!keyword.isChecked());

        List<Keyword> updatedList = new ArrayList<>(keywordAdapter.getCurrentList());
        updatedList.set(position, keyword);
        keywordAdapter.setKeywordList(updatedList);
        saveKeywords();

        if(keyword.isChecked() && stateView.getState() == StateView.STATE_MONITORING_REQUEST_KEYWORD){
            stateView.setState(StateView.STATE_MONITORING);
        }else if(keywordAdapter.getCheckedKeywords().isEmpty() && stateView.getState() == StateView.STATE_MONITORING) {
            stateView.setState(StateView.STATE_MONITORING_REQUEST_KEYWORD);
        }
    }

    private void stopNotificationListenerService() {
        Intent serviceIntent = new Intent(this, SNotificationListenerService.class);
        serviceIntent.setAction(SNotificationListenerService.ACTION_STOP_FOREGROUND);
        startService(serviceIntent);
    }

    public static PendingIntent getFullScreenIntent(Context context) {
        Intent fullScreenIntent = new Intent(context, MainActivity.class);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
