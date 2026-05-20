package unab.edu.co.abrahamcaceres.safebite.model.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class UserCloudModel(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var city: String = "",
    var allergens: List<String> = emptyList(),
    @ServerTimestamp var createdAt: Timestamp? = null
)
