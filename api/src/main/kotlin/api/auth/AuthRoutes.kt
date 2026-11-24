package api.auth

import api.dto.ErrorResponse
import api.profile.AccountResponse
import api.profile.response
import catalog.auth.AuthStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.Locale
import kotlinx.serialization.Serializable

internal fun Route.authRoutes(store: AuthStore) {
    post("/auth/register") {
        val (login, password) = call.credentials()
        val account = store.register(login, password)
            ?: return@post call.respond(HttpStatusCode.Conflict, ErrorResponse("LOGIN_TAKEN", "Login already in use"))
        call.respond(HttpStatusCode.Created, SessionResponse(store.createSession(account.id), account.response()))
    }
    post("/auth/login") {
        val (login, password) = call.credentials()
        val account = store.signIn(login, password)
            ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_CREDENTIALS", "Invalid login or password"))
        call.respond(SessionResponse(store.createSession(account.id), account.response()))
    }
    post("/auth/logout") {
        call.token()?.let { store.endSession(it) }
        call.respond(HttpStatusCode.NoContent)
    }
}

@Serializable private data class Credentials(val login: String, val password: String)
@Serializable private data class SessionResponse(val token: String, val account: AccountResponse)

private val loginPattern = Regex("[a-z0-9_.-]{3,32}")

private suspend fun ApplicationCall.credentials(): Credentials {
    val body = receive<Credentials>()
    val login = body.login.trim().lowercase(Locale.ROOT)
    if (!loginPattern.matches(login) || body.password.length !in 8..128) {
        throw BadRequestException("Invalid credentials format")
    }
    return body.copy(login = login)
}
