package unab.edu.co.abrahamcaceres.safebite.model.cloud

data class ProductModel(
    var id: String = "",
    var productName: String = "",
    var detectedText: String = "",
    var riskLevel: String = "SAFE",
    var matchedAllergens: List<String> = emptyList(),
    var addedAt: Long = System.currentTimeMillis()
)
