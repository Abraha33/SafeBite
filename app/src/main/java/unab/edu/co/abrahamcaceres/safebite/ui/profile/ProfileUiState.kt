package unab.edu.co.abrahamcaceres.safebite.ui.profile

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val allergens: List<String> = emptyList()
)
