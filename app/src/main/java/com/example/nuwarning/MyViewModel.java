package com.example.nuwarning;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MyViewModel extends ViewModel {
    
    private final MutableLiveData<String> notificationDetails = new MutableLiveData<>();

    // 通知内容を取得するための LiveData
    public LiveData<String> getNotificationDetails() {
        return notificationDetails;
    }

    // 通知内容を設定する
    public void setNotificationDetails(String details) {
        notificationDetails.setValue(details);
    }
}
