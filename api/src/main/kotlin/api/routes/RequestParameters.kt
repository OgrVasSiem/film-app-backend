package api.routes

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException

internal fun ApplicationCall.positiveId(): Long =
    parameters["id"]?.toLongOrNull()?.takeIf { it > 0 } ?: throw BadRequestException("Invalid id")
