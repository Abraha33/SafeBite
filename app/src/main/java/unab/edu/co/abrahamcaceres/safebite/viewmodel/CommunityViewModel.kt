package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.SightingRepository
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.model.Sighting

class CommunityViewModel(
    application: Application,
    private val repository: SightingRepository
) : AndroidViewModel(application) {

    private val _selectedCity = MutableLiveData<String?>(null)

    val sightings: LiveData<List<Sighting>> = _selectedCity.switchMap { city ->
        if (city.isNullOrBlank()) repository.observeAll()
        else repository.observeByCity(city)
    }

    val selectedCity: LiveData<String?> = _selectedCity

    fun filterByCity(city: String?) {
        _selectedCity.value = city
    }

    fun publishSighting(sighting: Sighting) {
        if (sighting.getTitle().isBlank()) return
        viewModelScope.launch {
            repository.insert(sighting)
        }
    }

    fun seedIfEmpty() {
        viewModelScope.launch {
            val count = repository.count()
            if (count == 0) {
                seedSampleData()
            }
        }
    }

    private suspend fun seedSampleData() {
        val ctx = getApplication<Application>()
        val samples = listOf(
            Sighting.crear(
                creatorName = "Chef María",
                timeAgo = "16 hours ago",
                title = "Panqueques sin Gluten con Almendras",
                allergenStatusText = "✓ 100% Seguro para tu Perfil",
                allergenRiskLevel = ScanRisk.SAFE,
                description = "Encontré harina de almendras en Store v. Bucaramanga. Precio especial por mayoreo. ¡Perfecta para repostería libre de gluten!",
                locationName = "Store v. Bucaramanga"
            ),
            Sighting.crear(
                creatorName = "Carlos Ruiz",
                timeAgo = "2 days ago",
                title = "Leche de Soya sin Lactosa",
                allergenStatusText = "⚠️ Alerta de Alérgeno: Contiene Soja",
                allergenRiskLevel = ScanRisk.DANGER,
                description = "Nueva marca 'NutriVeg' en Supermercado Los Andes. Leche vegetal fortificada con calcio, ideal para intolerantes a lactosa pero contiene soya.",
                locationName = "Supermercado Los Andes v. Piedecuesta"
            ),
            Sighting.crear(
                creatorName = "Laura Medina",
                timeAgo = "3 days ago",
                title = "Galletas de Avena sin Azúcar",
                allergenStatusText = "⚠️ Trazas de Gluten Detectadas",
                allergenRiskLevel = ScanRisk.WARNING,
                description = "Encontré estas galletas en la sección saludable del Éxito. Tienen etiqueta 'libre de azúcar' pero advierten trazas de trigo.",
                locationName = "Éxito v. Bucaramanga"
            ),
            Sighting.crear(
                creatorName = "Pedro Sánchez",
                timeAgo = "5 days ago",
                title = "Queso Vegano de Castañas",
                allergenStatusText = "✓ 100% Seguro para tu Perfil",
                allergenRiskLevel = ScanRisk.SAFE,
                description = "Producto artesanal en la feria orgánica de Floridablanca. Libre de lácteos, gluten y soya. Altamente recomendado.",
                locationName = "Feria Orgánica v. Floridablanca"
            ),
            Sighting.crear(
                creatorName = "Ana López",
                timeAgo = "1 week ago",
                title = "Barra Energética con Maní",
                allergenStatusText = "⚠️ Alerta de Alérgeno: Contiene Maní",
                allergenRiskLevel = ScanRisk.DANGER,
                description = "Cuidado con 'EnergyGo' en el gimnasio SportLife. Tiene maní como segundo ingrediente. No apta para alérgicos.",
                locationName = "SportLife v. Girón"
            ),
            Sighting.crear(
                creatorName = "Sofía Torres",
                timeAgo = "1 week ago",
                title = "Pan Integral Sin TACC",
                allergenStatusText = "✓ 100% Seguro para tu Perfil",
                allergenRiskLevel = ScanRisk.SAFE,
                description = "Panificadora 'Sin Límites' ahora distribuye en tiendas Nature's. Certificado libre de gluten, apto celíacos.",
                locationName = "Nature's v. Bucaramanga"
            )
        )
        samples.forEach { repository.insert(it) }
    }
}
