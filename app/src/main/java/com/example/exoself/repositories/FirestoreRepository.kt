package com.example.exoself.repositories

import android.util.Log
import com.example.exoself.domain.Orderings
import com.example.exoself.domain.Profile
import com.example.exoself.domain.Tag
import com.example.exoself.domain.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull

private const val TAG = "FirestoreRepository"

object FirestoreRepository {

    private const val PROFILES_COLLECTION = "profiles"
    private const val ORDERINGS_COLLECTION = "orderings"
    private const val TASKS_COLLECTION = "tasks"
    private const val TAGS_COLLECTION = "tags"
    private const val UID_KEY = "uid"

    fun addNewProfile(uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(PROFILES_COLLECTION).document(uid).get().addOnSuccessListener { doc ->
            if (doc.toObject<Profile>() == null) {
                db.runBatch { batch ->
                    batch.set(db.collection(PROFILES_COLLECTION).document(uid), Profile(uid))
                    batch.set(db.collection(ORDERINGS_COLLECTION).document(uid), Orderings(uid))
                }.addOnFailureListener {
                    Log.e(TAG, "exception", it)
                }
            }
        }.addOnFailureListener {
            Log.e(TAG, "exception", it)
        }
    }

    fun getProfile(uid: String): Flow<Profile> {
        val db = FirebaseFirestore.getInstance()
        return db.collection(PROFILES_COLLECTION)
            .document(uid)
            .snapshots().mapNotNull {
                it.toObject(Profile::class.java)
            }
    }

    fun setProfile(uid: String, profile: Profile) {
        val db = FirebaseFirestore.getInstance()
        db.collection(PROFILES_COLLECTION).document(uid).set(profile)
            .addOnFailureListener {
                Log.e(TAG, "exception", it)
            }
    }

    fun modifyOrderingByName(uid: String, ordering: List<String>, listName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(ORDERINGS_COLLECTION).document(uid).update(listName, ordering)
            .addOnFailureListener {
                Log.e(TAG, "exception", it)
            }
    }

    fun setOrderings(uid: String, ordering: Orderings) {
        val db = FirebaseFirestore.getInstance()
        db.collection(ORDERINGS_COLLECTION).document(uid).set(ordering)
            .addOnFailureListener {
                Log.e(TAG, "exception", it)
            }
    }

    fun deleteTask(uid: String, docId: String, ordering: Orderings, fullyDelete: Boolean) {
        val db = FirebaseFirestore.getInstance()
        db.runBatch { batch ->
            batch.set(db.collection(ORDERINGS_COLLECTION).document(uid), ordering)
            if (fullyDelete) {
                batch.delete(db.collection(TASKS_COLLECTION).document(docId))
            }
        }.addOnFailureListener {
            Log.e(TAG, "exception", it)
        }
    }

    fun completeTask(uid: String, task: Task, orderings: Orderings) {
        val db = FirebaseFirestore.getInstance()
        db.runBatch { batch ->
            batch.set(db.collection(ORDERINGS_COLLECTION).document(uid), orderings)
            batch.set(db.collection(TASKS_COLLECTION).document(task.docId), task)
        }.addOnFailureListener {
            Log.e(TAG, "exception", it)
        }
    }

    fun setTask(task: Task) {
        val db = FirebaseFirestore.getInstance()
        db.collection(TASKS_COLLECTION).document(task.docId).set(task)
            .addOnFailureListener {
                Log.e(TAG, "exception", it)
            }
    }

    fun modifyTaskSave(task: Task) {
        val db = FirebaseFirestore.getInstance()
        db.collection(TASKS_COLLECTION).document(task.docId).update("saved", task.saved)
            .addOnFailureListener {
                Log.e(TAG, "exception", it)
            }
    }

    fun modifyTaskCompletion(task: Task) {
        val db = FirebaseFirestore.getInstance()
        db.runBatch { batch ->
            batch.update(
                db.collection(TASKS_COLLECTION).document(task.docId),
                "completed",
                task.completed
            )
            batch.update(
                db.collection(TASKS_COLLECTION).document(task.docId),
                "current",
                task.current
            )
        }.addOnFailureListener {
            Log.e(TAG, "exception", it)
        }
    }

