package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import unab.edu.co.abrahamcaceres.safebite.SafeBiteApplication
import unab.edu.co.abrahamcaceres.safebite.data.repository.ScanHistoryRepository
import unab.edu.co.abrahamcaceres.safebite.data.repository.UserRepository

/**
 * FÃ¡brica de [ProfileViewModel] con dependencias de Room y sesiÃ³n local.
 */
class ProfileViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val db = (application as SafeBiteApplication).database
            val userRepository = UserRepository(db.userDao())
            val scanHistoryRepository = ScanHistoryRepository(db.productScanDao())
            return ProfileViewModel(application, userRepository, scanHistoryRepository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
