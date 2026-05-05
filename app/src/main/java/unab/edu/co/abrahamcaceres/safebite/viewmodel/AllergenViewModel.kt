package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.R
import unab.edu.co.abrahamcaceres.safebite.data.repository.AllergenRepository
import unab.edu.co.abrahamcaceres.safebite.model.Allergen

/**
 * MVVM: lista reactiva de alérgenos y operaciones de alta/baja sobre Room.
 */
class AllergenViewModel(
    application: Application,
    private val repository: AllergenRepository
) : AndroidViewModel(application) {

    private val app: Application = application

    val allergens: LiveData<List<Allergen>> = repository.observeAllergens()

    private val _formError = MutableLiveData<String?>(null)
    val formError: LiveData<String?> = _formError

    /** Limpia el mensaje de error del formulario (p. ej. al escribir de nuevo). */
    fun clearFormError() {
        _formError.value = null
    }

    /**
     * Inserta un alérgeno si pasa validación básica y no está duplicado.
     * La validación principal de vacío/longitud puede hacerse en la UI; aquí reforzamos duplicados.
     */
    fun insertAllergen(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) {
            _formError.value = app.getString(R.string.error_allergen_empty)
            return
        }
        if (name.length < 3) {
            _formError.value = app.getString(R.string.error_allergen_short)
            return
        }

        viewModelScope.launch {
            val entity = Allergen.desdeTextoPlano(name)
            val result = repository.insert(entity)
            result.fold(
                onSuccess = { _formError.postValue(null) },
                onFailure = { error ->
                    when (error.message) {
                        AllergenRepository.CODE_DUPLICATE ->
                            _formError.postValue(app.getString(R.string.error_allergen_duplicate))

                        else ->
                            _formError.postValue(app.getString(R.string.error_unknown))
                    }
                }
            )
        }
    }

    fun deleteAllergen(allergen: Allergen) {
        viewModelScope.launch {
            repository.delete(allergen)
        }
    }
}
