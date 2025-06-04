package com.example.exoself.ui.home

import androidx.lifecycle.*
import com.example.exoself.domain.Orderings
import com.example.exoself.domain.Tag
import com.example.exoself.domain.Task
import com.example.exoself.repositories.AuthRepository
import com.example.exoself.repositories.FirestoreRepository
import com.example.exoself.repositories.LiveDataHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

enum class TabPageIds {
    CURRENT, SAVED, SCHEDULED, COMPLETED
}

class TasksViewModel : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    private val _orderings = MutableLiveData<Orderings>()
    private val _tags = MutableLiveData<List<Tag>>()
    private var tasksLiveData: LiveData<List<Task>> = _tasks
    private var orderingsLiveData: LiveData<Orderings> = _orderings
    private var tagsLiveData: LiveData<List<Tag>> = _tags
    private var tasksMap = MediatorLiveData<Map<String, Task>>()
    var currentTasks = MediatorLiveData<List<Task>>()
    var scheduledTasks = MediatorLiveData<List<Task>>()
    var savedTasks = MediatorLiveData<List<Task>>()
    var checkTimer: Timer? = null

    init {
        viewModelScope.launch {
            tasksLiveData = LiveDataHelper.getTasksData()
            orderingsLiveData = LiveDataHelper.getOrderingsData()
            tagsLiveData = LiveDataHelper.getTagsData()
        }
        initTasksMapLiveData()
        initCurrentTasksLiveData()
        initSavedTasksLiveData()
        initScheduledTasksLiveData()
        startScheduledCheckTimer()
    }

    fun startScheduledCheckTimer() {
        val oneMinuteInMilliseconds: Long = 1 * 60 * 1000
        val fiveSecondsDelayInMilliseconds: Long = 5 * 1000
        checkTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    checkAndAssignScheduledTasks()
                }
            }, fiveSecondsDelayInMilliseconds, oneMinuteInMilliseconds)
        }
    }

    fun stopScheduledCheckTimer() {
        checkTimer?.cancel()
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

    private fun initSavedTasksLiveData() {
        savedTasks.apply {
            addSource(tasksMap) {
                value = combineTasksData(orderingsLiveData.value?.saved, tasksMap, tagsLiveData)
            }
            addSource(orderingsLiveData) {
                value = combineTasksData(orderingsLiveData.value?.saved, tasksMap, tagsLiveData)
            }
            addSource(tagsLiveData) {
                value = combineTasksData(orderingsLiveData.value?.saved, tasksMap, tagsLiveData)
            }
            value = listOf()
        }
    }

    private fun initScheduledTasksLiveData() {
        scheduledTasks.apply {
            addSource(tasksMap) {
                value = combineScheduledTasksData(tasksLiveData, tagsLiveData)
            }
            addSource(tagsLiveData) {
                value = combineScheduledTasksData(tasksLiveData, tagsLiveData)
            }
            value = listOf()
        }
    }

    private fun combineScheduledTasksData(
        tasks: LiveData<List<Task>>,
        tags: LiveData<List<Tag>>
    ): List<Task>? {
        val tagsMap = tags.value?.associate { it.docId to it }
        return tasks.value
            ?.filter { it.scheduledTime != null }
            ?.mapIndexed { _, task ->
                val newTask = task.copy()
                newTask.tags = task.tags.map { tag -> tagsMap?.get(tag)?.name ?: "" }
                newTask
            }?.sortedBy { it.scheduledTime }
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

    private fun initTasksMapLiveData() {
        tasksMap.apply {
            addSource(tasksLiveData) { value = tasksLiveData.value?.associate { it.docId to it } }
            value = hashMapOf()
        }
    }

    fun checkAndAssignScheduledTasks() {
        if (tasksLiveData.value == null || AuthRepository.getCurrentUser() == null) {
            return
        }
        val uid = AuthRepository.getCurrentUser()!!.uid
        val scheduledTasks =
            tasksLiveData.value!!.filter { it.scheduledTime != null && !it.completed && !it.current }

        for (task in scheduledTasks) {
            val today = LocalDate.now()
            val scheduledDate =
                task.scheduledTime!!.toDate().toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDate()

            if (scheduledDate.equals(today) || scheduledDate.isBefore(today)) {
                val modifiedTask = task.copy().apply {
                    scheduledTime = null
                    current = true
                }
                val modifiedCurrentOrdering = orderingsLiveData.value!!.current.toMutableList()
                if (!modifiedCurrentOrdering.contains(task.docId)) {
                    modifiedCurrentOrdering.add(task.docId)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    FirestoreRepository.setTask(modifiedTask)
                    FirestoreRepository.modifyOrderingByName(
                        uid, modifiedCurrentOrdering, "current"
                    )
                }
            }
        }
    }

    fun reorderTaskList(first: Task, second: Task, tabId: Int) {
        val uid = AuthRepository.getCurrentUser()!!.uid
        val listName: String
        val ordering: MutableList<String>?

        when (tabId) {
            TabPageIds.CURRENT.ordinal, TabPageIds.COMPLETED.ordinal -> {
                ordering = orderingsLiveData.value?.current!!.toMutableList()
                listName = "current"
            }
            TabPageIds.SAVED.ordinal -> {
                ordering = orderingsLiveData.value?.saved!!.toMutableList()
                listName = "saved"
            }
            else -> return
        }

        val firstOrderIdx = ordering.indexOfFirst { first.docId == it }
        val secondOrderIdx = ordering.indexOfFirst { second.docId == it }
        Collections.swap(ordering, firstOrderIdx, secondOrderIdx)

        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.modifyOrderingByName(uid, ordering, listName)
        }
    }

    fun deleteTask(task: Task, tabId: Int) {
        val uid = AuthRepository.getCurrentUser()!!.uid
        var fullyDelete = false
        val modifiedOrdering = orderingsLiveData.value!!.copy()
        val currentOrdering = orderingsLiveData.value?.current!!.toMutableList()
        val savedOrdering = orderingsLiveData.value?.saved!!.toMutableList()

        when (tabId) {
            TabPageIds.CURRENT.ordinal, TabPageIds.COMPLETED.ordinal -> {
                currentOrdering.remove(task.docId)
                if (!savedOrdering.contains(task.docId)) {
                    fullyDelete = true
                }
            }
            TabPageIds.SAVED.ordinal -> {
                savedOrdering.remove(task.docId)
                task.saved = false
                if (!currentOrdering.contains(task.docId)) {
                    fullyDelete = true
                }
            }
            else -> return
        }

        modifiedOrdering.saved = savedOrdering
        modifiedOrdering.current = currentOrdering

        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.deleteTask(uid, task.docId, modifiedOrdering, fullyDelete)
            if (!fullyDelete) {
                FirestoreRepository.modifyTaskSave(task)
            }
        }
    }

    fun completeTask(task: Task, tabId: Int) {
        val uid = AuthRepository.getCurrentUser()!!.uid
        val modifiedOrdering = orderingsLiveData.value!!.copy()
        val nameToTagMap = tagsLiveData.value?.associate { it.name to it }
        val modifiedTask = task.copy().apply {
            tags = task.tags.map { nameToTagMap!![it]!!.docId }
        }

        when (tabId) {
            TabPageIds.CURRENT.ordinal -> modifiedTask.apply {
                completedTime = Timestamp.now()
                completed = true
                current = false
            }
            TabPageIds.COMPLETED.ordinal -> modifiedTask.apply {
                completed = false
                current = true
            }
            TabPageIds.SAVED.ordinal -> {
                val modifiedSavedOrdering = modifiedOrdering.saved.toMutableList()
                val modifiedCurrentOrdering = modifiedOrdering.saved.toMutableList().also {
                    it.remove(task.docId)
                }
                if (!modifiedCurrentOrdering.contains(task.docId)) {
                    modifiedCurrentOrdering.add(task.docId)
                }
                modifiedOrdering.apply {
                    current = modifiedCurrentOrdering
                    saved = modifiedSavedOrdering
                }
                modifiedTask.apply {
                    completed = true
                    current = false
                }
            }
        }

        if (modifiedTask.completed) {
            incrementTagsNumberCounter(task)
        }
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.completeTask(uid, modifiedTask, modifiedOrdering)
        }
    }

    private fun incrementTagsNumberCounter(task: Task) {
        val nameToTagMap = tagsLiveData.value?.associate { it.name to it }
        val tags = task.tags.map { nameToTagMap!![it]!!.copy() }
        for (tag in tags) {
            tag.numberCompleted += 1
        }
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.modifyTags(tags)
        }
    }

    fun saveTask(task: Task, tabId: Int) {
        val uid = AuthRepository.getCurrentUser()!!.uid
        val modifiedOrderings = orderingsLiveData.value!!.copy()
        val modifiedSavedOrdering = modifiedOrderings.saved.toMutableList()

        when (tabId) {
            TabPageIds.SAVED.ordinal -> {
                modifiedSavedOrdering.remove(task.docId)
                task.saved = false
            }
            else -> {
                if (task.saved) {
                    modifiedSavedOrdering.remove(task.docId)
                    task.saved = false
                } else {
                    if (!modifiedSavedOrdering.contains(task.docId)) {
                        modifiedSavedOrdering.add(task.docId)
                    }
                    task.saved = true
                }
            }
        }
        modifiedOrderings.saved = modifiedSavedOrdering

        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.setOrderings(uid, modifiedOrderings)
            FirestoreRepository.modifyTaskSave(task)
        }
    }

    fun addSavedTaskToCurrent(task: Task) {
        val uid = AuthRepository.getCurrentUser()!!.uid
        val modifiedOrderings = orderingsLiveData.value!!.copy()
        val modifiedCurrentOrdering = modifiedOrderings.current.toMutableList()
        if (!modifiedCurrentOrdering.contains(task.docId)) {
            modifiedCurrentOrdering.add(task.docId)
        }
        val modifiedTask = task.copy().apply {
            completed = false
            current = true
        }
        modifiedOrderings.current = modifiedCurrentOrdering

        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.setOrderings(uid, modifiedOrderings)
            FirestoreRepository.modifyTaskCompletion(modifiedTask)
        }
    }
}