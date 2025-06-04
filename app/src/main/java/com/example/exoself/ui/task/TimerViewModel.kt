package com.example.exoself.ui.task

import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

private const val ONE_SECOND: Long = 1000

enum class TimerState {
    READY, RUNNING, STOPPED, FINISHED
}

class TimerViewModel : ViewModel() {

    private var timer: Timer? = null
    private val remainingSeconds = MutableLiveData<Long>().apply {
        value = 0
    }
    private val finished = MutableLiveData<Boolean>().apply {
        value = false
    }
    var timerState = TimerState.READY

    private fun start(milliseconds: Long) {
        val offsetTime = SystemClock.elapsedRealtime()
        timer?.cancel()
        timer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    val newValue: Long =
                        ((offsetTime + milliseconds) - SystemClock.elapsedRealtime()) / 1000

                    remainingSeconds.postValue(newValue)
                    if (newValue <= 0L) {
                        finished()
                    }
                }
            }, ONE_SECOND, ONE_SECOND)
        }
    }

    private fun finished() {
        timer?.cancel()
        finished.postValue(true)
    }

    fun startTimer(initialSeconds: Long) {
        remainingSeconds.postValue(initialSeconds)
        start(initialSeconds * 1000)
    }

    fun resumeTime() {
        start(remainingSeconds.value!! * ONE_SECOND)
    }

    fun stopTimer() {
        timer?.cancel()
    }

    fun getElapsedTime(): MutableLiveData<Long> {
        return remainingSeconds
    }

    fun isFinished(): MutableLiveData<Boolean> {
        return finished
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}