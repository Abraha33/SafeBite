package unab.edu.co.abrahamcaceres.safebite.model.cloud

data class UserCloudModel(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var city: String = "",
    var allergens: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis()
)
