package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirestoreDataRepository

class BlacklistViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlacklistViewModel::class.java)) {
            return BlacklistViewModel(application, FirestoreDataRepository()) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
