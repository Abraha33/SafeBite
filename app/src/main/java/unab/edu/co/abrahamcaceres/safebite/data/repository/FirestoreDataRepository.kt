package unab.edu.co.abrahamcaceres.safebite.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ProductModel
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ScanHistoryModel
import unab.edu.co.abrahamcaceres.safebite.model.cloud.UserCloudModel

class FirestoreDataRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun updateUserProfile(user: UserCloudModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val uid = auth.currentUser?.uid
                    ?: return@withContext Result.failure(IllegalStateException(NOT_AUTHENTICATED))
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .set(user, SetOptions.merge())
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: UNKNOWN_ERROR))
            }
        }

    fun getBlacklist(uid: String): Flow<List<ProductModel>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(SUBCOLLECTION_BLACKLIST)
            .orderBy(FIELD_ADDED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ProductModel(
                        id = doc.id,
                        productName = (data["productName"] as? String) ?: "",
                        detectedText = (data["detectedText"] as? String) ?: "",
                        riskLevel = (data["riskLevel"] as? String) ?: "SAFE",
                        matchedAllergens = (data["matchedAllergens"] as? List<String>) ?: emptyList(),
                        addedAt = (data["addedAt"] as? Long) ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addToBlacklist(uid: String, product: ProductModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(SUBCOLLECTION_BLACKLIST)
                    .add(product)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: UNKNOWN_ERROR))
            }
        }

    suspend fun removeFromBlacklist(uid: String, productId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firestore.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(SUBCOLLECTION_BLACKLIST)
                    .document(productId)
                    .delete()
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: UNKNOWN_ERROR))
            }
        }

    fun observeScanHistory(uid: String): Flow<List<ScanHistoryModel>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(SUBCOLLECTION_SCANS)
            .orderBy(FIELD_SCAN_DATE, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ScanHistoryModel(
                        scanId = doc.id,
                        scanDate = (data["scanDate"] as? Long) ?: System.currentTimeMillis(),
                        productName = (data["productName"] as? String) ?: "",
                        rawTextDetected = (data["rawTextDetected"] as? String) ?: "",
                        status = (data["status"] as? String) ?: "",
                        matchedAllergens = (data["matchedAllergens"] as? List<String>) ?: emptyList()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_BLACKLIST = "blacklist"
        const val SUBCOLLECTION_SCANS = "scan_history"
        const val FIELD_ADDED_AT = "addedAt"
        const val FIELD_SCAN_DATE = "scanDate"
        const val NOT_AUTHENTICATED = "NOT_AUTHENTICATED"
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    }
}
