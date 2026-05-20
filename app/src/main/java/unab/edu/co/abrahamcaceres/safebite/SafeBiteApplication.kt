package unab.edu.co.abrahamcaceres.safebite

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import unab.edu.co.abrahamcaceres.safebite.data.local.SafeBiteDatabase

/**
 * Punto de entrada de la capa de datos: expone la base Room como singleton en memoria
 * e inicializa Firebase para autenticación y almacenamiento en la nube.
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

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
