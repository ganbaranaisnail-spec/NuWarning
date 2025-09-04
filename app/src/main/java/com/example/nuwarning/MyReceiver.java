package com.example.nuwarning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 通知の内容を受け取る
        String notificationDetails = intent.getStringExtra("notification_details");

        if (notificationDetails != null) {
            // NotificationViewModel を取得
            NotificationViewModel viewModel = new ViewModelProvider((MainActivity) context).get(NotificationViewModel.class);
            // ViewModel に通知内容をセット
            viewModel.setNotificationDetails(notificationDetails);
        }
    }
}
