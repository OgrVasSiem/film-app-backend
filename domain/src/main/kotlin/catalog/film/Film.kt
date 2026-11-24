package catalog.film

data class Film(
    val id: Long,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val year: Int?,
    val director: String?,
    val actors: List<String>,
    val durationMinutes: Int?,
    val genres: List<String>,
)
