package catalog.packs

data class ImportEvent(val filmId: Long, val title: String, val result: String)
data class PackResult(val state: String, val events: List<ImportEvent> = emptyList())

interface PackAdmin {
    suspend fun sync(packId: Long): PackResult
    suspend fun publish(packId: Long): PackResult
}
