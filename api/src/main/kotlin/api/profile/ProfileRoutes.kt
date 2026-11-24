package api.profile

import api.auth.requireAccount
import catalog.auth.Account
import catalog.auth.AuthStore
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

internal fun Route.profileRoutes(store: AuthStore) {
    get("/me") {
        val account = call.requireAccount(store) ?: return@get
        call.respond(account.response())
    }
}

@Serializable internal data class AccountResponse(val id: Long, val login: String, val premium: Boolean, val adsEnabled: Boolean)

internal fun Account.response() = AccountResponse(id, login, premium, !premium)
