package database.importer

import kotlinx.serialization.json.*
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.time.Duration
import java.util.Locale

internal data class Details(
    val id: Long, val poster: String?, val description: String?, val minutes: Int?,
    val director: String?, val actors: List<String>, val genres: List<String>
)

private fun JsonObject.field(name: String) = this[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
private fun JsonObject.objects(name: String) =
    (this[name] as? JsonArray).orEmpty().map { it.jsonObject }
private fun normalize(value: String) = value.trim().lowercase(Locale.ROOT).replace('ё','е')
    .replace(Regex("\\s+"), " ")

internal class PoiskKino(private val key: String) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()

    private fun get(db: Connection, path: String): JsonObject {
        if (key.isBlank()) throw ImportStopped("KEY_MISSING")
        val request = HttpRequest.newBuilder(URI.create("https://api.poiskkino.dev/v1.5/" + path))
            .header("X-API-KEY", key).timeout(Duration.ofSeconds(20)).GET().build()
        db.reserveRequest()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        when (val code = response.statusCode()) {
            200 -> return Json.parseToJsonElement(response.body()).jsonObject
            403, 429 -> { db.pauseProvider(); throw ImportStopped("PROVIDER_PAUSED_$code") }
            else -> throw ImportStopped("PROVIDER_HTTP_$code")
        }
    }

    fun matches(db: Connection, title: String, year: Int): Set<Long> {
        val query = URLEncoder.encode(title, StandardCharsets.UTF_8)
        val ids = mutableSetOf<Long>()
        var page = 1
        var seen = 0
        do {
            val data = get(db, "movie/search?query=$query&limit=250&page=$page")
            val docs = data.getValue("docs").jsonArray
            val total = data.getValue("total").jsonPrimitive.int
            val pages = data.getValue("pages").jsonPrimitive.int
            check(total >= 0 && pages >= 0 && data.getValue("page").jsonPrimitive.int == page)
            for (item in docs) {
                val film = item.jsonObject
                val names = listOfNotNull(film.field("name"), film.field("alternativeName"), film.field("enName")) +
                    film.objects("names").mapNotNull { it.field("name") }
                if (film.field("year")?.toIntOrNull() == year && names.any { normalize(it) == normalize(title) }) {
                    ids += film.getValue("id").jsonPrimitive.long
                }
            }
            seen += docs.size
            if (ids.size > 1) break
            if (page >= pages) {
                check(seen >= total) { "Incomplete search" }
                break
            }
            check(docs.isNotEmpty())
            page++
        } while (true)
        return ids
    }

    fun details(db: Connection, id: Long): Details {
        val film = get(db, "movie/$id")
        check(film.getValue("id").jsonPrimitive.long == id)
        val persons = film.objects("persons")
        fun names(role: String) = persons.filter { it.field("enProfession") == role }
            .mapNotNull { it.field("name") ?: it.field("enName") }.distinct()
        return Details(id, (film["poster"] as? JsonObject)?.field("url"),
            film.field("shortDescription") ?: film.field("description"),
            film.field("movieLength")?.toIntOrNull()?.takeIf { it > 0 },
            names("director").joinToString(", ").ifBlank { null }, names("actor").take(5),
            film.objects("genres").mapNotNull { it.field("name") }.distinct())
    }
}
