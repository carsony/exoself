package com.example.exoself.ui.stats

import androidx.lifecycle.*
import com.example.exoself.domain.Tag
import com.example.exoself.repositories.AuthRepository
import com.example.exoself.repositories.LiveDataHelper
import kotlinx.coroutines.launch

class StatsViewModel : ViewModel() {

    private val _tags = MutableLiveData<List<Tag>>()
    private var tagsLiveData: LiveData<List<Tag>> = _tags
    var topTenTags = MediatorLiveData<List<Tag>>()

    init {
        viewModelScope.launch {
            tagsLiveData = LiveDataHelper.getTagsData()
        }
        initTopTenTags()
    }

    private fun initTopTenTags() {
        topTenTags.apply {
            addSource(tagsLiveData) { tags ->
                value = tags.sortedBy { it.minutesCompleted }.takeLast(10)
            }
        }
    }

    fun getAuthState(): LiveData<Boolean> {
        return AuthRepository.getFirebaseAuthState()
    }
}