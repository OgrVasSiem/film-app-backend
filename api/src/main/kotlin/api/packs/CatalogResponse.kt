package api.packs

import catalog.packs.FilmPack
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.HexFormat

@Serializable internal data class PackResponse(
    val id: Long, val title: String, val description: String, val premium: Boolean, val locked: Boolean,
    val coverUrl: String, val isCompleted: Boolean
)

internal fun FilmPack.response(hasPremium: Boolean) = PackResponse(
    id, title, description, premium, premium && !hasPremium, "/api/v1/pack-covers/$coverFile", isCompleted
)

@Serializable private data class Cover(val mimeType: String, val sha256: String, val base64: String)
@Serializable private data class Catalog(val packs: List<PackResponse>, val covers: Map<String, Cover>)

private fun digest(bytes: ByteArray) =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

internal suspend fun ApplicationCall.respondCatalog(accountId: Long, packs: List<PackResponse>) {
    val directory = File(System.getenv("PACK_COVER_DIR") ?: "media/pack-covers")
    val images = withContext(Dispatchers.IO) {
        packs.map { it.coverUrl }.distinct().associateWith { url ->
            val file = File(directory, url.substringAfterLast('/'))
            val actual = if (file.isFile) file else File(directory, "default.png")
            val mime = when (actual.extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                else -> "image/png"
            }
            val bytes = actual.readBytes()
            Triple(mime, digest(bytes), bytes)
        }
    }
    val manifest = buildString {
        appendLine("catalog-v1:$accountId")
        appendLine(Json.encodeToString(packs))
        images.values.forEach { appendLine(it.first + ":" + it.second) }
    }
    val tag = "\"" + digest(manifest.toByteArray()) + "\""
    response.headers.append("ETag", tag)
    response.headers.append("Cache-Control", "private, no-cache")
    response.headers.append("Vary", "Authorization")
    val matches = request.headers["If-None-Match"].orEmpty().split(',').any {
        it.trim().removePrefix("W/") == tag || it.trim() == "*"
    }
    if (matches) {
        respond(HttpStatusCode.NotModified)
        return
    }
    respond(Catalog(packs, images.mapValues { (_, image) ->
        Cover(image.first, image.second, Base64.getEncoder().encodeToString(image.third))
    }))
}
