package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.ScanHistoryRepository
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk

/**
 * MVVM: historial reactivo de escaneos y altas en Room.
 */
class ScanHistoryViewModel(
    application: Application,
    private val repository: ScanHistoryRepository
) : AndroidViewModel(application) {

    val scans: LiveData<List<ProductScan>> = repository.observeScans()

    /** Inserta un escaneo (p. ej. tras OCR en el escáner). */
    fun insertScan(detectedText: String, riskLevel: String = ScanRisk.SAFE) {
        if (detectedText.isBlank()) return
        viewModelScope.launch {
            repository.insert(ProductScan.crear(detectedText, riskLevel))
        }
    }

    /**
     * Datos de demostración para probar el RecyclerView antes de integrar ML Kit.
     */
    fun insertSampleScan() {
        val sample = getApplication<Application>().getString(R.string.history_sample_text)
        viewModelScope.launch {
            repository.insert(
                ProductScan.crear(
                    textoDetectado = sample,
                    nivelRiesgo = ScanRisk.WARNING
                )
            )
        }
    }
}
