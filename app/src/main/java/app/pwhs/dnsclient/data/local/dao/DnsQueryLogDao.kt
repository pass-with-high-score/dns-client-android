package app.pwhs.dnsclient.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.pwhs.dnsclient.data.local.entity.DnsQueryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsQueryLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DnsQueryLogEntity)

    @Query("SELECT * FROM dns_query_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 500): Flow<List<DnsQueryLogEntity>>

    @Query("SELECT COUNT(*) FROM dns_query_log")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT AVG(latencyMs) FROM dns_query_log WHERE responseCode = 0")
    fun getAverageLatency(): Flow<Double?>

    @Query("DELETE FROM dns_query_log")
    suspend fun clearAll()

    @Query("DELETE FROM dns_query_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
