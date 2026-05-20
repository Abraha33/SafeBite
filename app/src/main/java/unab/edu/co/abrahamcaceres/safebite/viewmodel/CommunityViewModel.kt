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

    private val _selectedCity = MutableLiveData<String>("")

    val sightings: LiveData<List<Sighting>> = _selectedCity.switchMap { city ->
        if (city.isBlank()) repository.observeAll()
        else repository.observeByCity(city)
    }

    val selectedCity: LiveData<String> = _selectedCity

    fun filterByCity(city: String) {
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
                creatorName = "Chef María",
                timeAgo = "Hace 16 horas",
                productName = "Harina de Almendras",
                storeName = "Store v. Bucaramanga",
                communityTip = "Precio especial por mayoreo. Perfecta para repostería libre de gluten.",
                targetCity = "Bucaramanga",
                allergenTag = "Gluten-Free"
            ),
            Sighting.crear(
                creatorName = "Carlos Ruiz",
                timeAgo = "Hace 2 días",
                productName = "Leche de Soya NutriVeg",
                storeName = "Supermercado Los Andes",
                communityTip = "Leche vegetal fortificada con calcio. Ideal para intolerantes a lactosa pero contiene soya.",
                targetCity = "Piedecuesta",
                allergenTag = "Contiene Soja"
            ),
            Sighting.crear(
                creatorName = "Laura Medina",
                timeAgo = "Hace 3 días",
                productName = "Galletas de Avena sin Azúcar",
                storeName = "Éxito v. Bucaramanga",
                communityTip = "Sección saludable. Etiqueta sin azúcar pero advierten trazas de trigo.",
                targetCity = "Bucaramanga",
                allergenTag = "Trazas de Gluten"
            ),
            Sighting.crear(
                creatorName = "Pedro Sánchez",
                timeAgo = "Hace 5 días",
                productName = "Queso Vegano de Castañas",
                storeName = "Feria Orgánica Floridablanca",
                communityTip = "Producto artesanal. Libre de lácteos, gluten y soya.",
                targetCity = "Floridablanca",
                allergenTag = "Lactose-Free"
            ),
            Sighting.crear(
                creatorName = "Ana López",
                timeAgo = "Hace 1 semana",
                productName = "Barra Energética EnergyGo",
                storeName = "SportLife v. Girón",
                communityTip = "Tiene maní como segundo ingrediente. No apta para alérgicos.",
                targetCity = "Girón",
                allergenTag = "Contiene Maní"
            ),
            Sighting.crear(
                creatorName = "Sofía Torres",
                timeAgo = "Hace 1 semana",
                productName = "Pan Integral Sin TACC",
                storeName = "Nature's v. Bucaramanga",
                communityTip = "Certificado libre de gluten, apto celíacos.",
                targetCity = "Bucaramanga",
                allergenTag = "Gluten-Free"
            )
        )
        samples.forEach { repository.insert(it) }
    }
}
