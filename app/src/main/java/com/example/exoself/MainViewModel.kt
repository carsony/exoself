package com.example.exoself

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.exoself.repositories.AuthRepository
import com.example.exoself.repositories.FirestoreRepository

class MainViewModel : ViewModel() {

    fun addNewProfile() {
        val uid = AuthRepository.getCurrentUser()!!.uid
        FirestoreRepository.addNewProfile(uid)
    }

    fun getAuthState(): LiveData<Boolean> {
        return AuthRepository.getFirebaseAuthState()
    }

    fun signOut() {
        AuthRepository.signOut()
    }
}