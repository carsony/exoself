package com.example.exoself.ui.taskedit

import androidx.lifecycle.*
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

class TaskEditViewModel : ViewModel() {

    private var _existingTask: LiveData<Task>? = null
    private val _tags = MutableLiveData<List<Tag>>()
    private var tagsLiveData: LiveData<List<Tag>> = _tags
    var nameToTagMap = MediatorLiveData<Map<String, Tag>>()
    var idToTagMap = MediatorLiveData<Map<String, Tag>>()
    var tagsNameSet: MutableSet<String> = mutableSetOf()
    var scheduledTime = MutableLiveData<Calendar>().apply {
        value = Calendar.getInstance()
    }
    var willSchedule = MutableLiveData<Boolean>().apply {
        value = false
    }

    init {
        viewModelScope.launch {
            tagsLiveData = LiveDataHelper.getTagsData()
        }
        initTagsMapLiveData()
    }

    fun getTask(id: String): LiveData<Task> {
        return liveData(Dispatchers.IO) {
            FirestoreRepository.getTask(id).collect { response ->
                emit(response)
            }
        }.also {
            _existingTask = it
        }
    }

    private fun initTagsMapLiveData() {
        idToTagMap.apply {
            addSource(tagsLiveData) { value = tagsLiveData.value?.associate { it.docId to it } }
        }
        nameToTagMap.apply {
            addSource(tagsLiveData) { value = tagsLiveData.value?.associate { it.name to it } }
        }
    }

    fun editTask(description: String, color: Int) {
        if (description.isEmpty()) {
            return
        }
        val uid = AuthRepository.getCurrentUser()!!.uid
        val task = _existingTask?.value!!.apply {
            this.description = description
            this.color = color
        }
        val existingTagsAsIds = tagsNameSet.filter { nameToTagMap.value?.contains(it) ?: true }
            .mapNotNull { nameToTagMap.value?.get(it)?.docId }

        val newTagsAsNames = tagsNameSet.filterNot {
            nameToTagMap.value?.contains(it) ?: true
        }

        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.editTask(
                uid, task, existingTagsAsIds, newTagsAsNames
            )
        }
    }

    fun addTask(description: String, color: Int) {
        if (description.isEmpty()) {
            return
        }
        val uid = AuthRepository.getCurrentUser()!!.uid
        val task = Task(uid, description, color)

        val existingTagsAsIds = tagsNameSet.filter { nameToTagMap.value?.contains(it) ?: true }
            .mapNotNull { nameToTagMap.value?.get(it)?.docId }
        val newTagsAsNames = tagsNameSet.filterNot {
            nameToTagMap.value?.contains(it) ?: true
        }
        
        if (willSchedule.value == true) {
            task.scheduledTime = scheduledTime.value?.let { Timestamp(it.time) }
            task.current = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.addTask(
                uid, task, existingTagsAsIds, newTagsAsNames
            )
        }
    }

    fun setScheduleDate(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal[Calendar.YEAR] = year
        cal[Calendar.MONTH] = month
        cal[Calendar.DAY_OF_MONTH] = day
        scheduledTime.postValue(cal)
    }

    fun setWillSchedule() {
        willSchedule.postValue(true)
    }

    fun setWillNotSchedule() {
        willSchedule.postValue(false)
    }
}