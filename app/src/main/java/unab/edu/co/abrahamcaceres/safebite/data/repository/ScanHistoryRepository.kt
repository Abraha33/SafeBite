package unab.edu.co.abrahamcaceres.safebite.data.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.data.local.ProductScanDao
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan

/**
 * Repositorio del historial de escaneos persistido en Room.
 */
class ScanHistoryRepository(private val productScanDao: ProductScanDao) {

    fun observeScans(): LiveData<List<ProductScan>> = productScanDao.observeScans()

    suspend fun insert(scan: ProductScan): Result<Long> = withContext(Dispatchers.IO) {
        runCatching { productScanDao.insert(scan) }
    }

    suspend fun getById(id: Long): ProductScan? = withContext(Dispatchers.IO) {
        productScanDao.getById(id)
    }
}
