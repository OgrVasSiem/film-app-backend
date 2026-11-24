package database.importer

import java.sql.Connection

internal class ImportStopped(val reason: String) : RuntimeException(reason)

// The caller holds a session-level advisory lock across imports. Each reservation
// commits before HTTP so that a timeout or process restart still costs one request.
internal fun Connection.reserveRequest() {
    createStatement().use { sql ->
        sql.executeUpdate("DELETE FROM poiskkino_requests WHERE requested_at <= clock_timestamp() - interval '24 hours'")
        val reserved = sql.executeUpdate(
            """INSERT INTO poiskkino_requests(requested_at)
               SELECT clock_timestamp() FROM poiskkino_quota
               WHERE (blocked_until IS NULL OR blocked_until <= clock_timestamp())
                 AND (SELECT count(*) FROM poiskkino_requests) < 200"""
        )
        if (reserved == 0) throw ImportStopped("QUOTA_PAUSED")
    }
}

internal fun Connection.pauseProvider() {
    createStatement().use {
        it.executeUpdate("UPDATE poiskkino_quota SET blocked_until = clock_timestamp() + interval '24 hours'")
    }
}