    fun editTask(
        uid: String,
        existingTask: Task,
        existingTagsAsIds: List<String>,
        newTagsAsNames: List<String>,
    ) {
        val db = FirebaseFirestore.getInstance()

        val newTagIds: MutableList<String> = mutableListOf()
        val newTagAndDocRefs: MutableList<Pair<DocumentReference, Tag>> = mutableListOf()
        for (tag in newTagsAsNames) {
            val newTagDocRef = db.collection(TAGS_COLLECTION).document()
            newTagIds.add(newTagDocRef.id)
            newTagAndDocRefs.add(Pair(newTagDocRef, Tag(uid, tag)))
        }
        existingTask.tags = existingTagsAsIds + newTagIds

        db.runBatch { batch ->
            for (pair in newTagAndDocRefs) {
                batch.set(pair.first, pair.second)
            }
            batch.set(db.collection(TASKS_COLLECTION).document(existingTask.docId), existingTask)
        }.addOnFailureListener {
            Log.e(TAG, "exception", it)
        }
    }

    fun addTask(
        uid: String,
        newTask: Task,
        existingTagsAsIds: List<String>,
        newTagsAsNames: List<String>
    ) {
        val db = FirebaseFirestore.getInstance()

        val newTagIds: MutableList<String> = mutableListOf()
        val newTagAndDocRefs: MutableList<Pair<DocumentReference, Tag>> = mutableListOf()
        for (tag in newTagsAsNames) {
            val newTagDocRef = db.collection(TAGS_COLLECTION).document()
            newTagIds.add(newTagDocRef.id)
            newTagAndDocRefs.add(Pair(newTagDocRef, Tag(uid = uid, name = tag)))
        }
        newTask.tags = existingTagsAsIds + newTagIds

        val newTaskDocRef = db.collection(TASKS_COLLECTION).document()
        val orderingsDocRef = db.collection(ORDERINGS_COLLECTION).document(uid)

        orderingsDocRef.get()
            .addOnSuccessListener { doc ->
                val orderings = doc.toObject<Orderings>()?.current ?: listOf()
                val newCurrentOrdering = orderings + newTaskDocRef.id
                db.runBatch { batch ->
                    for (pair in newTagAndDocRefs) {
                        batch.set(pair.first, pair.second)
                    }
                    batch.set(newTaskDocRef, newTask)
                    batch.update(orderingsDocRef, "current", newCurrentOrdering)
                }.addOnFailureListener {
                    Log.e(TAG, "exception", it)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "exception", it)
            }
    }

    fun getTasks(uid: String): Flow<List<Task>> {
        val db = FirebaseFirestore.getInstance()
        try {
            return db.collection(TASKS_COLLECTION)
                .whereEqualTo(UID_KEY, uid)
                .snapshots().mapNotNull {
                    it.toObjects(Task::class.java)
                }
        } catch (e: Exception) {
            Log.e(TAG, "exception", e)
        }
        return flowOf(listOf())
    }

    fun getTask(taskId: String): Flow<Task> {
        val db = FirebaseFirestore.getInstance()
        return db.collection(TASKS_COLLECTION)
            .document(taskId)
            .snapshots().mapNotNull {
                it.toObject(Task::class.java)
            }
    }

    fun getTags(uid: String): Flow<List<Tag>> {
        val db = FirebaseFirestore.getInstance()
        return db.collection(TAGS_COLLECTION)
            .whereEqualTo(UID_KEY, uid)
            .snapshots().mapNotNull {
                it.toObjects(Tag::class.java)
            }
    }

    fun modifyTags(tags: List<Tag>) {
        val db = FirebaseFirestore.getInstance()
        db.runBatch { batch ->
            for (tag in tags) {
                batch.set(db.collection(TAGS_COLLECTION).document(tag.docId), tag)
            }
        }.addOnFailureListener {
            Log.e(TAG, "exception", it)
        }
    }

    fun getOrderings(uid: String): Flow<Orderings> {
        val db = FirebaseFirestore.getInstance()
        return db.collection(ORDERINGS_COLLECTION)
            .document(uid)
            .snapshots().mapNotNull {
                it.toObject(Orderings::class.java)
            }
    }
}