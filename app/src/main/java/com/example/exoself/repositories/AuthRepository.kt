package com.example.exoself.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthRepository {

    fun getFirebaseAuthState(): AuthLiveData {
        return AuthLiveData(FirebaseAuth.getInstance())
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun signOut(){
        FirebaseAuth.getInstance().signOut()
    }
}