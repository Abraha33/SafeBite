package unab.edu.co.abrahamcaceres.safebite.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.model.cloud.SightingCloudModel
import unab.edu.co.abrahamcaceres.safebite.model.cloud.UserCloudModel
import unab.edu.co.abrahamcaceres.safebite.model.cloud.toMap

class FirebaseFirestoreRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun saveUserProfile(profile: UserCloudModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val document = profile.copy(uid = profile.uid.ifBlank { auth.currentUser?.uid ?: "" })
                firestore.collection(COLLECTION_USERS)
                    .document(document.uid)
                    .set(document.toMap())
                    .await()
                Result.success(Unit)
            } catch (e: FirebaseFirestoreException) {
                Result.failure(IllegalStateException(e.message ?: ERROR_FIRESTORE))
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: ERROR_UNKNOWN))
            }
        }

    suspend fun uploadCommunitySighting(sighting: SightingCloudModel): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val creatorUid = auth.currentUser?.uid
                    ?: return@withContext Result.failure(IllegalStateException(ERROR_NOT_AUTHENTICATED))

                val document = sighting.copy(
                    creatorUid = creatorUid,
                    targetCity = "Bucaramanga"
                )
                val ref = firestore.collection(COLLECTION_SIGHTINGS)
                    .add(document.toMap())
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

        const val ERROR_FIRESTORE = "FIRESTORE_ERROR"
        const val ERROR_UNKNOWN = "UNKNOWN_ERROR"
        const val ERROR_NOT_AUTHENTICATED = "NOT_AUTHENTICATED"
    }
}
