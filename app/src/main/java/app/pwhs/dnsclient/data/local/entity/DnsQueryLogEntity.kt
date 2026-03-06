package app.pwhs.dnsclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_query_log")
data class DnsQueryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val queryType: String, // A, AAAA, CNAME, etc.
    val responseCode: Int, // 0 = NOERROR, 2 = SERVFAIL, 3 = NXDOMAIN
    val upstreamName: String,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    val responseCodeName: String
        get() = when (responseCode) {
            0 -> "NOERROR"
            1 -> "FORMERR"
            2 -> "SERVFAIL"
            3 -> "NXDOMAIN"
            5 -> "REFUSED"
            else -> "UNKNOWN($responseCode)"
        }

    val isSuccess: Boolean get() = responseCode == 0
}
