package app.pwhs.dnsclient.ui.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.dnsclient.service.DnsVpnService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

data class DiagnosticsUiState(
    val connectionType: String = "Unknown",
    val publicIp: String = "Fetching...",
    val isLoadingIp: Boolean = true,
    val isConnected: Boolean = false,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val totalQueries: Long = 0
)

class DiagnosticsViewModel(
    private val httpClient: HttpClient
) : ViewModel() {

    private val _connectionType = MutableStateFlow("Unknown")
    private val _publicIp = MutableStateFlow("Fetching...")
    private val _isLoadingIp = MutableStateFlow(true)

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        combine(
            _connectionType,
            _publicIp,
            _isLoadingIp
        ) { connectionType, publicIp, isLoadingIp ->
            Triple(connectionType, publicIp, isLoadingIp)
        },
        combine(
            DnsVpnService.isConnected,
            DnsVpnService.bytesSent,
            DnsVpnService.bytesReceived,
        ) { isConnected, bytesSent, bytesReceived ->
            Triple(isConnected, bytesSent, bytesReceived)
        },
        DnsVpnService.totalQueries
    ) { (connectionType, publicIp, isLoadingIp), (isConnected, bytesSent, bytesReceived), totalQueries ->
        DiagnosticsUiState(
            connectionType = connectionType,
            publicIp = publicIp,
            isLoadingIp = isLoadingIp,
            isConnected = isConnected,
            bytesSent = bytesSent,
            bytesReceived = bytesReceived,
            totalQueries = totalQueries
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiagnosticsUiState())

    fun loadDiagnostics(context: Context) {
        detectConnectionType(context)
        fetchPublicIp()
    }

    private fun detectConnectionType(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }

        _connectionType.value = when {
            capabilities == null -> "No connection"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
    }

    private fun fetchPublicIp() {
        viewModelScope.launch {
            _isLoadingIp.value = true
            try {
                val response = httpClient.get("https://api.ipify.org")
                _publicIp.value = response.bodyAsText().trim()
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch public IP")
                _publicIp.value = "Unavailable"
            } finally {
                _isLoadingIp.value = false
            }
        }
    }

    fun refresh(context: Context) {
        loadDiagnostics(context)
    }
}
