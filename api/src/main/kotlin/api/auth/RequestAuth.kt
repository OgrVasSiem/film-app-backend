package api.auth

import api.dto.ErrorResponse
import catalog.auth.Account
import catalog.auth.AuthStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

private val tokenPattern = Regex("[A-Za-z0-9_-]{43}")

internal fun ApplicationCall.token(): String? {
    val header = request.headers["Authorization"] ?: return null
    if (!header.startsWith("Bearer ", ignoreCase = true)) return null
    return header.substring(7).takeIf(tokenPattern::matches)
}

internal suspend fun ApplicationCall.requireAccount(store: AuthStore): Account? {
    val account = token()?.let { store.accountFor(it) }
    if (account == null) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Valid bearer token required"))
    }
    return account
}
