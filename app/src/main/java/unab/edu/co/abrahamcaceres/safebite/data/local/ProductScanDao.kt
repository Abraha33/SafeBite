package unab.edu.co.abrahamcaceres.safebite.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan

/**
 * Acceso a datos del historial de escaneos.
 */
@Dao
interface ProductScanDao {

    @Query("SELECT * FROM product_scans ORDER BY created_at_ms DESC")
    fun observeScans(): LiveData<List<ProductScan>>

    @Query("SELECT * FROM product_scans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductScan?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(scan: ProductScan): Long

    @Query("DELETE FROM product_scans")
    suspend fun deleteAll(): Int
}
