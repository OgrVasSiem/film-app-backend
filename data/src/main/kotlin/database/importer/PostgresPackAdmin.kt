package database.importer

import catalog.packs.ImportEvent
import catalog.packs.PackAdmin
import catalog.packs.PackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.Connection
import javax.sql.DataSource

private data class Pending(val id: Long, val title: String, val year: Int?)
private const val IMPORT_LOCK = 8925601421L

internal class PostgresPackAdmin(private val source: DataSource, key: String) : PackAdmin {
    private val provider = PoiskKino(key)
    private val log = LoggerFactory.getLogger("CatalogImport")

    override suspend fun sync(packId: Long): PackResult = withContext(Dispatchers.IO) {
        source.connection.use { db ->
            if (!db.flag("SELECT pg_try_advisory_lock(?)", IMPORT_LOCK)) return@use PackResult("BUSY")
            try {
                if (!db.flag("SELECT EXISTS(SELECT 1 FROM film_packs WHERE id=?)", packId))
                    return@use PackResult("NOT_FOUND")
                val pending = db.prepareStatement(
                    """SELECT f.id, f.title, f.release_year FROM films f JOIN pack_films pf ON pf.film_id=f.id
                       WHERE pf.pack_id=? AND f.poiskkino_id IS NULL ORDER BY pf.position,f.id"""
                ).use { q ->
                    q.setLong(1, packId)
                    q.executeQuery().use { rows -> buildList {
                        while (rows.next()) add(Pending(rows.getLong(1), rows.getString(2),
                            rows.getObject(3, Int::class.javaObjectType)))
                    } }
                }
                val events = mutableListOf<ImportEvent>()
                var state = "DONE"
                for (film in pending) {
                    val result = try {
                        importFilm(db, packId, film)
                    } catch (e: ImportStopped) {
                        state = e.reason
                        "PENDING"
                    } catch (e: Exception) {
                        log.warn("Import paused pack={} film={} cause={}", packId, film.id, e.javaClass.simpleName)
                        state = "IMPORT_ERROR"
                        "PENDING"
                    }
                    events += ImportEvent(film.id, film.title, result)
                    log.info("Import pack={} film={} title={} result={}", packId, film.id, film.title, result)
                    if (state != "DONE") break
                }
                log.info("Import pack={} state={} processed={}", packId, state, events.size)
                PackResult(state, events)
            } finally {
                db.flag("SELECT pg_advisory_unlock(?)", IMPORT_LOCK)
            }
        }
    }

    private fun importFilm(db: Connection, packId: Long, film: Pending): String {
        val year = film.year ?: return "YEAR_REQUIRED"
        if (film.title.isBlank()) return "TITLE_REQUIRED"
        val matches = provider.matches(db, film.title, year)
        val details = matches.singleOrNull()?.let { provider.details(db, it) }
        val guard = """ WHERE id=? AND title=? AND release_year=? AND poiskkino_id IS NULL
            AND EXISTS(SELECT 1 FROM pack_films WHERE film_id=films.id AND pack_id=?)"""
        val sql = if (details == null) "DELETE FROM films" else
            """UPDATE films SET poiskkino_id=?, poster_url=coalesce(?,poster_url),
               description=coalesce(?,description), duration_minutes=coalesce(?,duration_minutes),
               director=coalesce(?,director),
               actors=coalesce(?::text[],actors), genres=coalesce(?::text[],genres)"""
        return db.prepareStatement(sql + guard).use { q ->
            var i = 1
            if (details != null) {
                q.setLong(i++, details.id)
                q.setString(i++, details.poster)
                q.setString(i++, details.description)
                q.setObject(i++, details.minutes)
                q.setString(i++, details.director)
                for (list in listOf(details.actors, details.genres)) {
                    q.setArray(i++, list.takeIf { it.isNotEmpty() }?.let { db.createArrayOf("text", it.toTypedArray()) })
                }
            }
            q.setLong(i++, film.id)
            q.setString(i++, film.title)
            q.setInt(i++, year)
            q.setLong(i, packId)
            when {
                q.executeUpdate() == 0 -> "CHANGED_DURING_SYNC"
                details != null -> "IMPORTED"
                matches.isEmpty() -> "DELETED_NO_MATCH"
                else -> "DELETED_AMBIGUOUS"
            }
        }
    }

    override suspend fun publish(packId: Long): PackResult = withContext(Dispatchers.IO) {
        source.connection.use { db ->
            val updated = db.prepareStatement(
                """UPDATE film_packs SET is_published=true WHERE id=?
                   AND EXISTS(SELECT 1 FROM pack_films WHERE pack_id=film_packs.id)
                   AND NOT EXISTS(SELECT 1 FROM incomplete_pack_films WHERE pack_id=film_packs.id)"""
            ).use { q -> q.setLong(1, packId); q.executeUpdate() }
            if (updated > 0) return@use PackResult("PUBLISHED")
            if (!db.flag("SELECT EXISTS(SELECT 1 FROM film_packs WHERE id=?)", packId))
                return@use PackResult("NOT_FOUND")
            val issues = db.prepareStatement(
                """SELECT i.film_id,i.title,concat_ws(', ',
                    CASE WHEN trim(f.title)='' THEN 'title' END,
                    CASE WHEN release_year IS NULL THEN 'release_year' END,
                    CASE WHEN nullif(trim(poster_url),'') IS NULL THEN 'poster_url' END,
                    CASE WHEN trim(description)='' THEN 'description' END)
                   FROM incomplete_pack_films i JOIN films f ON f.id=i.film_id WHERE i.pack_id=?"""
            ).use { q ->
                q.setLong(1, packId)
                q.executeQuery().use { rows -> buildList {
                    while (rows.next()) add(ImportEvent(rows.getLong(1), rows.getString(2), rows.getString(3)))
                } }
            }
            PackResult(if (issues.isEmpty()) "EMPTY" else "INCOMPLETE", issues)
        }
    }

    private fun Connection.flag(sql: String, id: Long): Boolean = prepareStatement(sql).use { q ->
        q.setLong(1, id)
        q.executeQuery().use { rows -> rows.next() && rows.getBoolean(1) }
    }
}
