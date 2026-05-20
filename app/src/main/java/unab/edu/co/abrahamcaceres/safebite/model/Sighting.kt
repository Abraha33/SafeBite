package unab.edu.co.abrahamcaceres.safebite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sightings")
class Sighting(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private var id: Long = 0L,
    @ColumnInfo(name = "product_name")
    private var productName: String = "",
    @ColumnInfo(name = "store_name")
    private var storeName: String = "",
    @ColumnInfo(name = "price")
    private var price: String = "",
    @ColumnInfo(name = "community_tip")
    private var communityTip: String = "",
    @ColumnInfo(name = "city")
    private var city: String = "",
    @ColumnInfo(name = "allergen_tag")
    private var allergenTag: String = "",
    @ColumnInfo(name = "latitude")
    private var latitude: Double = 0.0,
    @ColumnInfo(name = "longitude")
    private var longitude: Double = 0.0
) {

    fun getId(): Long = id
    fun getProductName(): String = productName
    fun getStoreName(): String = storeName
    fun getPrice(): String = price
    fun getCommunityTip(): String = communityTip
    fun getCity(): String = city
    fun getAllergenTag(): String = allergenTag
    fun getLatitude(): Double = latitude
    fun getLongitude(): Double = longitude

    companion object {
        fun crear(
            productName: String,
            storeName: String,
            price: String,
            communityTip: String,
            city: String,
            allergenTag: String,
            latitude: Double = 0.0,
            longitude: Double = 0.0
        ): Sighting {
            return Sighting(
                id = 0L,
                productName = productName.trim(),
                storeName = storeName.trim(),
                price = price.trim(),
                communityTip = communityTip.trim(),
                city = city.trim(),
                allergenTag = allergenTag.trim(),
                latitude = latitude,
                longitude = longitude
            )
        }
    }
}
