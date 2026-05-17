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

    /** Inserta un escaneo ya construido desde la capa de presentaciÃ³n. */
    fun insertScan(scan: ProductScan) {
        if (scan.getDetectedText().isBlank()) return
        viewModelScope.launch {
            repository.insert(scan)
        }
    }

    /** Inserta un escaneo (p. ej. tras OCR en el escÃ¡ner). */
    fun insertScan(detectedText: String, riskLevel: String = ScanRisk.SAFE) {
        if (detectedText.isBlank()) return
        insertScan(ProductScan.crear(detectedText, riskLevel))
    }

    /** Elimina todo el historial de escaneos de Room. */
    fun clearHistory() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    /**
     * Datos de demostraciÃ³n para probar el RecyclerView antes de integrar ML Kit.
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
