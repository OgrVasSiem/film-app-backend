package catalog

import catalog.auth.AuthStore
import catalog.library.LibraryStore
import catalog.packs.PackStore

interface FilmStore : AuthStore, PackStore, LibraryStore
