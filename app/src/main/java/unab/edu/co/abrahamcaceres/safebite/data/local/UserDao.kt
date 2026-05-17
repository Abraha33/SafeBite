package unab.edu.co.abrahamcaceres.safebite.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import unab.edu.co.abrahamcaceres.safebite.model.User

/**
 * Contrato de acceso a datos para la tabla de usuarios.
 */
@Dao
interface UserDao {

    /** Inserta un usuario recién registrado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    /** Busca un usuario por correo (debe coincidir con el valor normalizado guardado). */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
