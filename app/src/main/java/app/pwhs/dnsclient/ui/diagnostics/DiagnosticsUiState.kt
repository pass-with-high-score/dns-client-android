package app.pwhs.dnsclient.ui.diagnostics

data class DiagnosticsUiState(
    val connectionType: String = "Unknown",
    val publicIp: String = "Fetching...",
    val isLoadingIp: Boolean = true,
    val isConnected: Boolean = false,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val totalQueries: Long = 0
)