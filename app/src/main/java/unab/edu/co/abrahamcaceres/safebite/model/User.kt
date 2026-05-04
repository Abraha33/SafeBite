package unab.edu.co.abrahamcaceres.safebite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import unab.edu.co.abrahamcaceres.safebite.utils.PasswordHasher
import java.util.Locale

/**
 * Modelo de dominio persistido con Room.
 * POO: atributos privados y métodos de acceso explícitos (requisito académico).
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private var id: Long = 0L,
    @ColumnInfo(name = "full_name")
    private var fullName: String = "",
    @ColumnInfo(name = "email")
    private var email: String = "",
    @ColumnInfo(name = "password_hash")
    private var passwordHash: String = ""
) {

    /** Identificador único del usuario en SQLite. */
    fun getId(): Long = id

    /** Nombre completo registrado. */
    fun getFullName(): String = fullName

    /** Correo en minúsculas (clave de búsqueda para login). */
    fun getEmail(): String = email

    /** Hash almacenado (nunca la contraseña en claro). */
    fun getPasswordHash(): String = passwordHash

    companion object {
        /**
         * Fábrica para crear un usuario nuevo a partir del formulario de registro.
         * Normaliza correo y calcula el hash de la contraseña.
         */
        fun desdeRegistro(
            nombreCompleto: String,
            correo: String,
            contrasenaPlano: String
        ): User {
            val emailNormalizado = correo.trim().lowercase(Locale.getDefault())
            return User(
                id = 0L,
                fullName = nombreCompleto.trim(),
                email = emailNormalizado,
                passwordHash = PasswordHasher.hash(contrasenaPlano)
            )
        }
    }
}
