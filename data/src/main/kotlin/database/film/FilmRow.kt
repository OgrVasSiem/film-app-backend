package database.film

import catalog.film.Film
import java.sql.ResultSet

internal fun film(row: ResultSet) =
    Film(
        id = row.getLong("id"),
        title = row.getString("title"),
        description = row.getString("description"),
        posterUrl = row.getString("poster_url"),
        year = row.getObject("release_year", Int::class.javaObjectType),
        director = row.getString("director"),
        actors = textList(row, "actors").take(5),
        durationMinutes = row.getObject("duration_minutes", Int::class.javaObjectType),
        genres = textList(row, "genres"),
    )

private fun textList(row: ResultSet, column: String): List<String> {
    val values = row.getArray(column)
    return try {
        (values.array as Array<*>).map { it as String }
    } finally {
        values.free()
    }
}
