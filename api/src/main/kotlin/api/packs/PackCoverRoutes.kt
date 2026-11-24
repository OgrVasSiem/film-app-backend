package api.packs

import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

private val coverName = Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,154}\\.(png|jpg|jpeg|webp)")

internal fun Route.packCoverRoutes() {
    val directory = File(System.getenv("PACK_COVER_DIR") ?: "media/pack-covers")
    get("/pack-covers/{file}") {
        val name = call.parameters["file"] ?: throw NotFoundException()
        if (!coverName.matches(name)) throw NotFoundException()
        val cover = File(directory, name)
        if (!cover.isFile) throw NotFoundException()
        call.respondFile(cover)
    }
}
