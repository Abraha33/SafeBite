package unab.edu.co.abrahamcaceres.safebite

import android.app.Application
import androidx.room.Room
import unab.edu.co.abrahamcaceres.safebite.data.local.SafeBiteDatabase

/**
 * Punto de entrada de la capa de datos: expone la base Room como singleton en memoria.
 */
class SafeBiteApplication : Application() {

    /** Instancia única de la base de datos Room (SQLite local). */
    val database: SafeBiteDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SafeBiteDatabase::class.java,
            SafeBiteDatabase.NAME
        ).fallbackToDestructiveMigration()
            .build()
    }
}
