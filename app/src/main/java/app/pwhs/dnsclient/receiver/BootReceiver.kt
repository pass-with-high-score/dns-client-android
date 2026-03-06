package app.pwhs.dnsclient.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.pwhs.dnsclient.data.preferences.DnsPreferences
import app.pwhs.dnsclient.service.DnsVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val preferences: DnsPreferences by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val autoConnect = preferences.autoConnect.first()
            if (autoConnect) {
                val serviceIntent = Intent(context, DnsVpnService::class.java).apply {
                    action = DnsVpnService.ACTION_START
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
