package app.pwhs.dnsclient.ui.servers

import app.pwhs.dnsclient.domain.model.DnsServer

data class ServerListUiState(
    val servers: List<DnsServer> = DnsServer.presets,
    val selectedServerKey: String = "Cloudflare",
    val nextDnsProfileId: String = "",
    val showNextDnsDialog: Boolean = false,
    val showCustomDialog: Boolean = false,
    val customDohUrl: String = ""
)
