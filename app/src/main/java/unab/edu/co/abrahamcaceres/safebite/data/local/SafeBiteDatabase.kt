package unab.edu.co.abrahamcaceres.safebite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import unab.edu.co.abrahamcaceres.safebite.model.Allergen
import unab.edu.co.abrahamcaceres.safebite.model.ProductScan
import unab.edu.co.abrahamcaceres.safebite.model.Sighting
import unab.edu.co.abrahamcaceres.safebite.model.User

/**
 * Base de datos local Room (persistencia del proyecto).
 */
@Database(
    entities = [User::class, Allergen::class, ProductScan::class, Sighting::class],
    version = 8,
    exportSchema = false
)
abstract class SafeBiteDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun allergenDao(): AllergenDao

    abstract fun productScanDao(): ProductScanDao

    abstract fun sightingDao(): SightingDao

    companion object {
        const val NAME = "safebite.db"
    }
}
