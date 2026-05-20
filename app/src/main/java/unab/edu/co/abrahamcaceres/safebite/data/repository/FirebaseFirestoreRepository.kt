package unab.edu.co.abrahamcaceres.safebite.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ScanHistoryModel
import unab.edu.co.abrahamcaceres.safebite.model.cloud.SightingCloudModel
import unab.edu.co.abrahamcaceres.safebite.model.cloud.UserCloudModel

class FirebaseFirestoreRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun createUserProfile(user: UserCloudModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val uid = auth.currentUser?.uid
                    ?: return@withContext Result.failure(IllegalStateException(ERROR_NOT_AUTHENTICATED))
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .set(user.copy(uid = uid))
                    .await()
                Result.success(Unit)
            } catch (e: FirebaseFirestoreException) {
                Result.failure(IllegalStateException(e.message ?: ERROR_FIRESTORE))
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: ERROR_UNKNOWN))
            }
        }

    suspend fun fetchUserProfile(uid: String): Result<UserCloudModel> =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .await()
                val user = snapshot.toObject(UserCloudModel::class.java)
                    ?: return@withContext Result.failure(IllegalStateException("USER_NOT_FOUND"))
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: "UNKNOWN_ERROR"))
            }
        }

    suspend fun addScanToHistory(userId: String, scan: ScanHistoryModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(SUBCOLLECTION_SCANS)
                    .document(scan.scanId.ifBlank { firestore.collection("_").document().id })
                    .set(scan)
                    .await()
                Result.success(Unit)
            } catch (e: FirebaseFirestoreException) {
                Result.failure(IllegalStateException(e.message ?: ERROR_FIRESTORE))
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: ERROR_UNKNOWN))
            }
        }

    fun listenToSightings(
        city: String = "",
        onEvent: (List<SightingCloudModel>?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        var query = firestore.collection(COLLECTION_SIGHTINGS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
        if (city.isNotBlank()) {
            query = query.whereEqualTo(FIELD_TARGET_CITY, city)
        }
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onEvent(null, error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(SightingCloudModel::class.java)?.copy(sightingId = doc.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Malformed sighting doc ${doc.id}, recovering: ${e.message}")
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        SightingCloudModel(
                            sightingId = doc.id,
                            creatorUid = (data["creatorUid"] as? String) ?: "",
                            creatorName = (data["creatorName"] as? String) ?: "",
                            productName = (data["productName"] as? String) ?: "",
                            storeName = (data["storeName"] as? String) ?: "",
                            price = (data["price"] as? Double) ?: 0.0,
                            communityTip = (data["communityTip"] as? String) ?: "",
                            allergenTag = (data["allergenTag"] as? String) ?: "",
                            targetCity = (data["targetCity"] as? String) ?: "",
                            latitude = (data["latitude"] as? Double) ?: 0.0,
                            longitude = (data["longitude"] as? Double) ?: 0.0,
                            createdAt = Timestamp.now()
                        )
                    } catch (e2: Exception) {
                        Log.e(TAG, "Could not recover sighting doc ${doc.id}: ${e2.message}")
                        null
                    }
                }
            } ?: emptyList()
            onEvent(list, null)
        }
    }

    suspend fun postSighting(sighting: SightingCloudModel): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val creatorUid = auth.currentUser?.uid
                    ?: return@withContext Result.failure(IllegalStateException(ERROR_NOT_AUTHENTICATED))

                val document = sighting.copy(
                    creatorUid = creatorUid,
                    targetCity = "Bucaramanga"
                )
                val ref = firestore.collection(COLLECTION_SIGHTINGS)
                    .add(document)
                    .await()
                Result.success(ref.id)
            } catch (e: FirebaseFirestoreException) {
                Result.failure(IllegalStateException(e.message ?: ERROR_FIRESTORE))
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: ERROR_UNKNOWN))
            }
        }

    companion object {
        const val TAG = "FirestoreRepo"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_SIGHTINGS = "sightings"
        const val SUBCOLLECTION_SCANS = "scan_history"
        const val FIELD_TARGET_CITY = "targetCity"
        const val FIELD_CREATED_AT = "createdAt"

        const val ERROR_FIRESTORE = "FIRESTORE_ERROR"
        const val ERROR_UNKNOWN = "UNKNOWN_ERROR"
        const val ERROR_NOT_AUTHENTICATED = "NOT_AUTHENTICATED"
    }
}
