package com.nilhcem.androidthings.deviceactions

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.nilhcem.androidthings.deviceactions.googleassistant.AssistantHelper

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName!!
    }

    private val assistant by lazy { AssistantHelper(this) }
    private val viewModel by lazy { ViewModelProviders.of(this).get(MainViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        lifecycle.addObserver(assistant)

        viewModel.buttonLiveData.observe({ lifecycle }) { pressed ->
            pressed?.let {
                Log.i(TAG, "Button pressed: $pressed")
                assistant.onButtonPressed(pressed)
            }
        }
    }
}
