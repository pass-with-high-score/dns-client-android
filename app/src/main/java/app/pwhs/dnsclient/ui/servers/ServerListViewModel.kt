package app.pwhs.dnsclient.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.dnsclient.data.preferences.DnsPreferences
import app.pwhs.dnsclient.domain.model.DnsServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServerListViewModel(
    private val preferences: DnsPreferences
) : ViewModel() {

    private val _showNextDnsDialog = MutableStateFlow(false)
    private val _showCustomDialog = MutableStateFlow(false)

    val showNextDnsDialog: StateFlow<Boolean> = _showNextDnsDialog.asStateFlow()
    val showCustomDialog: StateFlow<Boolean> = _showCustomDialog.asStateFlow()

    val uiState: StateFlow<ServerListUiState> = combine(
        preferences.selectedServerKey,
        preferences.nextDnsProfileId,
        preferences.customDohUrl,
        _showNextDnsDialog,
        _showCustomDialog
    ) { serverKey, nextDnsId, customUrl, showNextDns, showCustom ->
        ServerListUiState(
            selectedServerKey = serverKey,
            nextDnsProfileId = nextDnsId,
            customDohUrl = customUrl,
            showNextDnsDialog = showNextDns,
            showCustomDialog = showCustom
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerListUiState())

    fun onAction(action: ServerListUiAction) {
        when (action) {
            is ServerListUiAction.OnSelectServer -> selectServer(action.server)
            is ServerListUiAction.OnConfirmNextDns -> confirmNextDns(action.profileId)
            is ServerListUiAction.OnConfirmCustom -> confirmCustom(action.url)
            is ServerListUiAction.OnDismissDialog -> dismissDialog()
        }
    }

    private fun selectServer(server: DnsServer) {
        viewModelScope.launch {
            when (server) {
                is DnsServer.NextDns -> {
                    _showNextDnsDialog.value = true
                }

                is DnsServer.Custom -> {
                    _showCustomDialog.value = true
                }

                else -> {
                    preferences.setSelectedServer(server.key)
                }
            }
        }
    }

    private fun confirmNextDns(profileId: String) {
        viewModelScope.launch {
            preferences.setNextDnsProfileId(profileId)
            preferences.setSelectedServer("NextDns")
            _showNextDnsDialog.value = false
        }
    }

    private fun confirmCustom(url: String) {
        viewModelScope.launch {
            preferences.setCustomDohUrl(url)
            preferences.setSelectedServer("Custom")
            _showCustomDialog.value = false
        }
    }

    private fun dismissDialog() {
        _showNextDnsDialog.value = false
        _showCustomDialog.value = false
    }
}
