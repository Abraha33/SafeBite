package unab.edu.co.abrahamcaceres.safebite.model.cloud

data class SightingCloudModel(
    var sightingId: String = "",
    var creatorUid: String = "",
    var creatorName: String = "",
    var productName: String = "",
    var storeName: String = "",
    var price: Double = 0.0,
    var communityTip: String = "",
    var allergenTag: String = "",
    var targetCity: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Long = System.currentTimeMillis()
)
