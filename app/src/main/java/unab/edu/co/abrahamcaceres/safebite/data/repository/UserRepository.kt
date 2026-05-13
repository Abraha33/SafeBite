package unab.edu.co.abrahamcaceres.safebite.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import unab.edu.co.abrahamcaceres.safebite.data.local.UserDao
import unab.edu.co.abrahamcaceres.safebite.model.User
import unab.edu.co.abrahamcaceres.safebite.utils.PasswordHasher
import java.util.Locale

/**
 * Capa de repositorio: centraliza las operaciones de autenticaciÃ³n sobre Room.
 */
class UserRepository(private val userDao: UserDao) {

    /** Registra un usuario si el correo no existe aÃºn. */
    suspend fun register(user: User): Result<Long> = withContext(Dispatchers.IO) {
        val existente = userDao.getUserByEmail(user.getEmail())
        if (existente != null) {
            return@withContext Result.failure(IllegalStateException(CODE_EMAIL_EXISTS))
        }
        runCatching { userDao.insertUser(user) }
    }

    /** Valida credenciales contra los datos locales. */
    suspend fun login(email: String, passwordPlain: String): Result<User> =
        withContext(Dispatchers.IO) {
            val normalizado = email.trim().lowercase(Locale.getDefault())
            val usuario = userDao.getUserByEmail(normalizado)
                ?: return@withContext Result.failure(IllegalStateException(CODE_USER_NOT_FOUND))

            if (usuario.getPasswordHash() != PasswordHasher.hash(passwordPlain)) {
                return@withContext Result.failure(IllegalStateException(CODE_BAD_PASSWORD))
            }
            Result.success(usuario)
        }

    suspend fun getByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email.trim().lowercase(Locale.getDefault()))
    }

    companion object {
        const val CODE_EMAIL_EXISTS = "EMAIL_EXISTS"
        const val CODE_USER_NOT_FOUND = "USER_NOT_FOUND"
        const val CODE_BAD_PASSWORD = "BAD_PASSWORD"
    }
}
