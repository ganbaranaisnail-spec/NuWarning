package com.example.nuwarning;

import android.content.Context;
import android.provider.Settings;

public class NotificationUtils {

    // 通知アクセスが許可されているか確認するメソッド
    public static boolean isNotificationAccessGranted(Context context) {
        String enabledListeners = Settings.Secure.getString(context.getContentResolver(),
                "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(context.getPackageName());
    }
}
