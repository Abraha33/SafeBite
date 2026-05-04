package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import unab.edu.co.abrahamcaceres.safebite.SafeBiteApplication
import unab.edu.co.abrahamcaceres.safebite.data.repository.UserRepository

/**
 * Fábrica que inyecta el repositorio en el [AuthViewModel] (patrón recomendado con ViewModelProvider).
 */
class AuthViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val database = (application as SafeBiteApplication).database
            val repository = UserRepository(database.userDao())
            return AuthViewModel(application, repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
