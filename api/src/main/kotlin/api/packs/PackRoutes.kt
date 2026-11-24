package api.packs

import api.auth.requireAccount
import api.dto.ErrorResponse
import api.film.response
import api.routes.positiveId
import catalog.auth.AuthStore
import catalog.film.Film
import catalog.packs.PackStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.*

internal fun Route.packRoutes(store: PackStore, auth: AuthStore) {
    get("/packs") {
        val account = call.requireAccount(auth) ?: return@get
        call.respond(store.packs(account.id).map { it.response(account.premium) })
    }
    filmPageRoutes(store, auth)
    get("/packs/catalog") {
        val account = call.requireAccount(auth) ?: return@get
        call.respondCatalog(account.id, store.packs(account.id).map { it.response(account.premium) })
    }
    get("/packs/{id}/films") {
        val account = call.requireAccount(auth) ?: return@get
        val pack = store.pack(call.positiveId(), account.id) ?: throw NotFoundException()
        if (pack.premium && !account.premium) {
            return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("PREMIUM_REQUIRED", "Premium access required"))
        }
        call.respond(store.packFilms(pack.id, account.id).map(Film::response))
    }
}
