package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import unab.edu.co.abrahamcaceres.safebite.SafeBiteApplication
import unab.edu.co.abrahamcaceres.safebite.data.repository.ScanHistoryRepository

/**
 * Fábrica para [ScanHistoryViewModel].
 */
class ScanHistoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanHistoryViewModel::class.java)) {
            val dao = (application as SafeBiteApplication).database.productScanDao()
            val repo = ScanHistoryRepository(dao)
            return ScanHistoryViewModel(application, repo) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
