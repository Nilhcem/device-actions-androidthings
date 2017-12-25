package com.nilhcem.androidthings.deviceactions.device.components

import android.arch.lifecycle.LiveData
import android.util.Log
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat

class ButtonLiveData : LiveData<Boolean>() {

    companion object {
        private val TAG = ButtonLiveData::class.java.simpleName!!
        private const val BUTTON_DEBOUNCE_DELAY_MS = 20L
    }

    private var button: Button? = null

    override fun onActive() {
        Log.d(TAG, "onActive")
        button = RainbowHat.openButtonA().apply {
            setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS)
            setOnButtonEventListener { _, pressed ->
                value = pressed
            }
        }
    }

    override fun onInactive() {
        Log.d(TAG, "onInactive")
        button?.let {
            it.setOnButtonEventListener(null)
            it.close()
        }.also {
            button = null
        }
    }
}
