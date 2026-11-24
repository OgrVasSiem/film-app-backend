package api.routes

import api.admin.packAdminRoutes
import catalog.FilmStore
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(filmStore: FilmStore, admin: catalog.packs.PackAdmin, adminKey: String) {
    routing {
        filmRoutes(filmStore)
        packAdminRoutes(admin, adminKey)
    }
}
