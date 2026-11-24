package api.routes

import api.auth.authRoutes
import api.library.libraryRoutes
import api.packs.packCoverRoutes
import api.packs.packRoutes
import api.profile.profileRoutes
import catalog.FilmStore
import io.ktor.server.routing.*

fun Route.filmRoutes(store: FilmStore) = route("/api/v1") {
    packCoverRoutes()
    authRoutes(store)
    profileRoutes(store)
    packRoutes(store, store)
    libraryRoutes(store, store)
}
