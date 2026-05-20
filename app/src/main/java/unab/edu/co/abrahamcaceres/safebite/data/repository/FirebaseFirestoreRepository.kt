package unab.edu.co.abrahamcaceres.safebite.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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

    suspend fun fetchSightings(city: String = ""): Result<List<SightingCloudModel>> =
        withContext(Dispatchers.IO) {
            try {
                var query = firestore.collection(COLLECTION_SIGHTINGS)
                    .orderBy(FIELD_CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
                if (city.isNotBlank()) {
                    query = query.whereEqualTo(FIELD_TARGET_CITY, city)
                }
                val snapshot = query.get().await()
                val sightings = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SightingCloudModel::class.java)?.copy(sightingId = doc.id)
                }
                Result.success(sightings)
            } catch (e: FirebaseFirestoreException) {
                Result.failure(IllegalStateException(e.message ?: ERROR_FIRESTORE))
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: ERROR_UNKNOWN))
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
