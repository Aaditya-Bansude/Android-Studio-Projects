package com.example.unohome.ui.devicecontrol;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DeviceControlViewModel extends ViewModel{
    private MutableLiveData<String> mText;

    public DeviceControlViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is device control fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
