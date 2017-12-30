package com.nilhcem.androidthings.deviceactions.device.components

import android.arch.lifecycle.LiveData
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.nilhcem.androidthings.driver.mpr121.Mpr121
import java.io.IOException

class ButtonLiveData : LiveData<Boolean>() {

    companion object {
        private const val I2C_NAME = "I2C1"
        private const val MPR121_TOUCH_PIN = 11
        private const val SOFTWAREPOLL_DELAY_MS = 100L
    }

    private var mpr121: Mpr121? = null
    private var isTouched = false

    private var handlerThread: HandlerThread? = null
    private var inputHandler: Handler? = null

    /**
     * Callback invoked to poll the state of the controller
     */
    private val pollingCallback = object : Runnable {
        override fun run() {
            try {
                val data = mpr121!!.touched

                if (data and (1 shl MPR121_TOUCH_PIN) == 0) {
                    if (isTouched) {
                        isTouched = false
                        postValue(false)
                    }
                } else {
                    if (!isTouched) {
                        isTouched = true
                        postValue(true)
                    }
                }
            } catch (e: IOException) {
                Log.e(ButtonLiveData::class.java.simpleName, "Error getting data from peripheral device", e)
            } finally {
                inputHandler?.postDelayed(this, SOFTWAREPOLL_DELAY_MS)
            }
        }
    }

    override fun onActive() {
        mpr121 = Mpr121(I2C_NAME)

        handlerThread = HandlerThread("Mpr121Thread").apply {
            start()
            inputHandler = Handler(looper).apply {
                post(pollingCallback)
            }
        }
    }

    override fun onInactive() {
        inputHandler?.apply {
            removeCallbacks(pollingCallback)
            mpr121?.apply { close() }.also { mpr121 = null }
        }.also { inputHandler = null }
    }
}
