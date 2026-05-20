package unab.edu.co.abrahamcaceres.safebite.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import unab.edu.co.abrahamcaceres.safebite.model.Sighting

@Dao
interface SightingDao {

    @Query("SELECT * FROM sightings_table ORDER BY id DESC")
    fun observeAll(): LiveData<List<Sighting>>

    @Query("SELECT * FROM sightings_table WHERE target_city = :city ORDER BY id DESC")
    fun observeByCity(city: String): LiveData<List<Sighting>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sighting: Sighting): Long

    @Query("SELECT COUNT(*) FROM sightings_table")
    suspend fun count(): Int

    @Query("DELETE FROM sightings_table")
    suspend fun deleteAll(): Int
}
