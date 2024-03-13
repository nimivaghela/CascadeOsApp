package com.app.cascadeos.viewmodel

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Timer
import java.util.TimerTask


open class LiveDataTimerViewModel : ViewModel() {
    private val ONE_SECOND = 1000

    private val mElapsedTime = MutableLiveData<Long>()

    private var mInitialTime: Long = 0
    private var timer: Timer? = null

    init {
        mInitialTime = SystemClock.elapsedRealtime()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val newValue: Long = (SystemClock.elapsedRealtime() - mInitialTime) / 1000
                // setValue() cannot be called from a background thread so post to main thread.
                mElapsedTime.postValue(newValue)
            }
        }, ONE_SECOND.toLong(), ONE_SECOND.toLong())
    }

    fun getElapsedTime(): LiveData<Long?>? {
        return mElapsedTime
    }

    override fun onCleared() {
        super.onCleared()
        timer!!.cancel()
    }

}