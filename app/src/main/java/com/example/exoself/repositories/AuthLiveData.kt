package com.example.exoself.repositories

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth

class AuthLiveData(
    private val auth: FirebaseAuth
) : LiveData<Boolean>(), FirebaseAuth.AuthStateListener {

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        value = auth.currentUser == null
    }

    override fun onActive() {
        super.onActive()
        auth.addAuthStateListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        auth.removeAuthStateListener(this)
    }
}