package com.nilhcem.androidthings.deviceactions.device.components

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.Color
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat

class Ledstrip : LifecycleObserver {

    private var ledstrip: Apa102? = null
    private var curColor = Color.WHITE
    private val colors = IntArray(RainbowHat.LEDSTRIP_LENGTH)

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        ledstrip = RainbowHat.openLedStrip().apply {
            brightness = 1
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        ledstrip?.close().also { ledstrip = null }
    }

    fun on() = setColor(curColor)

    fun off() = setColor(Color.BLACK, false)

    fun setColor(color: Int, persistColor: Boolean = true) {
        for (i in colors.indices) {
            colors[i] = color
        }
        ledstrip?.write(colors)
        ledstrip?.write(colors)

        if (persistColor) {
            curColor = color
        }
    }
}
