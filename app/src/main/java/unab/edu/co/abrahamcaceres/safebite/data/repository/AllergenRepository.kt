package unab.edu.co.abrahamcaceres.safebite.data.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.data.local.AllergenDao
import unab.edu.co.abrahamcaceres.safebite.model.Allergen

/**
 * Repositorio de alérgenos: lectura reactiva e inserción con control de duplicados.
 */
class AllergenRepository(private val allergenDao: AllergenDao) {

    fun observeAllergens(): LiveData<List<Allergen>> = allergenDao.observeAllergens()

    suspend fun insert(allergen: Allergen): Result<Unit> = withContext(Dispatchers.IO) {
        val existe = allergenDao.countByNormalizedName(allergen.getNormalizedName()) > 0
        if (existe) {
            return@withContext Result.failure(IllegalStateException(CODE_DUPLICATE))
        }
        runCatching {
            allergenDao.insert(allergen)
        }.map { }
    }

    suspend fun delete(allergen: Allergen) = withContext(Dispatchers.IO) {
        allergenDao.delete(allergen)
    }

    companion object {
        const val CODE_DUPLICATE = "ALLERGEN_DUPLICATE"
    }
}
