package unab.edu.co.abrahamcaceres.safebite.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import unab.edu.co.abrahamcaceres.safebite.model.Allergen

/**
 * Acceso a datos para la lista negra de alérgenos.
 */
@Dao
interface AllergenDao {

    @Query("SELECT * FROM allergens ORDER BY display_name COLLATE NOCASE ASC")
    fun observeAllergens(): LiveData<List<Allergen>>

    @Query("SELECT COUNT(*) FROM allergens WHERE normalized_name = :normalized LIMIT 1")
    suspend fun countByNormalizedName(normalized: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(allergen: Allergen): Long

    @Delete
    suspend fun delete(allergen: Allergen)
}
