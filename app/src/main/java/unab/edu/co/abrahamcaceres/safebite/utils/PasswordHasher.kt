package unab.edu.co.abrahamcaceres.safebite.utils

import java.security.MessageDigest

/**
 * Evita guardar contraseñas en texto plano en la base local.
 * Nota académica: en producción se recomienda Argon2/bcrypt y autenticación remota.
 */
object PasswordHasher {

    /** Genera un hash SHA-256 en hexadecimal a partir de la contraseña en claro. */
    fun hash(plainPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(plainPassword.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
