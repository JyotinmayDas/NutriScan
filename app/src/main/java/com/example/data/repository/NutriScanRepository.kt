package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.local.LocalDiaryDao
import com.example.data.local.LocalDiaryEntity
import com.example.data.local.NutriScanDatabase
import com.example.model.AiCoachMessage
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

class NutriScanRepository(
    private val db: FirebaseFirestore,
    private val localDiaryDao: LocalDiaryDao? = null
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        NutriScanDatabase.getInstance(context.applicationContext).localDiaryDao()
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getUserProfile(userId: String = requireUserId()): Result<UserProfile?> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(profile.copy(userId = uid).toUpdateMap()).await()
            } else {
                docRef.set(profile.copy(userId = uid).toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteUserAccountData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- FOOD DIARY ---

    fun observeFoodDiary(userId: String = requireUserId()): Flow<List<FoodDiaryEntry>> = flow {
        val path = "users/$userId/foodDiary"
        emitAll(
            db.collection("users").document(userId).collection("foodDiary")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FoodDiaryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .onEach { entries ->
                    localDiaryDao?.upsertEntries(entries.map { LocalDiaryEntity.fromDomain(it) })
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOfflineCachedDiary(userId: String): Flow<List<FoodDiaryEntry>>? {
        return localDiaryDao?.observeCachedEntries(userId)?.map { list -> list.map { it.toDomain() } }
    }

    suspend fun addFoodDiaryEntry(entry: FoodDiaryEntry): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("foodDiary")
        try {
            val docRef = colRef.document()
            val payload = entry.copy(userId = uid).toCreateMap()
            docRef.set(payload).await()
            localDiaryDao?.upsertEntry(LocalDiaryEntity.fromDomain(entry.copy(id = docRef.id, userId = uid)))
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateFoodDiaryEntry(entry: FoodDiaryEntry): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(entry.id.isNotBlank()) { "Entry ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("foodDiary").document(entry.id)
        try {
            docRef.update(entry.copy(userId = uid).toUpdateMap()).await()
            localDiaryDao?.upsertEntry(LocalDiaryEntity.fromDomain(entry.copy(userId = uid)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFoodDiaryEntry(entryId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("foodDiary").document(entryId)
        try {
            docRef.delete().await()
            localDiaryDao?.deleteEntryById(entryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getFoodEntryById(targetUserId: String, entryId: String): Result<FoodDiaryEntry?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("foodDiary").document(entryId)
        try {
            val snap = docRef.get().await()
            snap.toObject(FoodDiaryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    // --- WATER TRACKER ---

    fun observeWaterHistory(userId: String = requireUserId()): Flow<List<WaterRecord>> = flow {
        val path = "users/$userId/waterHistory"
        emitAll(
            db.collection("users").document(userId).collection("waterHistory")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WaterRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addWaterRecord(quantityMl: Int, dateKey: String): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("waterHistory")
        try {
            val docRef = colRef.document()
            val record = WaterRecord(userId = uid, quantity = quantityMl, dateKey = dateKey)
            docRef.set(record.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun deleteWaterRecord(recordId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("waterHistory").document(recordId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- WEIGHT TRACKER ---

    fun observeWeightHistory(userId: String = requireUserId()): Flow<List<WeightRecord>> = flow {
        val path = "users/$userId/weightHistory"
        emitAll(
            db.collection("users").document(userId).collection("weightHistory")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WeightRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addWeightRecord(weightKg: Double, dateKey: String, note: String = ""): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("weightHistory")
        try {
            val docRef = colRef.document()
            val record = WeightRecord(userId = uid, weight = weightKg, dateKey = dateKey, note = note)
            docRef.set(record.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    // --- MEAL PLANS ---

    fun observeMealPlans(userId: String = requireUserId()): Flow<List<MealPlanEntry>> = flow {
        val path = "users/$userId/mealPlans"
        emitAll(
            db.collection("users").document(userId).collection("mealPlans")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MealPlanEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveMealPlan(plan: MealPlanEntry): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("mealPlans")
        try {
            val docRef = colRef.document()
            docRef.set(plan.copy(userId = uid).toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    // --- AI COACH HISTORY ---

    fun observeAiHistory(userId: String = requireUserId()): Flow<List<AiCoachMessage>> = flow {
        val path = "users/$userId/aiHistory"
        emitAll(
            db.collection("users").document(userId).collection("aiHistory")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AiCoachMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveAiCoachMessage(question: String, response: String, language: String): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("aiHistory")
        try {
            val docRef = colRef.document()
            val msg = AiCoachMessage(userId = uid, question = question, response = response, language = language)
            docRef.set(msg.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }
}
