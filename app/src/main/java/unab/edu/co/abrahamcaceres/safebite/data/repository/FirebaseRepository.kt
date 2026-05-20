package unab.edu.co.abrahamcaceres.safebite.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.model.Sighting
import java.util.Locale

class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    suspend fun registerUserInCloud(
        email: String,
        password: String,
        name: String,
        city: String,
        allergens: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw IllegalStateException(ERROR_AUTH_FAILED)

            val profile = hashMapOf(
                FIELD_NAME to name.trim(),
                FIELD_EMAIL to email.trim().lowercase(Locale.getDefault()),
                FIELD_CITY to city.trim(),
                FIELD_ALLERGENS to allergens,
                FIELD_CREATED_AT to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS).document(uid).set(profile).await()
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(mapAuthError(e))
        }
    }

    suspend fun loginUserInCloud(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw IllegalStateException(ERROR_AUTH_FAILED)
                Result.success(uid)
            } catch (e: Exception) {
                Result.failure(mapAuthError(e))
            }
        }

    suspend fun getCurrentUid(): String? = withContext(Dispatchers.IO) {
        auth.currentUser?.uid
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        auth.signOut()
    }

    suspend fun uploadProductSighting(
        sighting: Sighting,
        imageUri: Uri?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var imageUrl = ""
            if (imageUri != null) {
                val fileName = "sightings/${System.currentTimeMillis()}_${imageUri.lastPathSegment}"
                val uploadTask = storage.reference.child(fileName).putFile(imageUri).await()
                imageUrl = uploadTask.storage.downloadUrl.await().toString()
            }

            val docData = hashMapOf(
                FIELD_CREATOR_NAME to sighting.getCreatorName(),
                FIELD_TIME_AGO to sighting.getTimeAgo(),
                FIELD_PRODUCT_NAME to sighting.getProductName(),
                FIELD_STORE_NAME to sighting.getStoreName(),
                FIELD_PRICE to sighting.getPrice(),
                FIELD_COMMUNITY_TIP to sighting.getCommunityTip(),
                FIELD_TARGET_CITY to sighting.getTargetCity(),
                FIELD_ALLERGEN_TAG to sighting.getAllergenTag(),
                FIELD_LATITUDE to sighting.getLatitude(),
                FIELD_LONGITUDE to sighting.getLongitude(),
                FIELD_IMAGE_URL to imageUrl,
                FIELD_CREATED_AT to System.currentTimeMillis()
            )
            val ref = firestore.collection(COLLECTION_SIGHTINGS).add(docData).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun observeSightingsByCity(city: String): List<Map<String, Any>> =
        withContext(Dispatchers.IO) {
            val snapshot = firestore.collection(COLLECTION_SIGHTINGS)
                .whereEqualTo(FIELD_TARGET_CITY, city)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.map { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data[FIELD_DOC_ID] = doc.id
                data
            }
        }

    suspend fun observeAllSightings(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection(COLLECTION_SIGHTINGS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .get()
            .await()
        snapshot.documents.map { doc ->
            val data = doc.data?.toMutableMap() ?: mutableMapOf()
            data[FIELD_DOC_ID] = doc.id
            data
        }
    }

    private fun mapAuthError(error: Throwable): Throwable = when (error) {
        is FirebaseAuthWeakPasswordException -> IllegalStateException(CODE_WEAK_PASSWORD)
        is FirebaseAuthInvalidCredentialsException -> IllegalStateException(CODE_INVALID_CREDENTIALS)
        is FirebaseAuthUserCollisionException -> IllegalStateException(CODE_EMAIL_EXISTS)
        else -> error
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_SIGHTINGS = "sightings"

        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_CITY = "city"
        const val FIELD_ALLERGENS = "allergens"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_DOC_ID = "docId"
        const val FIELD_CREATOR_NAME = "creatorName"
        const val FIELD_TIME_AGO = "timeAgo"
        const val FIELD_PRODUCT_NAME = "productName"
        const val FIELD_STORE_NAME = "storeName"
        const val FIELD_PRICE = "price"
        const val FIELD_COMMUNITY_TIP = "communityTip"
        const val FIELD_TARGET_CITY = "targetCity"
        const val FIELD_ALLERGEN_TAG = "allergenTag"
        const val FIELD_LATITUDE = "latitude"
        const val FIELD_LONGITUDE = "longitude"
        const val FIELD_IMAGE_URL = "imageUrl"

        const val CODE_WEAK_PASSWORD = "WEAK_PASSWORD"
        const val CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
        const val CODE_EMAIL_EXISTS = "EMAIL_EXISTS"
        const val ERROR_AUTH_FAILED = "AUTH_FAILED"
    }
}
