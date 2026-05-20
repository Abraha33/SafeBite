package unab.edu.co.abrahamcaceres.safebite.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class FirebaseFirestoreRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun saveUserProfile(
        uid: String,
        name: String,
        email: String,
        city: String,
        allergens: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = hashMapOf(
                FIELD_NAME to name.trim(),
                FIELD_EMAIL to email.trim().lowercase(Locale.getDefault()),
                FIELD_CITY to city.trim(),
                FIELD_ALLERGENS to allergens,
                FIELD_CREATED_AT to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .set(profile)
                .await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(IllegalStateException(e.message ?: ERROR_FIRESTORE))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: ERROR_UNKNOWN))
        }
    }

    suspend fun uploadCommunitySighting(
        productName: String,
        storeName: String,
        price: Double,
        tip: String,
        allergenTag: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val creatorUid = auth.currentUser?.uid
                ?: return@withContext Result.failure(IllegalStateException(ERROR_NOT_AUTHENTICATED))

            val sighting = hashMapOf(
                FIELD_CREATOR_UID to creatorUid,
                FIELD_PRODUCT_NAME to productName.trim(),
                FIELD_STORE_NAME to storeName.trim(),
                FIELD_PRICE to price,
                FIELD_COMMUNITY_TIP to tip.trim(),
                FIELD_TARGET_CITY to "Bucaramanga",
                FIELD_ALLERGEN_TAG to allergenTag.trim(),
                FIELD_CREATED_AT to FieldValue.serverTimestamp()
            )
            val ref = firestore.collection(COLLECTION_SIGHTINGS)
                .add(sighting)
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

        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_CITY = "city"
        const val FIELD_ALLERGENS = "allergens"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_CREATOR_UID = "creatorUid"
        const val FIELD_PRODUCT_NAME = "productName"
        const val FIELD_STORE_NAME = "storeName"
        const val FIELD_PRICE = "price"
        const val FIELD_COMMUNITY_TIP = "communityTip"
        const val FIELD_TARGET_CITY = "targetCity"
        const val FIELD_ALLERGEN_TAG = "allergenTag"

        const val ERROR_FIRESTORE = "FIRESTORE_ERROR"
        const val ERROR_UNKNOWN = "UNKNOWN_ERROR"
        const val ERROR_NOT_AUTHENTICATED = "NOT_AUTHENTICATED"
    }
}
