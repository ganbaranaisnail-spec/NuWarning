package com.example.nuwarning;

import android.os.Bundle;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
// ... 他の必要な import ...

public class SettingActivity extends AppCompatActivity {

    private Switch tcSwitch;
    private boolean isTC;
    private StateView.OnSwitchStateChangeListener tcSwitchSChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting); // レイアウトファイルを指定

        tcSwitch = findViewById(R.id.tcSwitch);

        isTC = Util.isAppTC(this);
        if(isTC){
            tcSwitch.setText("有効");
        }else {
            tcSwitch.setText("無効");
        }

        tcSwitch.setChecked(isTC);
        tcSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Util.saveAppIsTC(isChecked,this);
            isTC = isChecked;
            if(isChecked){
                tcSwitch.setText("有効");
            }else {
                tcSwitch.setText("無効");
            }
        });

        // ... 初期化処理 ...
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isTC){
            tcSwitch.setText("有効");
        }else {
            tcSwitch.setText("無効");
        }
    }
}