package com.example.unohome.ui.addroom;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AddroomViewModel extends ViewModel{
    private MutableLiveData<String> mText;

    public AddroomViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is add room fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
