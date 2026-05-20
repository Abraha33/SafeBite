package unab.edu.co.abrahamcaceres.safebite.model.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

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
    @ServerTimestamp var createdAt: Timestamp? = null
)
