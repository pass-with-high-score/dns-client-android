package app.pwhs.dnsclient.ui.home

data class HomeUiState(
    val isConnected: Boolean = false,
    val serverName: String = "Cloudflare",
    val serverEmoji: String = "⚡",
    val totalQueries: Long = 0,
    val connectedSince: Long? = null
)