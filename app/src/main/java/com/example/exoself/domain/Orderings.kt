package com.example.exoself.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Orderings(
    var uid: String = "",
    var current: List<String> = listOf(),
    var saved: List<String> = listOf(),
    @ServerTimestamp val createdTime: Timestamp? = null,
    @DocumentId var docId: String = ""
)
