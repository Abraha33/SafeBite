package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

    init {
        fetchSightings("")
    }

    fun filterByCity(city: String) {
        _selectedCity.value = city
        fetchSightings(city)
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

    private fun fetchSightings(city: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firestoreRepo.fetchSightings(city = city)
            result.fold(
                onSuccess = { cloudList ->
                    _sightings.value = cloudList.map { it.toSighting() }
                },
                onFailure = { _sightings.value = emptyList() }
            )
            _isLoading.value = false
        }
    }

    private companion object {
        fun SightingCloudModel.toSighting(): Sighting {
            val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            fmt.maximumFractionDigits = 0
            return Sighting.crear(
                creatorName = creatorName.ifBlank { "Anónimo" },
                timeAgo = formatRelativeTime(createdAt),
                productName = productName,
                storeName = storeName,
                price = fmt.format(price),
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
