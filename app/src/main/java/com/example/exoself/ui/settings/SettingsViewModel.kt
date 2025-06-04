package com.example.exoself.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exoself.domain.Profile
import com.example.exoself.repositories.AuthRepository
import com.example.exoself.repositories.FirestoreRepository
import com.example.exoself.repositories.LiveDataHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val _profile = MutableLiveData<Profile>()
    var profileLiveData: LiveData<Profile> = _profile

    init {
        viewModelScope.launch {
            profileLiveData = LiveDataHelper.getProfileData()
        }
    }

    fun setTimerDuration(time: Int) {
        val uid = AuthRepository.getCurrentUser()!!.uid
        val modifiedProfile = profileLiveData.value!!.copy()
        modifiedProfile.timerDuration = time.toLong()
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.setProfile(uid, modifiedProfile)
        }
    }

    fun getAuthState(): LiveData<Boolean> {
        return AuthRepository.getFirebaseAuthState()
    }
}