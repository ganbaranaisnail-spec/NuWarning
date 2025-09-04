package com.example.nuwarning;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<String> notificationDetails = new MutableLiveData<>();

    // 通知内容をセット
    public void setNotificationDetails(String details) {
        notificationDetails.setValue(details);
    }

    // 通知内容を取得
    public LiveData<String> getNotificationDetails() {
        return notificationDetails;
    }
}
