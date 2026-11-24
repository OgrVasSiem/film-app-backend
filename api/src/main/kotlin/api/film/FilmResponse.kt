package api.film

import catalog.film.Film
import kotlinx.serialization.Serializable

@Serializable internal data class FilmResponse(
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

internal fun Film.response() = FilmResponse(
    id, title, description, posterUrl, year, director, actors, durationMinutes, genres
)
