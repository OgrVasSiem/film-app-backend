package database.library

import catalog.auth.Account
import catalog.library.FilmStatus
import catalog.library.LibraryStore
import database.film.film
import database.jdbc.query
import javax.sql.DataSource

internal class PostgresLibraryStore(private val source: DataSource) : LibraryStore {
    override suspend fun library(accountId: Long, status: FilmStatus) = source.query(
        """SELECT f.* FROM film_library l JOIN films f ON f.id = l.film_id
           WHERE l.account_id = ? AND l.status = ? ORDER BY l.updated_at DESC, f.id""",
        accountId, status.name, map = ::film
    )

    override suspend fun setStatus(account: Account, filmId: Long, status: FilmStatus): Boolean = source.query(
        """INSERT INTO film_library (account_id, film_id, status)
           SELECT ?, f.id, ? FROM films f WHERE f.id = ? AND (
               EXISTS (SELECT 1 FROM film_library l WHERE l.account_id = ? AND l.film_id = f.id)
               OR EXISTS (
                   SELECT 1 FROM pack_films pf JOIN visible_film_packs p ON p.id = pf.pack_id
                   WHERE pf.film_id = f.id AND (NOT p.premium OR ?)
               )
           )
           ON CONFLICT (account_id, film_id) DO UPDATE SET status = excluded.status, updated_at = now()
           RETURNING film_id""",
        account.id, status.name, filmId, account.id, account.premium
    ) { it.getLong(1) }.isNotEmpty()

    override suspend fun clearStatus(accountId: Long, filmId: Long) {
        source.query(
            "DELETE FROM film_library WHERE account_id = ? AND film_id = ? RETURNING film_id",
            accountId, filmId
        ) { it.getLong(1) }
    }
}
