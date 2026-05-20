package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.data.repository.SightingRepository
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
        if (sighting.getProductName().isBlank()) return
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
        val samples = listOf(
            Sighting.crear(
                productName = "Harina de Almendras",
                storeName = "Store v. Bucaramanga",
                price = "$12.900 COP",
                communityTip = "Precio especial por mayoreo. Perfecta para repostería libre de gluten.",
                city = "Bucaramanga",
                allergenTag = "✓ 100% Seguro",
                latitude = 7.1254,
                longitude = -73.1198
            ),
            Sighting.crear(
                productName = "Leche de Soya NutriVeg",
                storeName = "Supermercado Los Andes",
                price = "$8.500 COP",
                communityTip = "Nueva marca, leche vegetal fortificada con calcio. Ideal para intolerantes a lactosa pero contiene soya.",
                city = "Piedecuesta",
                allergenTag = "Contiene Soja",
                latitude = 6.9896,
                longitude = -73.0536
            ),
            Sighting.crear(
                productName = "Galletas de Avena sin Azúcar",
                storeName = "Éxito v. Bucaramanga",
                price = "$6.200 COP",
                communityTip = "Sección saludable. Etiqueta 'libre de azúcar' pero advierten trazas de trigo.",
                city = "Bucaramanga",
                allergenTag = "Trazas de Gluten",
                latitude = 7.1186,
                longitude = -73.1161
            ),
            Sighting.crear(
                productName = "Queso Vegano de Castañas",
                storeName = "Feria Orgánica Floridablanca",
                price = "$15.000 COP",
                communityTip = "Producto artesanal. Libre de lácteos, gluten y soya. Altamente recomendado.",
                city = "Floridablanca",
                allergenTag = "✓ 100% Seguro",
                latitude = 7.0648,
                longitude = -73.0894
            ),
            Sighting.crear(
                productName = "Barra Energética EnergyGo",
                storeName = "SportLife v. Girón",
                price = "$4.500 COP",
                communityTip = "Tiene maní como segundo ingrediente. No apta para alérgicos.",
                city = "Girón",
                allergenTag = "Contiene Maní",
                latitude = 7.0682,
                longitude = -73.1697
            ),
            Sighting.crear(
                productName = "Pan Integral Sin TACC",
                storeName = "Nature's v. Bucaramanga",
                price = "$10.200 COP",
                communityTip = "Panificadora 'Sin Límites'. Certificado libre de gluten, apto celíacos.",
                city = "Bucaramanga",
                allergenTag = "✓ 100% Seguro",
                latitude = 7.1295,
                longitude = -73.1227
            )
        )
        samples.forEach { repository.insert(it) }
    }
}
