package com.example.exoself.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class LiveDataHelper {

    companion object {
        private fun <T> makeLiveData(query: (String) -> Flow<T>): LiveData<T> =
            liveData(Dispatchers.IO) {
                AuthRepository.getCurrentUser()?.let {
                    query(it.uid).collect { response -> emit(response) }
                }
            }

        fun getTasksData() = makeLiveData(FirestoreRepository::getTasks)
        fun getTagsData() = makeLiveData(FirestoreRepository::getTags)
        fun getOrderingsData() = makeLiveData(FirestoreRepository::getOrderings)
        fun getProfileData() = makeLiveData(FirestoreRepository::getProfile)
    }
}