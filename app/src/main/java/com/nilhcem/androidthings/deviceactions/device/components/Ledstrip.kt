package com.nilhcem.androidthings.deviceactions.device.components

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.Color
import com.nilhcem.androidthings.driver.blinkt.Blinkt

class Ledstrip : LifecycleObserver {

    private var blinkt: Blinkt? = null
    private var curColor = Color.WHITE
    private val colors = IntArray(Blinkt.LEDSTRIP_LENGTH)

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        blinkt = Blinkt().apply {
            brightness = Blinkt.MAX_BRIGHTNESS
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        blinkt?.close().also { blinkt = null }
    }

    fun on() = setColor(curColor)

    fun off() = setColor(Color.BLACK, false)

    fun setColor(color: Int, persistColor: Boolean = true) {
        for (i in colors.indices) {
            colors[i] = color
        }

        blinkt?.write(colors)

        if (persistColor) {
            curColor = color
        }
    }
}
