package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import unab.edu.co.abrahamcaceres.safebite.SafeBiteApplication
import unab.edu.co.abrahamcaceres.safebite.data.repository.AllergenRepository

/**
 * Fábrica para [AllergenViewModel] con inyección del repositorio Room.
 */
class AllergenViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AllergenViewModel::class.java)) {
            val dao = (application as SafeBiteApplication).database.allergenDao()
            val repo = AllergenRepository(dao)
            return AllergenViewModel(application, repo) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
