package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.UserRepository
import unab.edu.co.abrahamcaceres.safebite.model.User
import unab.edu.co.abrahamcaceres.safebite.utils.InputValidators
import unab.edu.co.abrahamcaceres.safebite.utils.SessionManager

/**
 * LÃ³gica de negocio MVVM para Login y Registro (validaciÃ³n + acceso a Room vÃ­a repositorio).
 */
class AuthViewModel(
    application: Application,
    private val repository: UserRepository
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

    /** Reinicia el evento de registro exitoso para evitar navegaciones duplicadas al rotar. */
    fun consumeRegisterSuccess() {
        _registerSuccess.value = false
    }

    /** Reinicia el evento de login exitoso. */
    fun consumeLoginSuccess() {
        _loginSuccess.value = false
    }

    /**
     * Valida campos y, si todo es correcto, persiste el usuario en Room.
     */
    fun validateAndRegister(name: String, email: String, password: String) {
        if (!validateRegisterFields(name, email, password)) return

        viewModelScope.launch {
            val user = User.desdeRegistro(
                nombreCompleto = name,
                correo = email,
                contrasenaPlano = password
            )
            val result = repository.register(user)
            result.fold(
                onSuccess = { _registerSuccess.postValue(true) },
                onFailure = { error ->
                    when (error.message) {
                        UserRepository.CODE_EMAIL_EXISTS ->
                            _emailError.postValue(appContext.getString(R.string.error_email_registered))

                        else ->
                            _emailError.postValue(appContext.getString(R.string.error_unknown))
                    }
                }
            )
        }
    }

    /**
     * Valida credenciales y consulta Room para iniciar sesiÃ³n local.
     */
    fun validateAndLogin(email: String, password: String) {
        if (!validateLoginFields(email, password)) return

        viewModelScope.launch {
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    sessionManager.saveCurrentUserEmail(user.getEmail())
                    _loginSuccess.postValue(true)
                },
                onFailure = { error ->
                    when (error.message) {
                        UserRepository.CODE_USER_NOT_FOUND ->
                            _emailError.postValue(appContext.getString(R.string.error_user_not_found))

                        UserRepository.CODE_BAD_PASSWORD ->
                            _passwordError.postValue(appContext.getString(R.string.error_bad_password))

                        else ->
                            _emailError.postValue(appContext.getString(R.string.error_unknown))
                    }
                }
            )
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
