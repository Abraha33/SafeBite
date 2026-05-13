package unab.edu.co.abrahamcaceres.safebite.utils

import android.content.Context

/**
 * Maneja la sesiÃ³n local del usuario autenticado usando SharedPreferences.
 * Guardamos el correo normalizado porque ya es una clave Ãºnica en Room.
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCurrentUserEmail(email: String) {
        prefs.edit()
            .putString(KEY_CURRENT_USER_EMAIL, email.trim())
            .apply()
    }

    fun getCurrentUserEmail(): String? {
        return prefs.getString(KEY_CURRENT_USER_EMAIL, null)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_CURRENT_USER_EMAIL)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "safebite_session"
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
    }
}
