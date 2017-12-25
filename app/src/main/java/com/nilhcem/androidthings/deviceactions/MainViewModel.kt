package com.nilhcem.androidthings.deviceactions

import android.arch.lifecycle.ViewModel
import android.util.Log
import com.nilhcem.androidthings.deviceactions.device.components.ButtonLiveData

class MainViewModel : ViewModel() {

    companion object {
        private val TAG = MainViewModel::class.java.simpleName!!
    }

    val buttonLiveData by lazy { ButtonLiveData() }

    override fun onCleared() {
        Log.i(TAG, "onCleared")
    }
}
