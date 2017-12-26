package com.nilhcem.androidthings.deviceactions

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.nilhcem.androidthings.deviceactions.device.components.Ledstrip
import com.nilhcem.androidthings.deviceactions.googleassistant.AssistantHelper
import com.nilhcem.androidthings.deviceactions.googleassistant.BuildConfig

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName!!
    }

    private val assistant by lazy { AssistantHelper(this) }
    private val ledstrip by lazy { Ledstrip() }
    private val viewModel by lazy { ViewModelProviders.of(this).get(MainViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        lifecycle.addObserver(assistant)
        lifecycle.addObserver(ledstrip)

        viewModel.buttonLiveData.observe({ lifecycle }) { pressed ->
            pressed?.let {
                Log.i(TAG, "Button pressed: $pressed")
                assistant.onButtonPressed(pressed)
            }
        }

        assistant.deviceActionLiveData.observe({ lifecycle }) { deviceAction ->
            deviceAction?.let {
                deviceAction.inputs.forEach { input ->
                    input.payload.commands.forEach { command ->
                        if (command.devices.any { it.id == BuildConfig.ASSISTANT_DEVICE_ID }) {
                            command.execution.forEach { execution ->
                                when (execution.command) {
                                    "action.devices.commands.OnOff" -> {
                                        if (execution.params.on) {
                                            ledstrip.on()
                                        } else {
                                            ledstrip.off()
                                        }
                                    }
                                    "action.devices.commands.ColorAbsolute" -> {
                                        ledstrip.setColor(execution.params.color.spectrumRGB)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
