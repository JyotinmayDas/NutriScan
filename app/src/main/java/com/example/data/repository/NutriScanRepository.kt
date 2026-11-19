package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.local.LocalDiaryDao
import com.example.data.local.LocalDiaryEntity
import com.example.data.local.NutriScanDatabase
import com.example.model.AiCoachMessage
import com.example.model.FoodDiaryEntry
import com.example.model.MealPlanEntry
import com.example.model.SavedRecipe
import com.example.model.SavedRecipeItem
import com.example.model.UserProfile
import com.example.model.WaterRecord
import com.example.model.WeightRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
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
    private val localDiaryDao: LocalDiaryDao? = null,
    val appContext: Context? = null
) {
    companion object {
        private const val TAG = "NutriScanRepo"

        /**
         * Configures Firestore persistent disk cache for seamless offline access.
         */
        fun configureFirestorePersistence(db: FirebaseFirestore) {
            try {
                val current = db.firestoreSettings
                val newSettings = FirebaseFirestoreSettings.Builder(current)
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                db.firestoreSettings = newSettings
                Log.d(TAG, "Firestore persistent local cache configured successfully.")
            } catch (e: Exception) {
                // If firestore instance settings are already locked or configured
                Log.w(TAG, "Firestore local persistence configuration note: ${e.message}")
            }
        }
    }

    init {
        configureFirestorePersistence(db)
    }

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        NutriScanDatabase.getInstance(context.applicationContext).localDiaryDao(),
        context.applicationContext
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- LOCAL ON-DEVICE PROFILE PERSISTENCE (SharedPreferences: nutriscan_profile_prefs) ---

    fun saveLocalUserProfile(profile: UserProfile) {
        val ctx = appContext ?: return
        val keyPrefix = "profile_${profile.userId.ifBlank { "default" }}_"
        ctx.getSharedPreferences("nutriscan_profile_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("${keyPrefix}userId", profile.userId)
            .putString("${keyPrefix}name", profile.name)
            .putString("${keyPrefix}email", profile.email)
            .putInt("${keyPrefix}age", profile.age)
            .putString("${keyPrefix}gender", profile.gender)
            .putFloat("${keyPrefix}height", profile.height.toFloat())
            .putFloat("${keyPrefix}currentWeight", profile.currentWeight.toFloat())
            .putFloat("${keyPrefix}targetWeight", profile.targetWeight.toFloat())
            .putString("${keyPrefix}activityLevel", profile.activityLevel)
            .putString("${keyPrefix}fitnessGoal", profile.fitnessGoal)
            .putString("${keyPrefix}dietaryPreference", profile.dietaryPreference)
            .putInt("${keyPrefix}dailyCalorieGoal", profile.dailyCalorieGoal)
            .putInt("${keyPrefix}dailyProteinGoalG", profile.dailyProteinGoalG)
            .putInt("${keyPrefix}dailyCarbsGoalG", profile.dailyCarbsGoalG)
            .putInt("${keyPrefix}dailyFatGoalG", profile.dailyFatGoalG)
            .putInt("${keyPrefix}dailyFiberGoalG", profile.dailyFiberGoalG)
            .putBoolean("${keyPrefix}useCustomNutritionGoals", profile.useCustomNutritionGoals)
            .putInt("${keyPrefix}dailyWaterGoalMl", profile.dailyWaterGoalMl)
            .putString("${keyPrefix}language", profile.language)
            .putBoolean("${keyPrefix}isPremium", profile.isPremium)
            .putInt("${keyPrefix}dailyScansUsed", profile.dailyScansUsed)
            .putString("${keyPrefix}lastScanDate", profile.lastScanDate)
            .putLong("${keyPrefix}savedAtMillis", System.currentTimeMillis())
            .apply()
    }

    fun getLocalUserProfile(userId: String): UserProfile? {
        val ctx = appContext ?: return null
        val prefs = ctx.getSharedPreferences("nutriscan_profile_prefs", Context.MODE_PRIVATE)
        val keyPrefix = "profile_${userId.ifBlank { "default" }}_"
        if (!prefs.contains("${keyPrefix}name")) return null
        val calGoal = prefs.getInt("${keyPrefix}dailyCalorieGoal", 1850)
        return UserProfile(
            userId = prefs.getString("${keyPrefix}userId", userId) ?: userId,
            name = prefs.getString("${keyPrefix}name", "NutriScan Explorer") ?: "NutriScan Explorer",
            email = prefs.getString("${keyPrefix}email", "") ?: "",
            age = prefs.getInt("${keyPrefix}age", 28),
            gender = prefs.getString("${keyPrefix}gender", "Prefer not to say") ?: "Prefer not to say",
            height = prefs.getFloat("${keyPrefix}height", 168f).toDouble(),
            currentWeight = prefs.getFloat("${keyPrefix}currentWeight", 68f).toDouble(),
            targetWeight = prefs.getFloat("${keyPrefix}targetWeight", 62f).toDouble(),
            activityLevel = prefs.getString("${keyPrefix}activityLevel", "Moderately Active") ?: "Moderately Active",
            fitnessGoal = prefs.getString("${keyPrefix}fitnessGoal", "Lose Weight") ?: "Lose Weight",
            dietaryPreference = prefs.getString("${keyPrefix}dietaryPreference", "Bengali Traditional") ?: "Bengali Traditional",
            dailyCalorieGoal = calGoal,
            dailyProteinGoalG = prefs.getInt("${keyPrefix}dailyProteinGoalG", ((calGoal * 0.25) / 4.0).toInt()),
            dailyCarbsGoalG = prefs.getInt("${keyPrefix}dailyCarbsGoalG", ((calGoal * 0.50) / 4.0).toInt()),
            dailyFatGoalG = prefs.getInt("${keyPrefix}dailyFatGoalG", ((calGoal * 0.25) / 9.0).toInt()),
            dailyFiberGoalG = prefs.getInt("${keyPrefix}dailyFiberGoalG", 30),
            useCustomNutritionGoals = prefs.getBoolean("${keyPrefix}useCustomNutritionGoals", false),
            dailyWaterGoalMl = prefs.getInt("${keyPrefix}dailyWaterGoalMl", 2500),
            language = prefs.getString("${keyPrefix}language", "en") ?: "en",
            isPremium = prefs.getBoolean("${keyPrefix}isPremium", false),
            dailyScansUsed = prefs.getInt("${keyPrefix}dailyScansUsed", 0),
            lastScanDate = prefs.getString("${keyPrefix}lastScanDate", "") ?: ""
        )
    }

    private fun mergeWithLocalMacroGoals(remote: UserProfile, userId: String): UserProfile {
        val local = getLocalUserProfile(userId) ?: return remote
        return remote.copy(
            dailyProteinGoalG = local.dailyProteinGoalG,
            dailyCarbsGoalG = local.dailyCarbsGoalG,
            dailyFatGoalG = local.dailyFatGoalG,
            dailyFiberGoalG = local.dailyFiberGoalG,
            useCustomNutritionGoals = local.useCustomNutritionGoals
        )
    }

    // --- USER PROFILE (CLOUD FIRESTORE + LOCAL CACHE) ---

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        getLocalUserProfile(userId)?.let { emit(it) }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.let { mergeWithLocalMacroGoals(it, userId) }
                            ?.also { saveLocalUserProfile(it) }
                    } else {
                        getLocalUserProfile(userId)
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
                    ?.let { mergeWithLocalMacroGoals(it, userId) }
                    ?.also { saveLocalUserProfile(it) }
            } else {
                getLocalUserProfile(userId)
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
        val withUid = profile.copy(userId = uid)
        // Always persist locally first for instant offline availability
        saveLocalUserProfile(withUid)
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(withUid.toUpdateMap()).await()
            } else {
                docRef.set(withUid.toCreateMap()).await()
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
        // 1. Immediately emit offline Room cached entries so launch is instant without network
        try {
            localDiaryDao?.getCachedEntries(userId)?.let { cached ->
                if (cached.isNotEmpty()) {
                    emit(cached.map { it.toDomain() })
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local Room cache read exception: ${e.message}")
        }

        // 2. Observe Firestore with MetadataChanges.INCLUDE so local cache mutations are immediately visible offline
        emitAll(
            db.collection("users").document(userId).collection("foodDiary")
                .whereEqualTo("userId", userId)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    val entries = snapshot.toObjects(
                        FoodDiaryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (entries.isNotEmpty()) {
                        localDiaryDao?.upsertEntries(entries.map { LocalDiaryEntity.fromDomain(it) })
                    }
                    entries
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    val isNetworkUnavailable = error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.UNAVAILABLE
                    val dao = localDiaryDao
                    if (isNetworkUnavailable && dao != null) {
                        dao.observeCachedEntries(userId).collect { list ->
                            emit(list.map { it.toDomain() })
                        }
                    } else {
                        throw error
                    }
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
            val snap = try {
                docRef.get().await()
            } catch (netErr: FirebaseFirestoreException) {
                if (netErr.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    docRef.get(Source.CACHE).await()
                } else {
                    throw netErr
                }
            }
            snap.toObject(FoodDiaryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    /**
     * Real-time Firestore stream of the last [limit] (default 5) food items from `users/{userId}/foodDiary`.
     * Uses Firestore local persistent cache and MetadataChanges.INCLUDE to ensure items remain
     * accessible and responsive even when completely offline.
     */
    fun observeRecentScans(userId: String = requireUserId(), limit: Int = 5): Flow<List<FoodDiaryEntry>> = flow {
        val path = "users/$userId/foodDiary"
        // 1. Immediately emit from local Room cache for instantaneous offline load
        try {
            localDiaryDao?.getCachedEntries(userId)?.let { cached ->
                if (cached.isNotEmpty()) {
                    emit(sortAndTakeRecentItems(cached.map { it.toDomain() }, limit))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local cache preload error: ${e.message}")
        }

        // 2. Observe Firestore with MetadataChanges.INCLUDE so local cache updates reflect offline immediately
        emitAll(
            db.collection("users").document(userId).collection("foodDiary")
                .whereEqualTo("userId", userId)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    val allItems = snapshot.toObjects(
                        FoodDiaryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (allItems.isNotEmpty()) {
                        localDiaryDao?.upsertEntries(allItems.map { LocalDiaryEntity.fromDomain(it) })
                    }
                    sortAndTakeRecentItems(allItems, limit)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    // Stream from Room local cache as an offline guarantee
                    localDiaryDao?.observeCachedEntries(userId)?.collect { list ->
                        emit(sortAndTakeRecentItems(list.map { it.toDomain() }, limit))
                    }
                }
        )
    }

    /**
     * On-demand fetch of the last [limit] (default 5) food items.
     * Tries default server fetch, seamlessly falling back to Firestore persistent cache (Source.CACHE)
     * and local Room database when the user is offline.
     */
    suspend fun fetchRecentScans(userId: String = requireUserId(), limit: Int = 5): Result<List<FoodDiaryEntry>> = runCatching {
        val colRef = db.collection("users").document(userId).collection("foodDiary")
        val snap = try {
            colRef.whereEqualTo("userId", userId).get().await()
        } catch (netErr: Exception) {
            Log.d(TAG, "Network get failed, fetching from Firestore local persistent cache: ${netErr.message}")
            try {
                colRef.whereEqualTo("userId", userId).get(Source.CACHE).await()
            } catch (cacheErr: Exception) {
                Log.w(TAG, "Firestore cache fetch exception: ${cacheErr.message}")
                null
            }
        }
        val allItems = snap?.toObjects(
            FoodDiaryEntry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: emptyList()

        val sorted = sortAndTakeRecentItems(allItems, limit)
        if (sorted.isEmpty()) {
            val localItems = localDiaryDao?.getCachedEntries(userId)?.let { cached ->
                sortAndTakeRecentItems(cached.map { it.toDomain() }, limit)
            } ?: emptyList()
            sorted.ifEmpty { localItems }
        } else {
            localDiaryDao?.upsertEntries(allItems.map { LocalDiaryEntity.fromDomain(it) })
            sorted
        }
    }

    private fun sortAndTakeRecentItems(
        items: List<FoodDiaryEntry>,
        limit: Int
    ): List<FoodDiaryEntry> {
        return items.sortedWith(
            compareByDescending<FoodDiaryEntry> {
                it.createdAt?.seconds ?: it.updatedAt?.seconds ?: 0L
            }.thenByDescending {
                it.createdAt?.nanoseconds ?: it.updatedAt?.nanoseconds ?: 0
            }.thenByDescending {
                it.dateKey
            }
        ).take(limit.coerceAtLeast(1))
    }

    // --- WATER TRACKER ---

    fun observeWaterHistory(userId: String = requireUserId()): Flow<List<WaterRecord>> = flow {
        val path = "users/$userId/waterHistory"
        emitAll(
            db.collection("users").document(userId).collection("waterHistory")
                .whereEqualTo("userId", userId)
                .snapshots(MetadataChanges.INCLUDE)
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

    suspend fun updateWaterRecordQuantity(recordId: String, newQuantityMl: Int, dateKey: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(recordId.isNotBlank()) { "Water record ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("waterHistory").document(recordId)
        try {
            if (newQuantityMl <= 0) {
                docRef.delete().await()
            } else {
                val updatedRecord = WaterRecord(id = recordId, userId = uid, quantity = newQuantityMl.coerceIn(1, 5000), dateKey = dateKey)
                docRef.update(updatedRecord.toUpdateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Decrements today's water consumption by [stepMl] directly in Cloud Firestore
     * by reducing or deleting the most recent waterHistory documents for [dateKey].
     * Returns the actual milliliters subtracted.
     */
    suspend fun decrementDailyWaterIntake(
        stepMl: Int,
        dateKey: String,
        currentDayLogs: List<WaterRecord>
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        val safeStep = stepMl.coerceIn(10, 5000)
        val dayRecords = currentDayLogs
            .filter { it.dateKey == dateKey && it.id.isNotBlank() }
            .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }

        var remainingToSubtract = safeStep
        var totalSubtracted = 0

        for (rec in dayRecords) {
            if (remainingToSubtract <= 0) break
            val docRef = db.collection("users").document(uid).collection("waterHistory").document(rec.id)
            try {
                if (rec.quantity <= remainingToSubtract) {
                    docRef.delete().await()
                    remainingToSubtract -= rec.quantity
                    totalSubtracted += rec.quantity
                } else {
                    val newQty = (rec.quantity - remainingToSubtract).coerceIn(1, 5000)
                    val updated = WaterRecord(id = rec.id, userId = uid, quantity = newQty, dateKey = dateKey)
                    docRef.update(updated.toUpdateMap()).await()
                    totalSubtracted += remainingToSubtract
                    remainingToSubtract = 0
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
                throw e
            }
        }
        totalSubtracted
    }

    // --- WEIGHT TRACKER ---

    fun observeWeightHistory(userId: String = requireUserId()): Flow<List<WeightRecord>> = flow {
        val path = "users/$userId/weightHistory"
        emitAll(
            db.collection("users").document(userId).collection("weightHistory")
                .whereEqualTo("userId", userId)
                .snapshots(MetadataChanges.INCLUDE)
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
                .snapshots(MetadataChanges.INCLUDE)
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
                .snapshots(MetadataChanges.INCLUDE)
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

    // --- SAVED RECIPES ("MY RECIPES": FAVORITE SCANNED COMBOS & CUSTOM MEAL PLANS) ---

    fun observeSavedRecipes(userId: String = requireUserId()): Flow<List<SavedRecipe>> = flow {
        val path = "users/$userId/savedRecipes"
        emitAll(
            db.collection("users").document(userId).collection("savedRecipes")
                .whereEqualTo("userId", userId)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(SavedRecipe::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<SavedRecipe> { it.isFavorite }
                                .thenByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                        )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveRecipe(recipe: SavedRecipe): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("savedRecipes")
        try {
            val docRef = colRef.document()
            val normalized = recipe.copy(userId = uid).withRecalculatedTotals()
            docRef.set(normalized.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    suspend fun updateSavedRecipe(recipe: SavedRecipe): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(recipe.id.isNotBlank()) { "SavedRecipe ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("savedRecipes").document(recipe.id)
        try {
            val normalized = recipe.copy(userId = uid).withRecalculatedTotals()
            docRef.update(normalized.toUpdateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSavedRecipe(recipeId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("savedRecipes").document(recipeId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getSavedRecipeById(targetUserId: String, recipeId: String): Result<SavedRecipe?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("savedRecipes").document(recipeId)
        try {
            val snap = docRef.get().await()
            snap.toObject(SavedRecipe::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun getLocalGuestRecipes(userId: String): List<SavedRecipe> {
        val ctx = appContext ?: return emptyList()
        val raw = ctx.getSharedPreferences("nutriscan_saved_recipes_prefs", Context.MODE_PRIVATE)
            .getString("recipes_$userId", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val itemsArr = obj.optJSONArray("items")
                    val parsedItems = buildList {
                        if (itemsArr != null) {
                            for (j in 0 until itemsArr.length()) {
                                val itObj = itemsArr.optJSONObject(j) ?: continue
                                add(
                                    SavedRecipeItem(
                                        foodName = itObj.optString("foodName", ""),
                                        quantityG = itObj.optDouble("quantityG", 100.0),
                                        caloriesKcal = itObj.optInt("caloriesKcal", 0),
                                        proteinG = itObj.optDouble("proteinG", 0.0),
                                        carbohydratesG = itObj.optDouble("carbohydratesG", 0.0),
                                        fatG = itObj.optDouble("fatG", 0.0),
                                        fiberG = itObj.optDouble("fiberG", 0.0),
                                        mealSlot = itObj.optString("mealSlot", "Lunch")
                                    )
                                )
                            }
                        }
                    }
                    add(
                        SavedRecipe(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", userId),
                            title = obj.optString("title", ""),
                            category = obj.optString("category", SavedRecipe.CATEGORY_SCANNED_COMBO),
                            mealType = obj.optString("mealType", "Lunch"),
                            totalCalories = obj.optInt("totalCalories", 0),
                            totalProteinG = obj.optDouble("totalProteinG", 0.0),
                            totalCarbsG = obj.optDouble("totalCarbsG", 0.0),
                            totalFatG = obj.optDouble("totalFatG", 0.0),
                            totalFiberG = obj.optDouble("totalFiberG", 0.0),
                            totalWeightG = obj.optDouble("totalWeightG", 0.0),
                            items = parsedItems,
                            notes = obj.optString("notes", ""),
                            isFavorite = obj.optBoolean("isFavorite", true),
                            usageCount = obj.optInt("usageCount", 0)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse local guest recipes: ${e.message}")
            emptyList()
        }
    }

    fun saveLocalGuestRecipes(userId: String, recipes: List<SavedRecipe>) {
        val ctx = appContext ?: return
        try {
            val arr = org.json.JSONArray()
            recipes.forEach { recipe ->
                val obj = org.json.JSONObject().apply {
                    put("id", recipe.id)
                    put("userId", recipe.userId)
                    put("title", recipe.title)
                    put("category", recipe.category)
                    put("mealType", recipe.mealType)
                    put("totalCalories", recipe.totalCalories)
                    put("totalProteinG", recipe.totalProteinG)
                    put("totalCarbsG", recipe.totalCarbsG)
                    put("totalFatG", recipe.totalFatG)
                    put("totalFiberG", recipe.totalFiberG)
                    put("totalWeightG", recipe.totalWeightG)
                    put("notes", recipe.notes)
                    put("isFavorite", recipe.isFavorite)
                    put("usageCount", recipe.usageCount)
                    val itemsArr = org.json.JSONArray()
                    recipe.items.forEach { item ->
                        itemsArr.put(
                            org.json.JSONObject().apply {
                                put("foodName", item.foodName)
                                put("quantityG", item.quantityG)
                                put("caloriesKcal", item.caloriesKcal)
                                put("proteinG", item.proteinG)
                                put("carbohydratesG", item.carbohydratesG)
                                put("fatG", item.fatG)
                                put("fiberG", item.fiberG)
                                put("mealSlot", item.mealSlot)
                            }
                        )
                    }
                    put("items", itemsArr)
                }
                arr.put(obj)
            }
            ctx.getSharedPreferences("nutriscan_saved_recipes_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("recipes_$userId", arr.toString())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save local guest recipes: ${e.message}")
        }
    }
}
