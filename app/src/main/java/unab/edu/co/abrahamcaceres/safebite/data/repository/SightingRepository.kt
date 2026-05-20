package unab.edu.co.abrahamcaceres.safebite.data.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.data.local.SightingDao
import unab.edu.co.abrahamcaceres.safebite.model.Sighting

class SightingRepository(private val sightingDao: SightingDao) {

    fun observeAll(): LiveData<List<Sighting>> = sightingDao.observeAll()

    fun observeByCity(city: String): LiveData<List<Sighting>> = sightingDao.observeByCity(city)

    suspend fun insert(sighting: Sighting): Result<Long> = withContext(Dispatchers.IO) {
        runCatching { sightingDao.insert(sighting) }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        sightingDao.count()
    }

    suspend fun deleteAll(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching { sightingDao.deleteAll() }
    }
}
