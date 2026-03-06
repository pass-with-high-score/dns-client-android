package app.pwhs.dnsclient.ui.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.lifecycle.AndroidViewModel
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


class HomeViewModel(
    preferences: DnsPreferences,
    application: Application
) : AndroidViewModel(application) {

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

    fun onAction(action: HomeUiAction) {
        when (action) {
            HomeUiAction.OnToggleVpn -> toggleVpn()
        }
    }

    private fun toggleVpn() {
        viewModelScope.launch {
            if (DnsVpnService.isConnected.value) {
                stopVpn(getApplication<Application>().applicationContext)
            } else {
                startVpn(getApplication<Application>().applicationContext)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun stopVpn(context: Context) {
        val serviceIntent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }
}
