package unab.edu.co.abrahamcaceres.safebite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sightings")
class Sighting(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private var id: Long = 0L,
    @ColumnInfo(name = "creator_name")
    private var creatorName: String = "",
    @ColumnInfo(name = "time_ago")
    private var timeAgo: String = "",
    @ColumnInfo(name = "title")
    private var title: String = "",
    @ColumnInfo(name = "allergen_status_text")
    private var allergenStatusText: String = "",
    @ColumnInfo(name = "allergen_risk_level")
    private var allergenRiskLevel: String = ScanRisk.SAFE,
    @ColumnInfo(name = "description")
    private var description: String = "",
    @ColumnInfo(name = "location_name")
    private var locationName: String = ""
) {

    fun getId(): Long = id
    fun getCreatorName(): String = creatorName
    fun getTimeAgo(): String = timeAgo
    fun getTitle(): String = title
    fun getAllergenStatusText(): String = allergenStatusText
    fun getAllergenRiskLevel(): String = allergenRiskLevel
    fun getDescription(): String = description
    fun getLocationName(): String = locationName

    companion object {
        fun crear(
            creatorName: String,
            timeAgo: String,
            title: String,
            allergenStatusText: String,
            allergenRiskLevel: String,
            description: String,
            locationName: String
        ): Sighting {
            return Sighting(
                id = 0L,
                creatorName = creatorName.trim(),
                timeAgo = timeAgo.trim(),
                title = title.trim(),
                allergenStatusText = allergenStatusText.trim(),
                allergenRiskLevel = allergenRiskLevel,
                description = description.trim(),
                locationName = locationName.trim()
            )
        }
    }
}
