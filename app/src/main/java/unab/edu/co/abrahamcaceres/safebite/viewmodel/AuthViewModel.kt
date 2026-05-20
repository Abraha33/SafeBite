package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirebaseFirestoreRepository
import unab.edu.co.abrahamcaceres.safebite.model.cloud.UserCloudModel
import unab.edu.co.abrahamcaceres.safebite.utils.InputValidators
import unab.edu.co.abrahamcaceres.safebite.utils.SessionManager

class AuthViewModel(
    application: Application,
    private val firebaseAuth: FirebaseAuth,
    private val firestoreRepo: FirebaseFirestoreRepository
) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val sessionManager = SessionManager(application.applicationContext)

    private val _nameError = MutableLiveData<String?>(null)
    val nameError: LiveData<String?> = _nameError

    private val _emailError = MutableLiveData<String?>(null)
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>(null)
    val passwordError: LiveData<String?> = _passwordError

    private val _registerSuccess = MutableLiveData(false)
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _loginSuccess = MutableLiveData(false)
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun consumeRegisterSuccess() {
        _registerSuccess.value = false
    }

    fun consumeLoginSuccess() {
        _loginSuccess.value = false
    }

    fun validateAndRegister(
        name: String,
        email: String,
        password: String,
        city: String = "Bucaramanga",
        allergens: List<String> = emptyList()
    ) {
        if (!validateRegisterFields(name, email, password)) return

        viewModelScope.launch {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid
                    ?: throw IllegalStateException("AUTH_FAILED")

                val profile = UserCloudModel(
                    uid = uid,
                    name = name,
                    email = email,
                    city = city,
                    allergens = allergens
                )
                firestoreRepo.saveUserProfile(profile).fold(
                    onSuccess = { _registerSuccess.postValue(true) },
                    onFailure = { _emailError.postValue(appContext.getString(R.string.error_unknown)) }
                )
            } catch (e: FirebaseAuthUserCollisionException) {
                _emailError.postValue(appContext.getString(R.string.error_email_registered))
            } catch (e: FirebaseAuthWeakPasswordException) {
                _passwordError.postValue(appContext.getString(R.string.error_password_short))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _emailError.postValue(appContext.getString(R.string.error_email_invalid))
            } catch (e: Exception) {
                _emailError.postValue(appContext.getString(R.string.error_unknown))
            }
        }
    }

    fun validateAndLogin(email: String, password: String) {
        if (!validateLoginFields(email, password)) return

        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                sessionManager.saveCurrentUserEmail(email)
                _loginSuccess.postValue(true)
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _emailError.postValue(appContext.getString(R.string.error_user_not_found))
                _passwordError.postValue(appContext.getString(R.string.error_bad_password))
            } catch (e: Exception) {
                _emailError.postValue(appContext.getString(R.string.error_unknown))
            }
        }
    }

    private fun validateRegisterFields(name: String, email: String, password: String): Boolean {
        clearFieldErrors()
        var valid = true

        when {
            !InputValidators.isNotBlank(name) -> {
                _nameError.value = appContext.getString(R.string.error_required)
                valid = false
            }
            !InputValidators.isValidName(name) -> {
                _nameError.value = appContext.getString(R.string.error_name_short)
                valid = false
            }
        }

        when {
            !InputValidators.isNotBlank(email) -> {
                _emailError.value = appContext.getString(R.string.error_required)
                valid = false
            }
            !InputValidators.isValidEmail(email) -> {
                _emailError.value = appContext.getString(R.string.error_email_invalid)
                valid = false
            }
        }

        when {
            !InputValidators.isNotBlank(password) -> {
                _passwordError.value = appContext.getString(R.string.error_required)
                valid = false
            }
            !InputValidators.isValidPassword(password) -> {
                _passwordError.value = appContext.getString(R.string.error_password_short)
                valid = false
            }
        }

        return valid
    }

    private fun validateLoginFields(email: String, password: String): Boolean {
        clearFieldErrors()
        var valid = true

        when {
            !InputValidators.isNotBlank(email) -> {
                _emailError.value = appContext.getString(R.string.error_required)
                valid = false
            }
            !InputValidators.isValidEmail(email) -> {
                _emailError.value = appContext.getString(R.string.error_email_invalid)
                valid = false
            }
        }

        when {
            !InputValidators.isNotBlank(password) -> {
                _passwordError.value = appContext.getString(R.string.error_required)
                valid = false
            }
            !InputValidators.isValidPassword(password) -> {
                _passwordError.value = appContext.getString(R.string.error_password_short)
                valid = false
            }
        }

        return valid
    }

    private fun clearFieldErrors() {
        _nameError.value = null
        _emailError.value = null
        _passwordError.value = null
    }
}
