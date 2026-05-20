package unab.edu.co.abrahamcaceres.safebite.model.cloud

data class ScanHistoryModel(
    var scanId: String = "",
    var scanDate: Long = System.currentTimeMillis(),
    var productName: String = "",
    var rawTextDetected: String = "",
    var status: String = "",
    var matchedAllergens: List<String> = emptyList()
)
