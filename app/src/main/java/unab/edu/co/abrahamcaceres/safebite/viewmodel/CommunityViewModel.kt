package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirebaseFirestoreRepository
import unab.edu.co.abrahamcaceres.safebite.model.Sighting
import unab.edu.co.abrahamcaceres.safebite.model.cloud.SightingCloudModel
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class CommunityViewModel(
    application: Application,
    private val firestoreRepo: FirebaseFirestoreRepository
) : AndroidViewModel(application) {

    private val _sightings = MutableLiveData<List<Sighting>>(emptyList())
    val sightings: LiveData<List<Sighting>> = _sightings

    private val _selectedCity = MutableLiveData("")
    val selectedCity: LiveData<String> = _selectedCity

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isPublishing = MutableLiveData(false)
    val isPublishing: LiveData<Boolean> = _isPublishing

    private val _publishResult = MutableLiveData<Boolean?>(null)
    val publishResult: LiveData<Boolean?> = _publishResult

    private var snapshotRegistration: ListenerRegistration? = null

    init {
        if (FirebaseAuth.getInstance().currentUser != null) {
            listenToSightings("")
        }
    }

    override fun onCleared() {
        super.onCleared()
        snapshotRegistration?.remove()
    }

    fun filterByCity(city: String) {
        _selectedCity.value = city
        snapshotRegistration?.remove()
        if (FirebaseAuth.getInstance().currentUser != null) {
            listenToSightings(city)
        }
    }

    fun publishSighting(sighting: SightingCloudModel) {
        if (_isPublishing.value == true) return
        viewModelScope.launch {
            _isPublishing.value = true
            firestoreRepo.postSighting(sighting).fold(
                onSuccess = { _publishResult.value = true },
                onFailure = { _publishResult.value = false }
            )
            _isPublishing.value = false
        }
    }

    fun consumePublishResult() {
        _publishResult.value = null
    }

    private fun listenToSightings(city: String) {
        _isLoading.value = true
        snapshotRegistration = firestoreRepo.listenToSightings(city) { cloudList, error ->
            if (error != null) {
                Log.w(TAG, "Firestore snapshot error: ${error.message}")
                _sightings.value = emptyList()
                _isLoading.value = false
                return@listenToSightings
            }
            try {
                _sightings.value = cloudList?.map { it.toSighting() } ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping sightings: ${e.message}")
                _sightings.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    private companion object {
        const val TAG = "CommunityVM"

        fun SightingCloudModel.toSighting(): Sighting {
            val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            fmt.maximumFractionDigits = 0
            val millis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
            return Sighting.crear(
                creatorName = creatorName.ifBlank { "Anónimo" },
                timeAgo = formatRelativeTime(millis),
                productName = productName.ifBlank { "Producto" },
                storeName = storeName.ifBlank { "Tienda" },
                price = if (price > 0) fmt.format(price) else "$0",
                communityTip = communityTip,
                targetCity = targetCity,
                allergenTag = allergenTag,
                latitude = latitude,
                longitude = longitude
            )
        }

        fun formatRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                minutes < 1 -> "Justo ahora"
                minutes < 60 -> "Hace $minutes min"
                hours < 24 -> "Hace $hours h"
                days == 1L -> "Hace 1 día"
                days < 7 -> "Hace $days días"
                days < 30 -> "Hace ${days / 7} sem"
                else -> "Hace ${days / 30} meses"
            }
        }
    }
}
