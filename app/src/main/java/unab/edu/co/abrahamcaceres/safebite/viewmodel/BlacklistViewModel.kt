package unab.edu.co.abrahamcaceres.safebite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import unab.edu.co.abrahamcaceres.safebite.data.repository.FirestoreDataRepository
import unab.edu.co.abrahamcaceres.safebite.model.cloud.ProductModel

class BlacklistViewModel(
    application: Application,
    private val firestoreDataRepo: FirestoreDataRepository
) : AndroidViewModel(application) {

    private val _blacklist = MutableLiveData<List<ProductModel>>(emptyList())
    val blacklist: LiveData<List<ProductModel>> = _blacklist

    private val _isEmpty = MutableLiveData(true)
    val isEmpty: LiveData<Boolean> = _isEmpty

    fun observeBlacklist() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            firestoreDataRepo.getBlacklist(uid).collect { list ->
                _blacklist.postValue(list)
                _isEmpty.postValue(list.isEmpty())
            }
        }
    }

    fun removeProduct(productId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            firestoreDataRepo.removeFromBlacklist(uid, productId)
        }
    }
}
