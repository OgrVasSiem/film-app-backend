package database

import catalog.FilmStore
import catalog.auth.AuthStore
import catalog.library.LibraryStore
import catalog.packs.PackStore
import database.auth.PostgresAuthStore
import database.library.PostgresLibraryStore
import database.packs.PostgresPackStore
import javax.sql.DataSource

internal class PostgresFilmStore(source: DataSource) :
    FilmStore,
    AuthStore by PostgresAuthStore(source),
    PackStore by PostgresPackStore(source),
    LibraryStore by PostgresLibraryStore(source)
