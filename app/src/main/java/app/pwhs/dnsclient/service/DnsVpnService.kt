package app.pwhs.dnsclient.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import app.pwhs.dnsclient.MainActivity
import app.pwhs.dnsclient.R
import app.pwhs.dnsclient.core.dns.DnsPacketParser
import app.pwhs.dnsclient.core.dns.DohResolver
import app.pwhs.dnsclient.data.local.dao.DnsQueryLogDao
import app.pwhs.dnsclient.data.local.entity.DnsQueryLogEntity
import app.pwhs.dnsclient.data.preferences.DnsPreferences
import app.pwhs.dnsclient.domain.model.DnsServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class DnsVpnService : VpnService() {

    private val dohResolver: DohResolver by inject()
    private val queryLogDao: DnsQueryLogDao by inject()
    private val preferences: DnsPreferences by inject()

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isRunning = false

    companion object {
        const val ACTION_START = "app.pwhs.dnsclient.START_VPN"
        const val ACTION_STOP = "app.pwhs.dnsclient.STOP_VPN"
        private const val CHANNEL_ID = "dns_vpn_channel"
        private const val NOTIFICATION_ID = 1

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _totalQueries = MutableStateFlow(0L)
        val totalQueries: StateFlow<Long> = _totalQueries.asStateFlow()

        private val _connectedSince = MutableStateFlow<Long?>(null)
        val connectedSince: StateFlow<Long?> = _connectedSince.asStateFlow()

        private val _bytesSent = MutableStateFlow(0L)
        val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

        private val _bytesReceived = MutableStateFlow(0L)
        val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("Connecting..."))
                startVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        scope.launch {
            try {
                val serverKey = preferences.selectedServerKey.first()
                val nextDnsId = preferences.nextDnsProfileId.first()
                val customUrl = preferences.customDohUrl.first()
                val dnsServer = DnsServer.fromKey(serverKey, nextDnsId, customUrl)
                val logEnabled = preferences.logQueries.first()

                // Use a fake internal DNS IP so that DNS queries from apps go through
                // the VPN tunnel, but our own DoH HTTPS requests to real IPs
                // (e.g., 1.1.1.1:443) bypass the tunnel — avoiding a routing loop.
                val fakeDnsIp = "10.0.0.1"

                val builder = Builder()
                    .setSession("DNS Client")
                    .addAddress("10.0.0.2", 30) // /30 covers 10.0.0.0–10.0.0.3
                    .addDnsServer(fakeDnsIp)
                    .addRoute(fakeDnsIp, 32)    // Route ONLY the fake DNS IP through VPN
                    .setBlocking(true)
                    .setMtu(1500)

                // Exclude our own app so DoH requests go directly to the internet
                try {
                    builder.addDisallowedApplication(applicationContext.packageName)
                } catch (e: Exception) {
                    Timber.w(e, "Could not exclude own app from VPN")
                }

                vpnInterface = builder.establish() ?: run {
                    Timber.e("Failed to establish VPN interface")
                    stopSelf()
                    return@launch
                }

                isRunning = true
                _isConnected.value = true
                _connectedSince.value = System.currentTimeMillis()
                _totalQueries.value = 0

                updateNotification("Connected — ${dnsServer.name}")
                Timber.i("VPN started with DNS: ${dnsServer.name} (${dnsServer.dohUrl})")

                runTunnel(dnsServer, logEnabled)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start VPN")
                stopVpn()
                stopSelf()
            }
        }
    }

    private suspend fun runTunnel(dnsServer: DnsServer, logEnabled: Boolean) {
        val fd = vpnInterface ?: return
        val inputStream = FileInputStream(fd.fileDescriptor)
        val outputStream = FileOutputStream(fd.fileDescriptor)

        val packetBuffer = ByteBuffer.allocate(32767)

        try {
            while (isRunning) {
                packetBuffer.clear()
                val length = inputStream.read(packetBuffer.array())

                if (length <= 0) continue

                _bytesReceived.value += length

                val packet = DnsVpnConnection.extractDnsQuery(packetBuffer.array(), length)
                    ?: continue

                val query = DnsPacketParser.parseQuery(packet.dnsPayload) ?: continue

                _totalQueries.value++
                val startTime = System.currentTimeMillis()

                scope.launch {
                    try {
                        val responseBytes = dohResolver.resolve(dnsServer.dohUrl, query.rawBytes)

                        if (responseBytes != null) {
                            val responsePacket = DnsVpnConnection.buildResponsePacket(packet, responseBytes)
                            synchronized(outputStream) {
                                outputStream.write(responsePacket)
                                outputStream.flush()
                                _bytesSent.value += responsePacket.size
                            }

                            if (logEnabled) {
                                val latency = System.currentTimeMillis() - startTime
                                val rcode = DnsPacketParser.extractResponseCode(responseBytes)
                                queryLogDao.insert(
                                    DnsQueryLogEntity(
                                        domain = query.domain,
                                        queryType = query.queryTypeName,
                                        responseCode = rcode,
                                        upstreamName = dnsServer.name,
                                        latencyMs = latency
                                    )
                                )
                            }
                        } else {
                            Timber.w("No response for query: ${query.domain}")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error resolving DNS for ${query.domain}")
                    }
                }
            }
        } catch (e: Exception) {
            if (isRunning) {
                Timber.e(e, "Tunnel read error")
            }
        }
    }

    private fun stopVpn() {
        isRunning = false
        _isConnected.value = false
        _connectedSince.value = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing VPN interface")
        }
        vpnInterface = null

        scope.cancel()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DNS VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when DNS VPN is active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, DnsVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DNS Client")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
