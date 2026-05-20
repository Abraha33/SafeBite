package unab.edu.co.abrahamcaceres.safebite.model.cloud

import com.google.firebase.Timestamp

data class UserCloudModel(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var city: String = "",
    var allergens: List<String> = emptyList(),
    var createdAt: Timestamp? = null
)
