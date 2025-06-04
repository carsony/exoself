package com.example.exoself.ui.task

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.exoself.MainActivity.Companion.ACTION_TIMER_UP
import com.example.exoself.MainActivity.Companion.ARG_TIMER_UP
import com.example.exoself.R
import com.example.exoself.databinding.FragmentTaskBinding
import java.util.*

class TaskFragment : Fragment() {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private val timerViewModel: TimerViewModel by activityViewModels()
    private val taskViewModel: TaskViewModel by activityViewModels()
    private var timeLeft: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        binding.viewstubStart.inflate().visibility = View.GONE
        binding.viewstubStop.inflate().visibility = View.GONE
        binding.viewstubStopped.inflate().visibility = View.GONE
        binding.viewstubPostpone.inflate().visibility = View.GONE
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timerUp = arguments?.getBoolean(ARG_TIMER_UP) ?: false
        if (timerUp) {
            stopAlarm()
            timerViewModel.timerState = TimerState.FINISHED
        }

        taskViewModel.firstTask.observe(viewLifecycleOwner) { task ->
            if (task == null) {
                timerViewModel.timerState = TimerState.READY
                binding.viewstubStart.visibility = View.INVISIBLE
                binding.taskDescriptionTv.text = "All done for now!"
                binding.remainingTimeTv.text = "00:00:00"
                binding.taskTagsTv.text = ""
                binding.root.setBackgroundColor(-1)
            } else {
                if (timerViewModel.timerState == TimerState.READY) {
                    binding.viewstubStart.visibility = View.VISIBLE
                }
                binding.taskDescriptionTv.text = task.description
                binding.taskTagsTv.text = task.tags.joinToString { it }
                binding.root.setBackgroundColor(task.color ?: -1)
            }
        }

        timerViewModel.isFinished().observe(viewLifecycleOwner) { isFinished ->
            if (isFinished == true && timerViewModel.timerState == TimerState.RUNNING) {
                finished()
            }
        }
        timerViewModel.getElapsedTime().observe(viewLifecycleOwner) { seconds ->
            timeLeft = seconds
            binding.remainingTimeTv.text = timerFormatFromSeconds(timeLeft)
        }

        when (timerViewModel.timerState) {
            TimerState.READY -> ready()
            TimerState.RUNNING -> resume()
            TimerState.STOPPED -> stop()
            TimerState.FINISHED -> finished()
        }

        setupButtons()
    }

    // Couldn't figure out how to use data binding to access ViewStub's nodes
    private fun setupButtons() {
        binding.root.findViewById<Button>(R.id.start_btn).setOnClickListener {
            start()
        }
        binding.root.findViewById<Button>(R.id.stop_btn).setOnClickListener {
            stop()
        }
        binding.root.findViewById<Button>(R.id.resume_btn).setOnClickListener {
            resume()
        }
        binding.root.findViewById<Button>(R.id.restart_btn).setOnClickListener {
            restart()
        }
        binding.root.findViewById<Button>(R.id.complete_btn).setOnClickListener {
            complete()
        }
        binding.root.findViewById<Button>(R.id.postpone_btn).setOnClickListener {
            postpone()
        }
        binding.root.findViewById<Button>(R.id.back_btn).setOnClickListener {
            backToStopped()
        }
        binding.root.findViewById<Button>(R.id.back_of_stack_btn).setOnClickListener {
            sendToBackOfStack()
        }
        binding.root.findViewById<Button>(R.id.tomorrow_btn).setOnClickListener {
            scheduleForTomorrow()
        }
    }

    private fun ready() {
        binding.root.findViewById<Button>(R.id.start_btn).visibility = View.VISIBLE
        binding.viewstubStart.visibility = View.VISIBLE
        binding.viewstubStop.visibility = View.GONE
        binding.viewstubStopped.visibility = View.GONE
        binding.viewstubPostpone.visibility = View.GONE
        binding.taskStatusTv.text = getString(R.string.ready)
        timerViewModel.timerState = TimerState.READY
    }

    private fun start() {
        taskViewModel.profileLiveData.observe(viewLifecycleOwner) { profile ->
            val timerDuration = profile.timerDuration * 60
            startAlarm(timerDuration)
            binding.remainingTimeTv.text = timerFormatFromSeconds(timerDuration)
            timerViewModel.startTimer(timerDuration)
        }
        binding.taskStatusTv.text = getString(R.string.executing_task)
        binding.viewstubStart.visibility = View.GONE
        binding.viewstubStop.visibility = View.VISIBLE
        timerViewModel.timerState = TimerState.RUNNING
    }

    private fun stop() {
        stopAlarm()
        timerViewModel.stopTimer()
        binding.root.findViewById<Button>(R.id.resume_btn).visibility = View.VISIBLE
        binding.taskStatusTv.text = getString(R.string.task_paused)
        binding.viewstubStop.visibility = View.GONE
        binding.viewstubStopped.visibility = View.VISIBLE
        timerViewModel.timerState = TimerState.STOPPED
    }

    private fun resume() {
        startAlarm(timeLeft)
        timerViewModel.resumeTime()
        binding.taskStatusTv.text = getString(R.string.executing_task)
        binding.viewstubStopped.visibility = View.GONE
        binding.viewstubStop.visibility = View.VISIBLE
        timerViewModel.timerState = TimerState.RUNNING
    }

    private fun restart() {
        binding.viewstubStopped.visibility = View.GONE
        binding.taskStatusTv.text = getString(R.string.executing_task)
        start()
    }

    private fun complete() {
        taskViewModel.profileLiveData.observe(viewLifecycleOwner) {
            taskViewModel.completeTask()
        }
        ready()
    }

    private fun postpone() {
        binding.taskStatusTv.text = getString(R.string.postponing_task)
        binding.viewstubStopped.visibility = View.GONE
        binding.viewstubPostpone.visibility = View.VISIBLE
    }

    private fun backToStopped() {
        if (timerViewModel.timerState == TimerState.STOPPED) {
            binding.taskStatusTv.text = getString(R.string.task_paused)
        } else {
            binding.taskStatusTv.text = getString(R.string.timer_up)
        }
        binding.viewstubStopped.visibility = View.VISIBLE
        binding.viewstubPostpone.visibility = View.GONE
    }

    private fun sendToBackOfStack() {
        taskViewModel.sendToBackOfStack()
        ready()
    }

    private fun scheduleForTomorrow() {
        taskViewModel.scheduleForTomorrow()
        ready()
    }

    private fun finished() {
        binding.root.findViewById<Button>(R.id.resume_btn).visibility = View.GONE
        binding.root.findViewById<Button>(R.id.start_btn).visibility = View.GONE
        binding.taskStatusTv.text = getString(R.string.timer_up)
        binding.viewstubStop.visibility = View.GONE
        binding.viewstubStopped.visibility = View.VISIBLE
        timerViewModel.timerState = TimerState.FINISHED
    }

    private fun startAlarm(timeLeft: Long) {
        val calendar = Calendar.getInstance().also {
            it.add(Calendar.SECOND, timeLeft.toInt())
        }
        val intent = Intent().also {
            it.action = ACTION_TIMER_UP
        }
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent =
            PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun stopAlarm() {
        val intent = Intent().also {
            it.action = ACTION_TIMER_UP
        }
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent =
            PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun timerFormatFromSeconds(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds % 60)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}