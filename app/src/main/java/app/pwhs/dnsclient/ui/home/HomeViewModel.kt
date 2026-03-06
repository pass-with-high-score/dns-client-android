package app.pwhs.dnsclient.ui.home

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.dnsclient.data.preferences.DnsPreferences
import app.pwhs.dnsclient.domain.model.DnsServer
import app.pwhs.dnsclient.service.DnsVpnService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isConnected: Boolean = false,
    val serverName: String = "Cloudflare",
    val serverEmoji: String = "⚡",
    val totalQueries: Long = 0,
    val connectedSince: Long? = null
)

sealed class HomeEvent {
    data class RequestVpnPermission(val intent: Intent) : HomeEvent()
}

class HomeViewModel(
    private val preferences: DnsPreferences
) : ViewModel() {

    private val _events = MutableSharedFlow<HomeEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            DnsVpnService.isConnected,
            DnsVpnService.totalQueries,
            DnsVpnService.connectedSince,
        ) { isConnected, totalQueries, connectedSince ->
            Triple(isConnected, totalQueries, connectedSince)
        },
        combine(
            preferences.selectedServerKey,
            preferences.nextDnsProfileId,
            preferences.customDohUrl,
        ) { serverKey, nextDnsId, customUrl ->
            Triple(serverKey, nextDnsId, customUrl)
        }
    ) { (isConnected, totalQueries, connectedSince), (serverKey, nextDnsId, customUrl) ->
        val server = DnsServer.fromKey(serverKey, nextDnsId, customUrl)
        HomeUiState(
            isConnected = isConnected,
            serverName = server.name,
            serverEmoji = server.iconEmoji,
            totalQueries = totalQueries,
            connectedSince = connectedSince
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun toggleVpn(context: Context) {
        viewModelScope.launch {
            if (DnsVpnService.isConnected.value) {
                stopVpn(context)
            } else {
                startVpn(context)
            }
        }
    }

    private suspend fun startVpn(context: Context) {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            _events.emit(HomeEvent.RequestVpnPermission(prepareIntent))
        } else {
            launchVpnService(context)
        }
    }

    fun launchVpnService(context: Context) {
        val serviceIntent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
        }
        context.startForegroundService(serviceIntent)
    }

    private fun stopVpn(context: Context) {
        val serviceIntent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }
}
