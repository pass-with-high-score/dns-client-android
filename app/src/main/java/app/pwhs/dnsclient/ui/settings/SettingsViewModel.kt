package app.pwhs.dnsclient.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.dnsclient.data.preferences.DnsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoConnect: Boolean = false,
    val logQueries: Boolean = true,
    val appLockEnabled: Boolean = false
)

class SettingsViewModel(
    private val preferences: DnsPreferences
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.autoConnect,
        preferences.logQueries,
        preferences.appLockEnabled
    ) { autoConnect, logQueries, appLockEnabled ->
        SettingsUiState(
            autoConnect = autoConnect,
            logQueries = logQueries,
            appLockEnabled = appLockEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoConnect(enabled)
        }
    }

    fun setLogQueries(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setLogQueries(enabled)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAppLockEnabled(enabled)
        }
    }
}
