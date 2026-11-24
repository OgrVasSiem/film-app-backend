package catalog.packs

import catalog.film.Film

data class FilmCursor(val position: Int, val filmId: Long)
data class PositionedFilm(val film: Film, val position: Int)

interface PackStore {
    suspend fun filmPage(packId: Long, accountId: Long, cursor: FilmCursor?, fetch: Int): List<PositionedFilm>
    suspend fun packs(accountId: Long): List<FilmPack>
    suspend fun pack(id: Long, accountId: Long): FilmPack?
    suspend fun packFilms(packId: Long, accountId: Long): List<Film>
}
