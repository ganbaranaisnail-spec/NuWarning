package com.example.nuwarning;

import static android.content.Context.MODE_PRIVATE;

import static com.example.nuwarning.Util.APP_SETTINGS;
import static com.example.nuwarning.Util.APP_SETTINGS_KEY_STATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StateView extends ConstraintLayout {

    private ImageView stateImage;
    private TextView stateText;
    private TextView stateSubText;
    private Switch startSwitch;

    private int cState;

    // 定数の定義
    public final static int STATE_REQUEST_PERMISSION = 0;
    public final static int STATE_MONITORING_STOP = 1;
    public final static int STATE_MONITORING_REQUEST_KEYWORD = 2;
    public final static int STATE_MONITORING = 3;

    private OnSwitchStateChangeListener switchStateChangeListener;

    public StateView(Context context) {
        super(context);
        init(context);
    }

    public StateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.state_view, this);  // レイアウトをstate_view.xmlに変更

        stateImage = findViewById(R.id.stateImage);
        stateText = findViewById(R.id.stateText);
        stateSubText = findViewById(R.id.stateSubText);
        startSwitch = findViewById(R.id.startSwitch);

        setState(STATE_MONITORING);

        // Switchの状態変更をリスン
        startSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchStateChangeListener != null) {
                switchStateChangeListener.onSwitchStateChanged(isChecked);
            }
        });
    }

    // setState メソッドを追加

    public void setState(int state) {
        Context context = getContext();
        switch (state) {
            case STATE_REQUEST_PERMISSION:
                stateImage.setImageResource(android.R.drawable.ic_dialog_alert);  // アイコンを警告アイコンに設定
                stateText.setText("通知へのアクセス権が必要です");
                stateSubText.setText("アプリに必要な権限を付与してください");
                startSwitch.setVisibility(GONE);  // Switchは非表示にする
                setBackgroundColor(context.getResources().getColor(R.color.color_gray, context.getTheme())); // 背景色を灰色に設定
                break;

            case STATE_MONITORING_STOP:
                stateImage.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);  // アイコンを停止アイコンに設定
                stateText.setText("配信開始を監視してないよ！");
                stateSubText.setText("そこにあるスイッチで監視始めるわ↴");
                startSwitch.setVisibility(VISIBLE);  // Switchを表示
                startSwitch.setChecked(false);  // スイッチをオフに設定
                setBackgroundColor(context.getResources().getColor(R.color.color_yellow, context.getTheme())); // 背景色を黄色に設定
                break;

            case STATE_MONITORING_REQUEST_KEYWORD:
                stateImage.setImageResource(android.R.drawable.ic_input_add);  // アイコンを追加アイコンに設定
                stateText.setText("監視するキーワードが無いよ～");
                stateSubText.setText("監視するキーワードにチェックを入れるか、監視キーワードを追加してね(チャンネル名の一部など)");
                startSwitch.setVisibility(VISIBLE);  // Switchを表示
                startSwitch.setChecked(true);  // スイッチをオンに設定
                setBackgroundColor(context.getResources().getColor(R.color.color_light_green, context.getTheme())); // 背景色を黄緑に設定
                break;

            case STATE_MONITORING:
                stateImage.setImageResource(android.R.drawable.ic_media_play);  // アイコンを再生アイコンに設定
                stateText.setText("配信開始を監視中です");
                stateSubText.setText("Youtube配信、Xスペース、ツイキャス配信(未対応)開始の通知が来ると、警告音を無限ループで流します(サイレントモード貫通)");
                startSwitch.setVisibility(VISIBLE);  // Switchを表示
                startSwitch.setChecked(true);  // スイッチをオンに設定
                setBackgroundColor(context.getResources().getColor(R.color.color_orange, context.getTheme())); // 背景色をオレンジに設定
                break;

            default:
                stateImage.setImageResource(android.R.drawable.ic_dialog_info);
                stateText.setText("不明な状態");
                stateSubText.setText("状態が不明です");
                startSwitch.setVisibility(GONE);  // Switchを非表示にする
                setBackgroundColor(context.getResources().getColor(R.color.color_gray, context.getTheme())); // 背景色を灰色に設定
                break;
        }

        saveMonitoringState(state);
        cState = state;
    }

    public int getState(){
        return cState;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // ExecutorService のインスタンス

    private void saveMonitoringState(int state) {
        executorService.execute(() -> {
            SharedPreferences prefs = getContext().getSharedPreferences(APP_SETTINGS, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(APP_SETTINGS_KEY_STATE, state);
            editor.apply();
        });
    }

    // getter メソッドを追加して、他のクラスからアクセスできるようにする
    public ImageView getStateImage() {
        return stateImage;
    }

    public TextView getStateText() {
        return stateText;
    }

    public TextView getStateSubText() {
        return stateSubText;
    }

    public Switch getStartSwitch() {
        return startSwitch;
    }

    // リスナーを設定するためのメソッド
    public void setOnSwitchStateChangeListener(OnSwitchStateChangeListener listener) {
        this.switchStateChangeListener = listener;
    }

    // リスナーインターフェースを定義
    public interface OnSwitchStateChangeListener {
        void onSwitchStateChanged(boolean isChecked);
    }
}
