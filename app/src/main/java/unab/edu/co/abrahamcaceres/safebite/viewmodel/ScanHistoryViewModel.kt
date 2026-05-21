package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirestoreDataRepository
import unab.edu.co.abrahamcaceres.safebite.data.repository.ScanHistoryRepository
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.model.ScanRisk
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ScanHistoryModel

class ScanHistoryViewModel(
    application: Application,
    private val repository: ScanHistoryRepository,
    private val firestoreDataRepo: FirestoreDataRepository
) : AndroidViewModel(application) {

    val scans: LiveData<List<ProductScan>> = repository.observeScans()

    private val _scansFromFirestore = MutableLiveData<List<ScanHistoryModel>>(emptyList())
    val scansFromFirestore: LiveData<List<ScanHistoryModel>> = _scansFromFirestore

    private val _firestoreEmpty = MutableLiveData(true)
    val firestoreEmpty: LiveData<Boolean> = _firestoreEmpty

    fun observeFirestoreScanHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            firestoreDataRepo.observeScanHistory(uid).collect { list ->
                _scansFromFirestore.postValue(list)
                _firestoreEmpty.postValue(list.isEmpty())
            }
        }
    }

    fun insertScan(scan: ProductScan, onInserted: ((Long) -> Unit)? = null) {
        if (scan.getDetectedText().isBlank()) return
        viewModelScope.launch {
            val result = repository.insert(scan)
            result.onSuccess { id -> onInserted?.invoke(id) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

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
