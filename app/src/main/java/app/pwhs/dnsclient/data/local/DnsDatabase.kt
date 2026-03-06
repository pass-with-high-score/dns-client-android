package app.pwhs.dnsclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.pwhs.dnsclient.data.local.dao.DnsQueryLogDao
import app.pwhs.dnsclient.data.local.entity.DnsQueryLogEntity

@Database(
    entities = [DnsQueryLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class DnsDatabase : RoomDatabase() {
    abstract fun dnsQueryLogDao(): DnsQueryLogDao

    companion object {
        const val DATABASE_NAME = "dns_client_db"
    }
}
