package database.jdbc

import java.sql.ResultSet
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun <T> DataSource.query(
    sql: String,
    vararg parameters: Any,
    map: (ResultSet) -> T
): List<T> = withContext(Dispatchers.IO) {
    connection.use { connection ->
        connection.prepareStatement(sql).use { statement ->
            statement.queryTimeout = 5
            parameters.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
            statement.executeQuery().use { rows ->
                buildList { while (rows.next()) add(map(rows)) }
            }
        }
    }
}
