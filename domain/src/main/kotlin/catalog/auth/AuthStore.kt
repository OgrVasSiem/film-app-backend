package catalog.auth

interface AuthStore {
    suspend fun register(login: String, password: String): Account?
    suspend fun signIn(login: String, password: String): Account?
    suspend fun createSession(accountId: Long): String
    suspend fun accountFor(token: String): Account?
    suspend fun endSession(token: String)
}
