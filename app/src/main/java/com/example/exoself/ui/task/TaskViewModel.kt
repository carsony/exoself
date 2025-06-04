package com.example.exoself.ui.task

import androidx.lifecycle.*
import com.example.exoself.domain.Orderings
import com.example.exoself.domain.Profile
import com.example.exoself.domain.Tag
import com.example.exoself.domain.Task
import com.example.exoself.repositories.AuthRepository
import com.example.exoself.repositories.FirestoreRepository
import com.example.exoself.repositories.LiveDataHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class TaskViewModel : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    private val _orderings = MutableLiveData<Orderings>()
    private val _tags = MutableLiveData<List<Tag>>()
    private val _profile = MutableLiveData<Profile>()
    var firstTask = MediatorLiveData<Task>()
    var tasksLiveData: LiveData<List<Task>> = _tasks
    var tagsLiveData: LiveData<List<Tag>> = _tags
    var orderingsLiveData: LiveData<Orderings> = _orderings
    var profileLiveData: LiveData<Profile> = _profile
    var currentTasks = MediatorLiveData<List<Task>>()
    var tasksMap = MediatorLiveData<Map<String, Task>>()
    var tagsMap = MediatorLiveData<Map<String, Tag>>()

    init {
        viewModelScope.launch {
            tasksLiveData = LiveDataHelper.getTasksData()
            orderingsLiveData = LiveDataHelper.getOrderingsData()
            tagsLiveData = LiveDataHelper.getTagsData()
            profileLiveData = LiveDataHelper.getProfileData()
        }
        initCurrentTasksLiveData()
        initTagsMapLiveData()
        initTasksMapLiveData()
        initFirstTaskLiveData()
    }

    private fun initCurrentTasksLiveData() {
        currentTasks.apply {
            addSource(tasksMap) {
                value = combineTasksData(orderingsLiveData.value?.current, tasksMap, tagsLiveData)
            }
            addSource(orderingsLiveData) {
                value = combineTasksData(orderingsLiveData.value?.current, tasksMap, tagsLiveData)
            }
            addSource(tagsLiveData) {
                value = combineTasksData(orderingsLiveData.value?.current, tasksMap, tagsLiveData)
            }
            value = listOf()
        }
    }

    private fun initFirstTaskLiveData() {
        firstTask.apply {
            addSource(currentTasks) {
                value = currentTasks.value?.firstOrNull { t -> t.current && !t.completed }
            }
        }
    }

    private fun initTagsMapLiveData() {
        tagsMap.apply {
            addSource(tagsLiveData) { value = tagsLiveData.value?.associate { it.docId to it } }
            value = hashMapOf()
        }
    }

    private fun initTasksMapLiveData() {
        tasksMap.apply {
            addSource(tasksLiveData) { value = tasksLiveData.value?.associate { it.docId to it } }
            value = hashMapOf()
        }
    }

    private fun combineTasksData(
        order: List<String>?,
        tasksMap: LiveData<Map<String, Task>>,
        tags: LiveData<List<Tag>>
    ): List<Task>? {
        val tagsMap = tags.value?.associate { it.docId to it }
        return order?.mapNotNull {
            tasksMap.value?.get(it)
        }?.mapIndexed { _, task ->
            val newTask = task.copy()
            newTask.tags = task.tags.map { tag -> tagsMap?.get(tag)?.name ?: "" }
            newTask
        }
    }

    fun completeTask() {
        val task = firstTask.value!!
        val uid = AuthRepository.getCurrentUser()!!.uid
        val nameToTagMap = tagsLiveData.value?.associate { it.name to it }
        val modifiedTask = firstTask.value!!.copy().apply {
            completedTime = Timestamp.now()
            completed = true
            current = false
            tags = task.tags.map { nameToTagMap!![it]!!.docId }
        }
        incrementTagsNumberAndMinutesCounter(task)
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.completeTask(uid, modifiedTask, orderingsLiveData.value!!)
        }
    }

    private fun incrementTagsNumberAndMinutesCounter(task: Task) {
        val nameToTagMap = tagsLiveData.value?.associate { it.name to it }
        val tags = task.tags.map { nameToTagMap!![it]!!.copy() }
        for (tag in tags) {
            tag.minutesCompleted += profileLiveData.value!!.timerDuration
            tag.numberCompleted += 1
        }
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.modifyTags(tags)
        }
    }

    fun sendToBackOfStack() {
        val uid = AuthRepository.getCurrentUser()!!.uid
        val modifiedCurrentOrdering = orderingsLiveData.value!!.current.toMutableList()
        val firstTaskId = modifiedCurrentOrdering.firstOrNull()
        if (firstTaskId != null) {
            modifiedCurrentOrdering.removeAt(0)
            modifiedCurrentOrdering.add(firstTaskId)
        }
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.modifyOrderingByName(uid, modifiedCurrentOrdering, "current")
        }
    }

    fun scheduleForTomorrow() {
        val uid = AuthRepository.getCurrentUser()!!.uid
        val modifiedCurrentOrdering = orderingsLiveData.value!!.current.toMutableList()
        val firstTaskId = modifiedCurrentOrdering.firstOrNull()
        if (firstTaskId != null) {
            modifiedCurrentOrdering.removeAt(0)
        }
        val cal = Calendar.getInstance().also {
            it.add(Calendar.DATE, 1)
        }
        val nameToTagMap = tagsLiveData.value?.associate { it.name to it }
        val modifiedTask = firstTask.value!!.copy().apply {
            scheduledTime = Timestamp(cal.time)
            current = false
            tags = this.tags.map { nameToTagMap!![it]!!.docId }
        }
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.modifyOrderingByName(uid, modifiedCurrentOrdering, "current")
            FirestoreRepository.setTask(modifiedTask)
        }
    }
}