package unab.edu.co.abrahamcaceres.safebite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import unab.edu.co.abrahamcaceres.safebite.model.User

/**
 * Base de datos local Room (persistencia del proyecto).
 */
@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
abstract class SafeBiteDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "safebite.db"
    }
}
