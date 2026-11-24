package api.packs

import api.auth.requireAccount
import api.dto.ErrorResponse
import api.film.FilmResponse
import api.film.response
import api.routes.positiveId
import catalog.auth.AuthStore
import catalog.packs.PackStore
import catalog.packs.FilmCursor
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.*

@kotlinx.serialization.Serializable
private data class FilmPageResponse(val items: List<FilmResponse>, val nextCursor: String?)

internal fun Route.filmPageRoutes(store: PackStore, auth: AuthStore) {
    get("/packs/{id}/films/page") {
        val account = call.requireAccount(auth) ?: return@get
        val id = call.positiveId()
        val pack = store.pack(id, account.id) ?: throw NotFoundException()
        if (pack.premium && !account.premium) {
            return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("PREMIUM_REQUIRED", "Premium access required"))
        }
        val params = call.request.queryParameters
        val limit = (params["limit"] ?: "10").toIntOrNull()?.takeIf { it in 1..30 }
            ?: throw BadRequestException("Invalid limit")
        val cursor = params["cursor"]?.let { raw ->
            val parts = raw.split(':', limit = 3)
            val position = parts.getOrNull(1)?.toIntOrNull()
            val filmId = parts.getOrNull(2)?.toLongOrNull()
            if (parts.first().toLongOrNull() != id || position == null || filmId == null || filmId <= 0)
                throw BadRequestException("Invalid cursor")
            FilmCursor(position, filmId)
        }
        val rows = store.filmPage(id, account.id, cursor, limit + 1)
        val page = rows.take(limit)
        val next = if (rows.size > limit) page.last().let { "$id:${it.position}:${it.film.id}" } else null
        call.respond(FilmPageResponse(page.map { it.film.response() }, next))
    }
}
