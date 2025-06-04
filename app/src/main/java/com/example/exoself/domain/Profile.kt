package com.example.exoself.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Profile(
    var uid: String = "",
    var timerDuration: Long = 30,
    @ServerTimestamp val createdTime: Timestamp? = null,
    @DocumentId var docId: String = ""
)
