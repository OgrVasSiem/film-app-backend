package api.library

import api.auth.requireAccount
import api.film.response
import api.routes.positiveId
import catalog.auth.AuthStore
import catalog.film.Film
import catalog.library.FilmStatus
import catalog.library.LibraryStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

internal fun Route.libraryRoutes(store: LibraryStore, auth: AuthStore) {
    get("/library") {
        val account = call.requireAccount(auth) ?: return@get
        val status = FilmStatus.entries.find { it.name == call.request.queryParameters["status"] }
            ?: throw BadRequestException("Invalid library status")
        call.respond(store.library(account.id, status).map(Film::response))
    }
    put("/library/{id}") {
        val account = call.requireAccount(auth) ?: return@put
        if (!store.setStatus(account, call.positiveId(), call.receive<StatusRequest>().status)) {
            throw NotFoundException()
        }
        call.respond(HttpStatusCode.NoContent)
    }
    delete("/library/{id}") {
        val account = call.requireAccount(auth) ?: return@delete
        store.clearStatus(account.id, call.positiveId())
        call.respond(HttpStatusCode.NoContent)
    }
}

@Serializable private data class StatusRequest(val status: FilmStatus)
