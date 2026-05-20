package unab.edu.co.abrahamcaceres.safebite.model.cloud

fun UserCloudModel.toMap(): Map<String, Any> = hashMapOf(
    "uid" to uid,
    "name" to name,
    "email" to email,
    "city" to city,
    "allergens" to allergens,
    "createdAt" to (createdAt ?: com.google.firebase.Timestamp.now())
)

fun ScanHistoryModel.toMap(): Map<String, Any> = hashMapOf(
    "scanId" to scanId,
    "scanDate" to scanDate,
    "productName" to productName,
    "rawTextDetected" to rawTextDetected,
    "status" to status,
    "matchedAllergens" to matchedAllergens
)

fun SightingCloudModel.toMap(): Map<String, Any> = hashMapOf(
    "sightingId" to sightingId,
    "creatorUid" to creatorUid,
    "creatorName" to creatorName,
    "productName" to productName,
    "storeName" to storeName,
    "price" to price,
    "communityTip" to communityTip,
    "allergenTag" to allergenTag,
    "targetCity" to targetCity,
    "createdAt" to (createdAt ?: com.google.firebase.Timestamp.now())
)
