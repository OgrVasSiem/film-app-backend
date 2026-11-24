package database.packs

import catalog.packs.FilmPack
import catalog.packs.FilmCursor
import catalog.packs.PositionedFilm
import catalog.packs.PackStore
import database.film.film
import database.jdbc.query
import java.sql.ResultSet
import javax.sql.DataSource

internal class PostgresPackStore(private val source: DataSource) : PackStore {
    private val catalog = """
        SELECT v.*, p.cover_file, NOT EXISTS (
            SELECT 1 FROM pack_films pf JOIN films f ON f.id=pf.film_id
            WHERE pf.pack_id=v.id AND NOT film_is_decided(?,f)
        ) AS completed
        FROM visible_film_packs v JOIN film_packs p ON p.id=v.id
    """.trimIndent()

    override suspend fun packs(accountId: Long) =
        source.query("$catalog ORDER BY v.id", accountId, map = ::filmPack)

    override suspend fun pack(id: Long, accountId: Long) =
        source.query("$catalog WHERE v.id=?", accountId, id, map = ::filmPack).singleOrNull()

    override suspend fun packFilms(packId: Long, accountId: Long) = source.query(
        """SELECT f.* FROM pack_films p JOIN films f ON f.id = p.film_id
           JOIN visible_film_packs v ON v.id=p.pack_id
           WHERE p.pack_id=? AND NOT film_is_decided(?,f)
           ORDER BY p.position,f.id""",
        packId, accountId, map = ::film
    )

    override suspend fun filmPage(packId: Long, accountId: Long, cursor: FilmCursor?, fetch: Int): List<PositionedFilm> {
        val after = if (cursor == null) "" else "AND (p.position, f.id) > (?, ?)"
        val parameters = mutableListOf<Any>(packId, accountId)
        if (cursor != null) {
            parameters.add(cursor.position)
            parameters.add(cursor.filmId)
        }
        parameters.add(fetch)
        return source.query(
            """SELECT f.*, p.position FROM pack_films p JOIN films f ON f.id=p.film_id
               JOIN visible_film_packs v ON v.id=p.pack_id
               WHERE p.pack_id=? AND NOT film_is_decided(?,f) $after
               ORDER BY p.position,f.id LIMIT ?""",
            *parameters.toTypedArray()
        ) { row -> PositionedFilm(film(row), row.getInt("position")) }
    }

    private fun filmPack(row: ResultSet) = FilmPack(
        row.getLong("id"), row.getString("title"), row.getString("description"),
        row.getBoolean("premium"), row.getString("cover_file"), row.getBoolean("completed")
    )
}
