package com.example.exoself.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Tag(
    var uid: String = "",
    var name: String = "",
    var numberCompleted: Int = 0,
    var minutesCompleted: Long = 0,
    @ServerTimestamp val createdTime: Timestamp? = null,
    @DocumentId var docId: String = ""
)