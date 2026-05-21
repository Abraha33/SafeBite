package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirebaseFirestoreRepository
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirestoreDataRepository
import unab.edu.co.abrahamcaceres.safebite.data.repository.UserRepository
import unab.edu.co.abrahamcaceres.safebite.model.cloud.UserCloudModel
import unab.edu.co.abrahamcaceres.safebite.ui.profile.ProfileUiState
import unab.edu.co.abrahamcaceres.safebite.utils.SessionManager

class ProfileViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val firebaseFirestoreRepo: FirebaseFirestoreRepository,
    private val firestoreDataRepo: FirestoreDataRepository
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application.applicationContext)

    private val _profileState = MutableLiveData(ProfileUiState())
    val profileState: LiveData<ProfileUiState> = _profileState

    private val _shouldReturnToLogin = MutableLiveData(false)
    val shouldReturnToLogin: LiveData<Boolean> = _shouldReturnToLogin

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveSuccess = MutableLiveData<Boolean?>(null)
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    fun consumeSaveSuccess() {
        _saveSuccess.value = null
    }

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

            firebaseFirestoreRepo.fetchUserProfile(firebaseUser.uid).fold(
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

    fun saveProfileChanges(newName: String, newAllergens: List<String>) {
        val auth = FirebaseAuth.getInstance().currentUser ?: return
        _isSaving.value = true

        viewModelScope.launch {
            val update = UserCloudModel(
                uid = auth.uid,
                name = newName.trim(),
                email = auth.email ?: "",
                city = "",
                allergens = newAllergens
            )
            firestoreDataRepo.updateUserProfile(update).fold(
                onSuccess = {
                    _profileState.postValue(
                        ProfileUiState(
                            name = newName.trim(),
                            email = auth.email ?: "",
                            allergens = newAllergens
                        )
                    )
                    _isSaving.postValue(false)
                    _saveSuccess.postValue(true)
                },
                onFailure = {
                    _isSaving.postValue(false)
                    _saveSuccess.postValue(false)
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
