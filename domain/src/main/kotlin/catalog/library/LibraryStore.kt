package catalog.library

import catalog.auth.Account
import catalog.film.Film

interface LibraryStore {
    suspend fun library(accountId: Long, status: FilmStatus): List<Film>
    suspend fun setStatus(account: Account, filmId: Long, status: FilmStatus): Boolean
    suspend fun clearStatus(accountId: Long, filmId: Long)
}
