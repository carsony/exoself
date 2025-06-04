package com.example.exoself.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class Task(
    var uid: String = "",
    var description: String = "",
    var color: Int? = -1,
    var tags: List<String> = listOf(),
    var scheduledTime: Timestamp? = null,
    var current: Boolean = true,
    var completed: Boolean = false,
    var saved: Boolean = false,
    var completedTime: Timestamp? = null,
    @get:Exclude var orderIdx: Int = 0,
    @ServerTimestamp val createdTime: Timestamp? = null,
    @DocumentId var docId: String = ""
)