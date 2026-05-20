package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirebaseFirestoreRepository
import unab.edu.co.abrahamcaceres.safebite.data.repository.UserRepository
import unab.edu.co.abrahamcaceres.safebite.ui.profile.ProfileUiState
import unab.edu.co.abrahamcaceres.safebite.utils.SessionManager

class ProfileViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val firestoreRepo: FirebaseFirestoreRepository
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application.applicationContext)

    private val _profileState = MutableLiveData(ProfileUiState())
    val profileState: LiveData<ProfileUiState> = _profileState

    private val _shouldReturnToLogin = MutableLiveData(false)
    val shouldReturnToLogin: LiveData<Boolean> = _shouldReturnToLogin

    fun loadCurrentUser() {
        viewModelScope.launch {
            val auth = FirebaseAuth.getInstance()
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                val email = sessionManager.getCurrentUserEmail()
                if (email.isNullOrBlank()) {
                    _profileState.postValue(ProfileUiState())
                    _shouldReturnToLogin.postValue(true)
                    return@launch
                }
                val localUser = userRepository.getByEmail(email)
                _profileState.postValue(
                    ProfileUiState(
                        name = localUser?.getFullName() ?: "",
                        email = localUser?.getEmail() ?: email
                    )
                )
                _shouldReturnToLogin.postValue(false)
                return@launch
            }

            firestoreRepo.fetchUserProfile(firebaseUser.uid).fold(
                onSuccess = { cloudUser ->
                    _profileState.postValue(
                        ProfileUiState(
                            name = cloudUser.name,
                            email = cloudUser.email,
                            allergens = cloudUser.allergens
                        )
                    )
                    _shouldReturnToLogin.postValue(false)
                },
                onFailure = {
                    val localUser = userRepository.getByEmail(firebaseUser.email ?: "")
                    _profileState.postValue(
                        ProfileUiState(
                            name = localUser?.getFullName() ?: firebaseUser.displayName ?: "",
                            email = localUser?.getEmail() ?: firebaseUser.email ?: ""
                        )
                    )
                    _shouldReturnToLogin.postValue(false)
                }
            )
        }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        sessionManager.clearSession()
        _shouldReturnToLogin.value = true
    }

    fun consumeReturnToLogin() {
        _shouldReturnToLogin.value = false
    }
}
