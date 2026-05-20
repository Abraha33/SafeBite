package unab.edu.co.abrahamcaceres.safebite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sightings_table")
class Sighting(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private var id: Long = 0L,
    @ColumnInfo(name = "creator_name")
    private var creatorName: String = "",
    @ColumnInfo(name = "time_ago")
    private var timeAgo: String = "",
    @ColumnInfo(name = "product_name")
    private var productName: String = "",
    @ColumnInfo(name = "store_name")
    private var storeName: String = "",
    @ColumnInfo(name = "price")
    private var price: String = "",
    @ColumnInfo(name = "community_tip")
    private var communityTip: String = "",
    @ColumnInfo(name = "target_city")
    private var targetCity: String = "",
    @ColumnInfo(name = "allergen_tag")
    private var allergenTag: String = ""
) {

    fun getId(): Long = id
    fun getCreatorName(): String = creatorName
    fun getTimeAgo(): String = timeAgo
    fun getProductName(): String = productName
    fun getStoreName(): String = storeName
    fun getPrice(): String = price
    fun getCommunityTip(): String = communityTip
    fun getTargetCity(): String = targetCity
    fun getAllergenTag(): String = allergenTag

    companion object {
        fun crear(
            creatorName: String,
            timeAgo: String,
            productName: String,
            storeName: String,
            price: String = "",
            communityTip: String,
            targetCity: String,
            allergenTag: String
        ): Sighting {
            return Sighting(
                id = 0L,
                creatorName = creatorName.trim(),
                timeAgo = timeAgo.trim(),
                productName = productName.trim(),
                storeName = storeName.trim(),
                price = price.trim(),
                communityTip = communityTip.trim(),
                targetCity = targetCity.trim(),
                allergenTag = allergenTag.trim()
            )
        }
    }
}
