package database.auth

import catalog.auth.Account
import catalog.auth.AuthStore
import database.jdbc.query
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.ResultSet
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PostgresAuthStore(private val source: DataSource) : AuthStore {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    override suspend fun register(login: String, password: String): Account? {
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = withContext(Dispatchers.Default) { passwordHash(password, salt) }
        val stored = "${encoder.encodeToString(salt)}.${encoder.encodeToString(hash)}"
        return source.query(
            """INSERT INTO accounts (login, password_hash) VALUES (?, ?)
               ON CONFLICT (login) DO NOTHING RETURNING id, login, premium""",
            login, stored, map = ::account
        ).singleOrNull()
    }

    override suspend fun signIn(login: String, password: String): Account? {
        val entry = source.query("SELECT * FROM accounts WHERE login = ?", login) {
            account(it) to it.getString("password_hash")
        }.singleOrNull()
        val stored = entry?.second?.split('.')
        val salt = stored?.let { decoder.decode(it[0]) } ?: ByteArray(16)
        val expected = stored?.let { decoder.decode(it[1]) } ?: ByteArray(32)
        val actual = withContext(Dispatchers.Default) { passwordHash(password, salt) }
        return if (MessageDigest.isEqual(expected, actual)) entry?.first else null
    }

    override suspend fun createSession(accountId: Long): String {
        val token = encoder.encodeToString(ByteArray(32).also(random::nextBytes))
        source.query(
            """WITH expired AS (DELETE FROM sessions WHERE expires_at <= now())
               INSERT INTO sessions (token_hash, account_id, expires_at)
               VALUES (?, ?, now() + interval '30 days') RETURNING account_id""",
            tokenHash(token), accountId
        ) { it.getLong(1) }
        return token
    }

    override suspend fun accountFor(token: String): Account? = source.query(
        """SELECT a.* FROM sessions s JOIN accounts a ON a.id = s.account_id
           WHERE s.token_hash = ? AND s.expires_at > now()""",
        tokenHash(token), map = ::account
    ).singleOrNull()

    override suspend fun endSession(token: String) {
        source.query("DELETE FROM sessions WHERE token_hash = ? RETURNING account_id", tokenHash(token)) { it.getLong(1) }
    }

    private fun account(row: ResultSet) =
        Account(row.getLong("id"), row.getString("login"), row.getBoolean("premium"))

    private fun tokenHash(token: String): String =
        encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

    private fun passwordHash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
