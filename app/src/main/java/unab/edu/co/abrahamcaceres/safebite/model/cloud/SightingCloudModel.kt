package unab.edu.co.abrahamcaceres.safebite.model.cloud

import com.google.firebase.Timestamp

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
    var createdAt: Timestamp? = null
)
