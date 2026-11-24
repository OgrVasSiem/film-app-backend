package api.admin

import api.routes.positiveId
import catalog.packs.PackAdmin
import catalog.packs.PackResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable private data class Event(val filmId: Long, val title: String, val result: String)
@Serializable private data class Report(val state: String, val events: List<Event>)

fun Route.packAdminRoutes(admin: PackAdmin, secret: String) {
    route("/admin/packs/{id}") {
        post("/sync") { call.runAdmin(secret, admin::sync) }
        post("/publish") { call.runAdmin(secret, admin::publish) }
    }
}

private suspend fun ApplicationCall.runAdmin(secret: String, action: suspend (Long) -> PackResult) {
    val provided = request.headers["X-Admin-Key"].orEmpty()
    if (secret.isEmpty() || !MessageDigest.isEqual(secret.toByteArray(), provided.toByteArray())) {
        respond(HttpStatusCode.Unauthorized, Report("UNAUTHORIZED", emptyList()))
        return
    }
    val report = action(positiveId())
    val status = when (report.state) {
        "DONE", "PUBLISHED" -> HttpStatusCode.OK
        "NOT_FOUND" -> HttpStatusCode.NotFound
        "BUSY", "INCOMPLETE", "EMPTY" -> HttpStatusCode.Conflict
        "QUOTA_PAUSED" -> HttpStatusCode.TooManyRequests
        else -> HttpStatusCode.ServiceUnavailable
    }
    respond(status, Report(report.state, report.events.map { Event(it.filmId, it.title, it.result) }))
}
