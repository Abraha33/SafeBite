package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.data.repository.AllergenRepository
import unab.edu.co.abrahamcaceres.safebite.data.repository.ScanHistoryRepository
import unab.edu.co.abrahamcaceres.safebite.data.repository.UserRepository
import unab.edu.co.abrahamcaceres.safebite.model.Allergen
import unab.edu.co.abrahamcaceres.safebite.model.User
import unab.edu.co.abrahamcaceres.safebite.utils.SessionManager

class ProfileViewModel(
    application: Application,
    private val userRepository: UserRepository,
    scanHistoryRepository: ScanHistoryRepository,
    private val allergenRepository: AllergenRepository
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application.applicationContext)

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser

    private val _shouldReturnToLogin = MutableLiveData(false)
    val shouldReturnToLogin: LiveData<Boolean> = _shouldReturnToLogin

    val allergens: LiveData<List<Allergen>> = allergenRepository.observeAllergens()

    val scanCount: LiveData<Int> = scanHistoryRepository.observeScans().map { scans ->
        scans?.size ?: 0
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val email = sessionManager.getCurrentUserEmail()
            if (email.isNullOrBlank()) {
                _currentUser.postValue(null)
                _shouldReturnToLogin.postValue(true)
                return@launch
            }

            val user = userRepository.getByEmail(email)
            _currentUser.postValue(user)
            _shouldReturnToLogin.postValue(user == null)
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _shouldReturnToLogin.value = true
    }

    fun consumeReturnToLogin() {
        _shouldReturnToLogin.value = false
    }
}
