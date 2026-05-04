package unab.edu.co.abrahamcaceres.safebite.utils

import android.util.Patterns

/**
 * Reglas de validación reutilizables para formularios de autenticación.
 */
object InputValidators {

    private const val MIN_PASSWORD_LENGTH = 6
    private const val MIN_NAME_LENGTH = 2

    fun isNotBlank(value: String): Boolean = value.trim().isNotEmpty()

    fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isValidPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH

    fun isValidName(name: String): Boolean =
        name.trim().length >= MIN_NAME_LENGTH
}
