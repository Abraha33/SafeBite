package unab.edu.co.abrahamcaceres.safebite.utils

import android.content.Context

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

    fun saveRememberedCredentials(email: String, password: String) {
        prefs.edit()
            .putString(KEY_REMEMBERED_EMAIL, email.trim())
            .putString(KEY_REMEMBERED_PASSWORD, password)
            .putBoolean(KEY_REMEMBER_ME, true)
            .apply()
    }

    fun getRememberedEmail(): String? {
        return if (prefs.getBoolean(KEY_REMEMBER_ME, false)) {
            prefs.getString(KEY_REMEMBERED_EMAIL, null)
        } else null
    }

    fun getRememberedPassword(): String? {
        return if (prefs.getBoolean(KEY_REMEMBER_ME, false)) {
            prefs.getString(KEY_REMEMBERED_PASSWORD, null)
        } else null
    }

    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun clearRememberedCredentials() {
        prefs.edit()
            .remove(KEY_REMEMBERED_EMAIL)
            .remove(KEY_REMEMBERED_PASSWORD)
            .putBoolean(KEY_REMEMBER_ME, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "safebite_session"
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        private const val KEY_REMEMBERED_EMAIL = "remembered_email"
        private const val KEY_REMEMBERED_PASSWORD = "remembered_password"
        private const val KEY_REMEMBER_ME = "remember_me_enabled"
    }
}
